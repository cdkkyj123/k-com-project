package com.example.kcomproject.domain.menu.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MenuStatus {
    AVAILABLE("판매 중"),
    OUT_OF_STOCK("품절"),
    SOLD_OUT("매진"),
    HIDDEN("숨김");

    private final String description;
}
