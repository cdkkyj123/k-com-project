package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.dto.response.PointHistoryResponse;
import com.example.kcomproject.domain.point.dto.response.PointResponse;
import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.global.dto.PageResponseDto;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionService pointTransactionService;
    private final ChargeHistoryRepository chargeHistoryRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MockPaymentService mockPaymentService;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency_key:point_charge:";

    public PointResponse chargePoint(Long userId, Long amount, String idempotencyKey) {
        // 1. Redis 멱등성 체크 (필수)
        checkIdempotency(idempotencyKey);

        // 2. DB 멱등성 체크 (이미 성공한 기록이 있는지 확인)
        chargeHistoryRepository.findByIdempotencyKeyAndStatus(idempotencyKey, ChargeStatus.SUCCESS)
                .ifPresent(h -> {
                    throw new PointException(ErrorCode.DUPLICATE_REQUEST);
                });

        // 3. PENDING 상태로 이력 저장 (즉시 커밋)
        ChargeHistory history = pointTransactionService.createPendingHistory(userId, amount, idempotencyKey);

        try {
            // 4. 가상 결제 서비스 호출
            mockPaymentService.processPayment(userId, amount);

            // 5. 결제 성공 시: 포인트 충전 및 이력 상태 업데이트 (SUCCESS)
            User user = pointTransactionService.chargeAndCompleteHistory(userId, amount, history.getId());
            return PointResponse.builder()
                    .userId(user.getId())
                    .balanceAfter(user.getPointBalance())
                    .build();
        } catch (Exception e) {
            // 6. 결제 실패 시: 이력 상태 업데이트 (FAILED)
            pointTransactionService.updateHistoryStatus(history.getId(), ChargeStatus.FAILED);
            throw e;
        }
    }

    public PointResponse usePoint(Long userId, Long amount) {
        User user = pointTransactionService.use(userId, amount);
        return PointResponse.builder()
                .userId(user.getId())
                .balanceAfter(user.getPointBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PointHistoryResponse> getPointHistories(
            Long userId, PointTransactionType type, LocalDateTime startDate, LocalDateTime endDate, Long lastId, int size) {
        List<PointHistory> histories = pointHistoryRepository.findHistoriesByFilter(userId, type, startDate, endDate, lastId, size);

        boolean hasNext = histories.size() > size;
        List<PointHistory> content = hasNext ? histories.subList(0, size) : histories;
        Long nextLastId = content.isEmpty() ? null : content.get(content.size() - 1).getId();

        return PageResponseDto.ofCursor(
                content.stream().map(PointHistoryResponse::from).toList(),
                size,
                hasNext,
                nextLastId
        );
    }

    public void processWebhook(String idempotencyKey, String status) {
        ChargeHistory history = chargeHistoryRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new PointException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (history.getStatus() != ChargeStatus.PENDING) {
            log.info("Webhook ignored for idempotencyKey: {}. Current status: {}", idempotencyKey, history.getStatus());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            log.info("Webhook success for idempotencyKey: {}. Completing charge...", idempotencyKey);
            pointTransactionService.chargeAndCompleteHistory(
                    history.getUserId(), history.getAmount(), history.getId());
        } else {
            log.info("Webhook failed for idempotencyKey: {}. Updating status...", idempotencyKey);
            pointTransactionService.updateHistoryStatus(history.getId(), ChargeStatus.FAILED);
        }
    }

    private void checkIdempotency(String key) {
        String fullKey = IDEMPOTENCY_KEY_PREFIX + key;
        // setIfAbsent (NX)를 사용하여 원자적으로 중복 체크 및 저장 (1시간 유지)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(fullKey, "processed", Duration.ofHours(1));
        if (Boolean.FALSE.equals(success)) {
            log.warn("Duplicate request detected with idempotency key: {}", key);
            throw new PointException(ErrorCode.DUPLICATE_REQUEST);
        }
    }
}
