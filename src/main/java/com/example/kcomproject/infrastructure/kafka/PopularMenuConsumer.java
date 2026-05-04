package com.example.kcomproject.infrastructure.kafka;

import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuConsumer {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "popular_menus:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @KafkaListener(
            topics = "coffee-orders",
            groupId = "popular-menu-group",
            concurrency = "3"
    )
    public void consume(String message) {
        try {
            OrderRequest orderRequest = objectMapper.readValue(message, OrderRequest.class);
            Long menuId = orderRequest.menuId();

            if (menuId == null) {
                log.warn("MenuId is null in order message: {}", message);
                return;
            }

            String today = LocalDate.now().format(FORMATTER);
            String key = KEY_PREFIX + today;

            RScoredSortedSet<Long> popularMenus = redissonClient.getScoredSortedSet(key);
            popularMenus.addScore(menuId, 1);
            popularMenus.expire(Duration.ofDays(8));

            log.debug("Updated popular menu score: menuId={}, date={}", menuId, today);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse order message: {}", message, e);
        }
    }
}
