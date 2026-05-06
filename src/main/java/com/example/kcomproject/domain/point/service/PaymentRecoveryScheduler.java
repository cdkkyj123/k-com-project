package com.example.kcomproject.domain.point.service;

import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private final ChargeHistoryRepository chargeHistoryRepository;
    private final MockPaymentService mockPaymentService;
    private final PointService pointService;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @SchedulerLock(name = "paymentRecoveryLock", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void recoverPendingPayments() {
        log.info("Starting payment recovery scheduler...");
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<ChargeHistory> pendingHistories = chargeHistoryRepository.findAllByStatusAndCreatedAtBefore(
                ChargeStatus.PENDING, threshold);

        log.info("Found {} pending payments for recovery", pendingHistories.size());

        for (ChargeHistory history : pendingHistories) {
            try {
                boolean success = mockPaymentService.verifyPaymentStatus(history.getIdempotencyKey());
                log.info("Payment verified as {} for idempotencyKey: {}. Processing...", 
                        success ? "SUCCESS" : "FAILED", history.getIdempotencyKey());
                
                pointService.processWebhook(history.getIdempotencyKey(), success ? "SUCCESS" : "FAILED");
            } catch (Exception e) {
                log.error("Failed to recover payment for history ID: {}: {}", history.getId(), e.getMessage());
            }
        }
        
        log.info("Payment recovery scheduler finished.");
    }
}
