package com.example.kcomproject.global.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String refreshToken,
        String email
) {}