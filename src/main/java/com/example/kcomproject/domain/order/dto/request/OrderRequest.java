package com.example.kcomproject.domain.order.dto.request;

import java.util.List;

public record OrderRequest(
        Long userId,
        Long storeId,
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            Long menuId,
            Integer quantity
    ) {}
}
