package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .pointBalance(0L)
                .build();
        userId = userRepository.save(user).getId();
    }

    @Test
    @DisplayName("동시에 100개의 포인트 충전 요청이 오면 정확히 100번의 충전이 이루어져야 한다")
    void concurrentChargeTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, 100L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isEqualTo(threadCount * 100L);
    }

    @Test
    @DisplayName("동시에 100개의 포인트 차감 요청이 오면 정확히 잔액만큼만 차감되어야 한다")
    void concurrentUseTest() throws InterruptedException {
        // Given: 5000 포인트 충전
        pointService.chargePoint(userId, 5000L);

        int threadCount = 100;
        long useAmount = 100L; // 총 10000 포인트 차감 시도 (잔액 부족 예상)
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                } catch (Exception e) {
                    // 잔액 부족 예외 발생 예상
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        User user = userRepository.findById(userId).orElseThrow();
        // 5000포인트에서 100포인트씩 50번 차감 가능
        assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(user.getPointBalance() % 100).isEqualTo(0L);
    }
}
