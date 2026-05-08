package com.example.kcomproject.domain.outbox.service;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.global.kafka.KafkaProducerService;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService kafkaProducerService;

    private static final int MAX_RETRY_COUNT = 5;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "processOutbox", lockAtMostFor = "4s", lockAtLeastFor = "2s")
    public void processOutbox() {
        List<Outbox> targets = outboxRepository.findTop100ByStatusInOrderByIdAsc(
                List.of(OutboxStatus.INIT, OutboxStatus.FAILED));

        if (targets.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox records", targets.size());
        for (Outbox outbox : targets) {
            try {
                processRecord(outbox);
            } catch (Exception e) {
                log.error("Failed to process outbox record: {}", outbox.getId());
            }
        }
    }

    @Transactional
    public void processRecord(Outbox outbox) {
        if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
            log.error("Outbox record reached max retry count. Moving to DLQ: {}", outbox.getId());
            moveToDLQ(outbox);
            return;
        }

        try {
            // storeId를 키와 헤더에 포함시켜 전송 (ADR-ORDER-001)
            Map<String, String> headers = Map.of("storeId", outbox.getPartitionKey());
            kafkaProducerService.send("coffee-orders", outbox.getPartitionKey(), outbox.getPayload(), headers);
            
            outbox.sendSuccess();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to send outbox record to Kafka: {}", outbox.getId());
            outbox.sendFailed();
            outboxRepository.save(outbox);
            throw e;
        }
    }

    private void moveToDLQ(Outbox outbox) {
        try {
            outbox.moveToDLQ();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to move outbox record to DLQ: {}", outbox.getId(), e);
        }
    }
}
