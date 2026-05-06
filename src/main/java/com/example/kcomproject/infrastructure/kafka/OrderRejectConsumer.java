package com.example.kcomproject.infrastructure.kafka;

import com.example.kcomproject.domain.order.service.OrderTransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRejectConsumer {

    private final OrderTransactionService orderTransactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "coffee-order-reject",
            groupId = "order-group"
    )
    public void consume(String message) {
        log.info("Received reject message: {}", message);
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            Long orderId = jsonNode.get("orderId").asLong();
            orderTransactionService.rejectOrder(orderId);
        } catch (Exception e) {
            log.error("Failed to process order reject message: {}", message, e);
        }
    }
}
