package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.dto.response.PointChargeResponse;
import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "user_point_lock:";

    public PointChargeResponse chargePoint(Long userId, Long amount) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                throw new RuntimeException("Could not acquire lock for user: " + userId);
            }
            User user = executeCharge(userId, amount);
            return PointChargeResponse.builder()
                    .userId(user.getId())
                    .balanceAfter(user.getPointBalance())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public User executeCharge(Long userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.charge(amount);
        User savedUser = userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .userId(userId)
                .type(PointTransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(savedUser.getPointBalance())
                .build();
        pointHistoryRepository.save(history);

        return savedUser;
    }
}
