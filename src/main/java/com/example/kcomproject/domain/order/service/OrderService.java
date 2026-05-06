package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.dto.response.OrderResponse;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderItem;
import com.example.kcomproject.domain.order.repository.OrderItemRepository;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderTransactionService orderTransactionService;
    private final OrderItemRepository orderItemRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "user_point_lock:";

    public OrderResponse createOrder(Long userId, Long storeId, List<OrderRequest.OrderItemRequest> items) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                throw new OrderException(ErrorCode.POINT_LOCK_FAILED);
            }
            Order order = orderTransactionService.executeOrder(userId, storeId, items);
            
            // Fetch order items to build response
            List<OrderItem> savedItems = orderItemRepository.findAllByOrderId(order.getId());
            
            return OrderResponse.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .storeId(order.getStoreId())
                    .totalPrice(order.getTotalPrice())
                    .status(order.getStatus())
                    .items(savedItems.stream()
                            .map(item -> OrderResponse.OrderItemResponse.builder()
                                    .menuId(item.getMenuId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
