package com.example.kcomproject.domain.point.repository;

import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChargeHistoryRepository extends JpaRepository<ChargeHistory, Long> {
    Optional<ChargeHistory> findByIdempotencyKeyAndStatus(String idempotencyKey, ChargeStatus status);
    Optional<ChargeHistory> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<ChargeHistory> findAllByStatusAndCreatedAtBefore(ChargeStatus status, LocalDateTime createdAt);
}
