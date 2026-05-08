package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
public class PointResilienceTest {

    private static final Logger log = LoggerFactory.getLogger(PointResilienceTest.class);

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChargeHistoryRepository chargeHistoryRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockBean
    private MockPaymentService mockPaymentService;

    private Long userId;

    @BeforeEach
    void setUp() {
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
        pointHistoryRepository.deleteAll();
        chargeHistoryRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .pointBalance(0L)
                .build();
        userId = userRepository.save(user).getId();
    }

    @Test
    @DisplayName("[RESILIENCE-001] 외부 PG 응답이 극도로 느릴 때(15초), Facade의 tryLock(10초)이 스레드 점유를 차단하는지 확인")
    void slowPoisonTimeoutTest() throws InterruptedException {
        // Mock: 15초 지연 발생 (tryLock 대기 시간 10초보다 김)
        doAnswer(invocation -> {
            Thread.sleep(15000);
            return null;
        }).when(mockPaymentService).processPayment(anyLong(), anyLong());

        int threadCount = 5; // 적은 수의 스레드로도 확인 가능
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger lockTimeoutCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    pointFacade.chargePoint(userId, 1000L, "idempotency-" + index);
                    successCount.incrementAndGet();
                } catch (PointException e) {
                    if (e.getErrorCode() == ErrorCode.POINT_LOCK_FAILED) {
                        lockTimeoutCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Unexpected error: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = (System.currentTimeMillis() - startTime) / 1000;

        log.info("Duration: {}s", duration);
        log.info("Success Count: {}", successCount.get());
        log.info("Lock Timeout Count: {}", lockTimeoutCount.get());

        // 1명은 락을 획득하고 15초 대기 중일 것임.
        // 나머지는 10초 대기 후 LOCK_FAILED 발생해야 함.
        assertThat(lockTimeoutCount.get()).isGreaterThanOrEqualTo(threadCount - 1);
        assertThat(duration).isLessThan(20); // 15초 + 알파 정도에서 끝나야 함.
    }
}
