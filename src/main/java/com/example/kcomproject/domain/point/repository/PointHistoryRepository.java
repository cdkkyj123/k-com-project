package com.example.kcomproject.domain.point.repository;

import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>, PointHistoryQueryRepository {
    List<PointHistory> findByUserId(Long userId);
    List<PointHistory> findByUserIdAndTypeAndRemainAmountGreaterThanAndExpiredAtAfterOrderByCreatedAtAsc(
            Long userId, PointTransactionType type, Long remainAmount, LocalDateTime now);
}
