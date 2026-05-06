package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class MockPaymentService {

    private final Random random = new Random();

    public void processPayment(Long userId, Long amount) {
        log.info("Processing payment for user: {}, amount: {}", userId, amount);

        // 1~3초 사이의 무작위 지연 시뮬레이션
        try {
            int delay = 1000 + random.nextInt(2000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PointException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 10% 확률로 실패 시뮬레이션
        if (random.nextInt(100) < 10) {
            log.error("Payment failed for user: {}, amount: {}", userId, amount);
            throw new PointException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.info("Payment successful for user: {}, amount: {}", userId, amount);
    }

    public boolean verifyPaymentStatus(String idempotencyKey) {
        log.info("Verifying payment status for idempotencyKey: {}", idempotencyKey);
        // 90% 확률로 성공 반환
        return random.nextInt(100) < 90;
    }
}
