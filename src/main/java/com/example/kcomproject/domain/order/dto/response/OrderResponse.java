package com.example.kcomproject.domain.order.dto.response;

import lombok.Builder;

@Builder
public record OrderResponse(
        Long orderId,
        Long userId,
        Long storeId,
        Long menuId,
        Long price
) {}
