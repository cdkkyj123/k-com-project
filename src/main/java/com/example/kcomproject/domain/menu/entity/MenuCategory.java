package com.example.kcomproject.domain.menu.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MenuCategory {
    COFFEE("커피"),
    NON_COFFEE("논커피"),
    ADE("에이드"),
    SMOOTHIE("스무디"),
    DESSERT("디저트");

    private final String description;
}
