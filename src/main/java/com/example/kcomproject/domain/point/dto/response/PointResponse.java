package com.example.kcomproject.domain.point.dto.response;

import lombok.Builder;

@Builder
public record PointResponse(
        Long userId,
        Long balanceAfter
) {}
