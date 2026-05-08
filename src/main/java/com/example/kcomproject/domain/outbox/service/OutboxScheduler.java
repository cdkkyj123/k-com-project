package com.example.kcomproject.domain.outbox.service;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import com.example.kcomproject.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "processOutbox", lockAtMostFor = "4s", lockAtLeastFor = "2s")
    public void processOutbox() {
        log.info("Outbox scheduler started");
        List<Outbox> targets = outboxRepository.findTop100ByStatusInOrderByIdAsc(
                List.of(OutboxStatus.INIT, OutboxStatus.FAILED));

        if (targets.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox records", targets.size());
        for (Outbox outbox : targets) {
            try {
                outboxService.processRecord(outbox.getId());
            } catch (Exception e) {
                log.error("Unexpected error processing outbox record: {}", outbox.getId(), e);
            }
        }
    }
}
