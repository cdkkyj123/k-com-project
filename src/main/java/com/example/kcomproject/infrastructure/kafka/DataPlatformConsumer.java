package com.example.kcomproject.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataPlatformConsumer {

    @KafkaListener(
            topics = "coffee-orders",
            groupId = "data-platform-group",
            concurrency = "3"
    )
    public void consume(String message) {
        log.info("Received message from coffee-orders topic: {}", message);
        // TODO: 실제 데이터 플랫폼으로 전송하는 Mock API 호출 로직 추가
        simulateDataPlatformTransmission(message);
    }

    private void simulateDataPlatformTransmission(String message) {
        log.debug("Transmitting data to platform: {}", message);
        // 시뮬레이션을 위한 지연 시간
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Successfully transmitted to data platform.");
    }
}
