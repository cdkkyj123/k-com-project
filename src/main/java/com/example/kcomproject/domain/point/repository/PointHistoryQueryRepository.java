package com.example.kcomproject.domain.point.repository;

import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryQueryRepository {
    List<PointHistory> findHistoriesByFilter(Long userId, PointTransactionType type, LocalDateTime startDate, LocalDateTime endDate, Long lastId, int size);
}
