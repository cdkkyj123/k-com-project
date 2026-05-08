package com.example.kcomproject.domain.point.dto.response;

import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointHistoryResponse(
        Long id,
        Long userId,
        PointTransactionType type,
        Long amount,
        Long balanceAfter,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return PointHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .type(history.getType())
                .amount(history.getAmount())
                .balanceAfter(history.getBalanceAfter())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
