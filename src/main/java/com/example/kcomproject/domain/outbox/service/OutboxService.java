package com.example.kcomproject.domain.outbox.service;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import com.example.kcomproject.global.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService kafkaProducerService;

    private static final int MAX_RETRY_COUNT = 5;

    @Transactional
    public void processRecord(Long outboxId) {
        Outbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox record not found: " + outboxId));

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
            // No need to call save() explicitly if dirty checking works, but let's be explicit
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to send outbox record to Kafka: {} - {}", outbox.getId(), e.getMessage());
            outbox.sendFailed();
            outboxRepository.save(outbox);
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
