package com.example.kcomproject.domain.store.dto.response;

import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.entity.StoreStatus;
import lombok.Builder;

@Builder
public record StoreResponse(
        Long id,
        String name,
        String address,
        StoreStatus status
) {
    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .status(store.getStatus())
                .build();
    }
}
