package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.order.dto.response.OrderResponse;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.order.repository.OrderRepository;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OutboxRepository outboxRepository;
    private final com.example.kcomproject.domain.store.service.StoreService storeService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String LOCK_KEY_PREFIX = "user_point_lock:";

    public OrderResponse createOrder(Long userId, Long storeId, Long menuId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                throw new RuntimeException("Could not acquire lock for user: " + userId);
            }
            Order order = executeOrder(userId, storeId, menuId);
            return OrderResponse.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .storeId(order.getStoreId())
                    .menuId(order.getMenuId())
                    .price(order.getPrice())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public Order executeOrder(Long userId, Long storeId, Long menuId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));

        // 0. Validate store status
        storeService.validateStoreStatus(storeId);

        // 1. Deduct point balance
        user.use(menu.getPrice());
        userRepository.save(user);

        // 2. Save Order record
        Order order = Order.builder()
                .userId(userId)
                .storeId(storeId)
                .menuId(menuId)
                .price(menu.getPrice())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 3. Save PointHistory record
        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(PointTransactionType.USE)
                .amount(menu.getPrice())
                .balanceAfter(user.getPointBalance())
                .build();
        pointHistoryRepository.save(history);

        // 4. Create Outbox record
        try {
            String payload = objectMapper.writeValueAsString(savedOrder);
            Outbox outbox = Outbox.builder()
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getId())
                    .payload(payload)
                    .status(OutboxStatus.INIT)
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order to JSON for outbox", e);
            throw new RuntimeException("Order processing failed due to internal error");
        }

        return savedOrder;
    }
}
