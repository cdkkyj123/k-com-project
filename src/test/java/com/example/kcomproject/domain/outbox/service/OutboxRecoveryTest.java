package com.example.kcomproject.domain.outbox.service;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import com.example.kcomproject.global.kafka.KafkaProducerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class OutboxRecoveryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @Test
    @DisplayName("[RESILIENCE-003] DB에 INIT 상태로 남겨진 Outbox 레코드가 스케줄러에 의해 결국 전송되는지 확인 (Saga 복구력)")
    void outboxRecoveryTest() {
        // Given: 전송되지 않은(INIT) 레코드 강제 생성
        Outbox outbox = Outbox.builder()
                .aggregateType("ORDER")
                .aggregateId(1L)
                .payload("{\"orderId\":1}")
                .partitionKey("store-1")
                .status(OutboxStatus.INIT)
                .build();
        outboxRepository.save(outbox);

        // When: 스케줄러 수동 실행 (원래는 @Scheduled에 의해 실행됨)
        outboxScheduler.processOutbox();

        // Then: Kafka로 전송되었고 상태가 SENT로 변경되었는지 확인
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(OutboxStatus.SENT);
        });

        verify(kafkaProducerService, times(1)).send(eq("coffee-orders"), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("[RESILIENCE-004] Kafka 전송 실패 시, Retry Count가 증가하고 결국 DLQ로 이동하는지 확인")
    void outboxToDLQTest() {
        // Given: Kafka 전송 시 무조건 예외 발생하도록 설정
        doThrow(new RuntimeException("Kafka Down")).when(kafkaProducerService)
                .send(anyString(), anyString(), anyString(), anyMap());

        Outbox outbox = Outbox.builder()
                .aggregateType("ORDER")
                .aggregateId(2L)
                .payload("{\"orderId\":2}")
                .partitionKey("store-1")
                .status(OutboxStatus.INIT)
                .build();
        outboxRepository.save(outbox);

        // When: 스케줄러를 최대 시도 횟수(5회)만큼 반복 실행
        for (int i = 0; i < 6; i++) {
            outboxScheduler.processOutbox();
        }

        // Then: 최종적으로 DLQ 상태가 되어야 함
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.DLQ);
        assertThat(result.getRetryCount()).isEqualTo(5);
    }
}
