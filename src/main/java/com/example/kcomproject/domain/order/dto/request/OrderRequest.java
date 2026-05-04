package com.example.kcomproject.domain.order.dto.request;

public record OrderRequest(
        Long userId,
        Long storeId,
        Long menuId
) {}
