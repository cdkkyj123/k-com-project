package com.example.kcomproject.domain.order.dto.event;

import com.example.kcomproject.domain.order.entity.OrderStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long storeId,
        Long totalPrice,
        OrderStatus status,
        List<OrderItemEvent> items
) {
    @Builder
    public record OrderItemEvent(
            Long menuId,
            Integer quantity,
            Long price
    ) {}
}
