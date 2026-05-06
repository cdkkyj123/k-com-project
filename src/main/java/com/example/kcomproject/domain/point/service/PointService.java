package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.dto.response.PointResponse;
import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionService pointTransactionService;
    private final ChargeHistoryRepository chargeHistoryRepository;
    private final MockPaymentService mockPaymentService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY_PREFIX = "user_point_lock:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency_key:point_charge:";

    public PointResponse chargePoint(Long userId, Long amount, String idempotencyKey) {
        // 1. Redis 멱등성 체크
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
            return withLock(userId, () -> pointTransactionService.chargeAndCompleteHistory(userId, amount, history.getId()));
        } catch (Exception e) {
            // 6. 결제 실패 시: 이력 상태 업데이트 (FAILED)
            pointTransactionService.updateHistoryStatus(history.getId(), ChargeStatus.FAILED);
            throw e;
        }
    }

    public PointResponse usePoint(Long userId, Long amount) {
        return withLock(userId, () -> pointTransactionService.use(userId, amount));
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
            withLock(history.getUserId(), () -> pointTransactionService.chargeAndCompleteHistory(
                    history.getUserId(), history.getAmount(), history.getId()));
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

    private PointResponse withLock(Long userId, Supplier<User> action) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                throw new PointException(ErrorCode.POINT_LOCK_FAILED);
            }
            User user = action.get();
            return PointResponse.builder()
                    .userId(user.getId())
                    .balanceAfter(user.getPointBalance())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PointException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
