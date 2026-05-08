package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import com.example.kcomproject.domain.outbox.service.OutboxScheduler;
import com.example.kcomproject.global.kafka.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class OutboxResilienceTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @MockBean
    private LockProvider lockProvider;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // Mock ShedLock to always succeed
        when(lockProvider.lock(any())).thenReturn(Optional.of(mock(SimpleLock.class)));

        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("[STRESS-TEST-005] Kafka 장애 시에도 주문은 성공하고, 인프라 복구 시 아웃박스가 재발행되어야 한다")
    void outboxResilienceTest() {
        // 1. Kafka 장애 시뮬레이션 (발행 시 예외 발생)
        doThrow(new RuntimeException("Kafka is down")).when(kafkaProducerService).send(anyString(), anyString(), anyString(), anyMap());

        // 2. 아웃박스 데이터 생성
        Outbox outbox = Outbox.builder()
                .aggregateType("ORDER")
                .aggregateId(100L)
                .partitionKey("1")
                .payload("{\"orderId\":100}")
                .status(OutboxStatus.INIT)
                .build();
        outboxRepository.save(outbox);

        // 3. 스케줄러 실행 (장애 상황)
        outboxScheduler.processOutbox();

        // 4. 상태 확인: FAILED로 변경되었는지 확인
        List<Outbox> outboxes = outboxRepository.findAll();
        Outbox failedOutbox = outboxes.get(0);
        log.info("Actual Outbox Status after failure: {}", failedOutbox.getStatus());
        assertThat(failedOutbox.getStatus()).isEqualTo(OutboxStatus.FAILED);

        // 5. Kafka 복구 시뮬레이션
        reset(kafkaProducerService);
        doNothing().when(kafkaProducerService).send(anyString(), anyString(), anyString(), anyMap());

        // 6. 스케줄러 재실행
        outboxScheduler.processOutbox();

        // 7. 최종 확인: SENT 상태로 변경됨
        Outbox recoveredOutbox = outboxRepository.findAll().get(0);
        assertThat(recoveredOutbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(kafkaProducerService, times(1)).send(anyString(), anyString(), anyString(), anyMap());
    }
}
