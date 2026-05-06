package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.order.dto.event.OrderCreatedEvent;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderItem;
import com.example.kcomproject.domain.order.entity.OrderStatus;
import com.example.kcomproject.domain.order.repository.OrderItemRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxRepository outboxRepository;
    private final PointTransactionService pointTransactionService;
    private final StoreService storeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order executeOrder(Long userId, Long storeId, List<OrderRequest.OrderItemRequest> itemRequests) {
        // 1. Validate store status
        storeService.validateStoreStatus(storeId);

        List<OrderItem> orderItems = new ArrayList<>();
        long totalPrice = 0;

        for (OrderRequest.OrderItemRequest itemRequest : itemRequests) {
            Menu menu = menuRepository.findById(itemRequest.menuId())
                    .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));
            
            OrderItem orderItem = OrderItem.builder()
                    .menuId(menu.getId())
                    .quantity(itemRequest.quantity())
                    .price(menu.getPrice())
                    .build();
            orderItems.add(orderItem);
            totalPrice += menu.getPrice() * itemRequest.quantity();
        }

        // 2. Deduct point balance
        pointTransactionService.use(userId, totalPrice);

        // 3. Save Order record
        Order order = Order.builder()
                .userId(userId)
                .storeId(storeId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        // 4. Save OrderItems
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            OrderItem itemToSave = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .menuId(item.getMenuId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build();
            savedItems.add(orderItemRepository.save(itemToSave));
        }

        // 5. Create Outbox record with OrderCreatedEvent
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .storeId(savedOrder.getStoreId())
                    .totalPrice(savedOrder.getTotalPrice())
                    .status(savedOrder.getStatus())
                    .items(savedItems.stream()
                            .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                                    .menuId(item.getMenuId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
                    
            String payload = objectMapper.writeValueAsString(event);
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

    @Transactional
    public void rejectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.REJECTED) {
            log.info("Order already rejected: {}", orderId);
            return;
        }

        // 1. Update status to REJECTED
        order.updateStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        // 2. Refund points
        String idempotencyKey = "REFUND-" + orderId;
        pointTransactionService.refund(order.getUserId(), order.getTotalPrice(), idempotencyKey);
        
        log.info("Order rejected and points refunded: {}", orderId);
    }
}
