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

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService kafkaProducerService;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "processOutbox", lockAtMostFor = "4s", lockAtLeastFor = "2s")
    public void processOutbox() {
        List<Outbox> outboxes = outboxRepository.findByStatus(OutboxStatus.INIT);
        if (outboxes.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox records", outboxes.size());
        for (Outbox outbox : outboxes) {
            try {
                processRecord(outbox);
            } catch (Exception e) {
                log.error("Failed to process outbox record: {}", outbox.getId(), e);
            }
        }
    }

    @Transactional
    public void processRecord(Outbox outbox) {
        try {
            kafkaProducerService.send("coffee-orders", outbox.getPayload());
            outbox.sendSuccess();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to send outbox record to Kafka: {}", outbox.getId(), e);
            outbox.sendFailed();
            outboxRepository.save(outbox);
            throw e;
        }
    }
}
