package com.example.kcomproject.domain.point.dto.response;

import lombok.Builder;

@Builder
public record PointChargeResponse(
        Long userId,
        Long balanceAfter
) {}
