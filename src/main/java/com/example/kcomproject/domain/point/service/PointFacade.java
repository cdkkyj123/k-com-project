package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.dto.response.PointResponse;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;
    private final PointTransactionService pointTransactionService;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "user_point_lock:";

    public PointResponse chargePoint(Long userId, Long amount, String idempotencyKey) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!available) {
                throw new PointException(ErrorCode.POINT_LOCK_FAILED);
            }
            return pointService.chargePoint(userId, amount, idempotencyKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PointException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public PointResponse usePoint(Long userId, Long amount) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!available) {
                throw new PointException(ErrorCode.POINT_LOCK_FAILED);
            }
            return pointService.usePoint(userId, amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PointException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void refundPoint(Long userId, Long amount, String idempotencyKey) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!available) {
                throw new PointException(ErrorCode.POINT_LOCK_FAILED);
            }
            pointTransactionService.refund(userId, amount, idempotencyKey);
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
