package com.example.kcomproject.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("주문 대기"),
    ACCEPTED("주문 수락"),
    REJECTED("주문 거절"),
    CANCELED("주문 취소");

    private final String description;
}
