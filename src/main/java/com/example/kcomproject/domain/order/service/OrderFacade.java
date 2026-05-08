package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderTransactionService orderTransactionService;
    private final RedissonClient redissonClient;

    public OrderTransactionService.OrderResult executeOrder(Long userId, Long storeId, List<OrderRequest.OrderItemRequest> itemRequests) {
        // Sort items by menuId to prevent deadlock during lock acquisition
        List<OrderRequest.OrderItemRequest> sortedRequests = itemRequests.stream()
                .sorted(Comparator.comparing(OrderRequest.OrderItemRequest::menuId))
                .toList();

        // Acquire Locks (Point Lock then Stock Locks)
        RLock pointLock = redissonClient.getLock("user_point_lock:" + userId);
        List<RLock> stockLocks = sortedRequests.stream()
                .map(req -> redissonClient.getLock("lock:stock:" + storeId + ":" + req.menuId()))
                .toList();

        try {
            boolean pointLockAcquired = pointLock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!pointLockAcquired) {
                throw new OrderException(ErrorCode.POINT_LOCK_FAILED);
            }

            for (RLock stockLock : stockLocks) {
                boolean stockLockAcquired = stockLock.tryLock(10, -1, TimeUnit.SECONDS);
                if (!stockLockAcquired) {
                    throw new OrderException(ErrorCode.STOCK_LOCK_FAILED);
                }
            }

            // Call the transactional service method
            return orderTransactionService.executeOrder(userId, storeId, itemRequests);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            // Unlock in reverse order
            for (int i = stockLocks.size() - 1; i >= 0; i--) {
                RLock lock = stockLocks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            if (pointLock.isHeldByCurrentThread()) {
                pointLock.unlock();
            }
        }
    }

    public void rejectOrder(Long orderId) {
        RLock pointLock = null;
        try {
            Order order = orderTransactionService.getOrder(orderId);
            pointLock = redissonClient.getLock("user_point_lock:" + order.getUserId());
            
            boolean pointLockAcquired = pointLock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!pointLockAcquired) {
                throw new OrderException(ErrorCode.POINT_LOCK_FAILED);
            }
            
            orderTransactionService.rejectOrder(orderId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (pointLock != null && pointLock.isHeldByCurrentThread()) {
                pointLock.unlock();
            }
        }
    }
}
