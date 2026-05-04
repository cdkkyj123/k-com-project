package com.example.kcomproject.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String COFFEE_ORDERS_TOPIC = "coffee-orders";

    @Bean
    public NewTopic coffeeOrdersTopic() {
        return TopicBuilder.name(COFFEE_ORDERS_TOPIC)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
