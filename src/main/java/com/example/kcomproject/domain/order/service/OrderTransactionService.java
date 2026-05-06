package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.repository.OrderRepository;
import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import com.example.kcomproject.domain.point.service.PointTransactionService;
import com.example.kcomproject.domain.store.service.StoreService;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.MenuException;
import com.example.kcomproject.global.exception.domain.OrderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final PointTransactionService pointTransactionService;
    private final StoreService storeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order executeOrder(Long userId, Long storeId, Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        // 1. Validate store status
        storeService.validateStoreStatus(storeId);

        // 2. Deduct point balance (Atomically within this transaction)
        pointTransactionService.use(userId, menu.getPrice());

        // 3. Save Order record
        Order order = Order.builder()
                .userId(userId)
                .storeId(storeId)
                .menuId(menuId)
                .price(menu.getPrice())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 4. Create Outbox record with partitionKey (storeId)
        try {
            String payload = objectMapper.writeValueAsString(savedOrder);
            Outbox outbox = Outbox.builder()
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getId())
                    .partitionKey(String.valueOf(storeId))
                    .payload(payload)
                    .status(OutboxStatus.INIT)
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order to JSON for outbox", e);
            throw new OrderException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return savedOrder;
    }
}
