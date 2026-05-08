package com.example.kcomproject.infrastructure.kafka;

import com.example.kcomproject.domain.order.dto.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class KafkaResilienceTest {

    @Autowired
    private PopularMenuConsumer popularMenuConsumer;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "popular_menus:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        String today = LocalDate.now().format(FORMATTER);
        redissonClient.getScoredSortedSet(KEY_PREFIX + today).clear();
    }

    @Test
    @DisplayName("[RESILIENCE-002] 동일한 주문 메시지가 중복 인입될 때, 인기 메뉴 점수가 중복 합산되지 않고 멱등성이 보장되는지 확인")
    void duplicateMessageIdempotencyTest() throws JsonProcessingException {
        // Given
        Long menuId = 1L;
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, // orderId
                1L,   // userId
                1L,   // storeId
                1000L,
                com.example.kcomproject.domain.order.entity.OrderStatus.PENDING,
                List.of(new OrderCreatedEvent.OrderItemEvent(menuId, 1, 1000L))
        );
        String message = objectMapper.writeValueAsString(event);

        // When: 동일 메시지 2번 소비
        popularMenuConsumer.consume(message);
        popularMenuConsumer.consume(message);

        // Then
        String today = LocalDate.now().format(FORMATTER);
        RScoredSortedSet<Long> popularMenus = redissonClient.getScoredSortedSet(KEY_PREFIX + today);
        Double score = popularMenus.getScore(menuId);

        // 현재 로직은 멱등성이 있으므로 1이어야 함.
        log.info("Menu Score after duplicate messages: {}", score);
        assertThat(score).isEqualTo(1.0); 
    }
}
