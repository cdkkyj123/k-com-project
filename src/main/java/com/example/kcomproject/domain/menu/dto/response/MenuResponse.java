package com.example.kcomproject.domain.menu.dto.response;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import lombok.Builder;

@Builder
public record MenuResponse(
        Long id,
        String name,
        Long price,
        MenuStatus status,
        MenuCategory category
) {
    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .status(menu.getStatus())
                .category(menu.getCategory())
                .build();
    }
}
