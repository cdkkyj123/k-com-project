package com.example.kcomproject.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, String key, String payload, Map<String, String> headers) {
        log.info("Sending message to topic {}: key={}, payload={}", topic, key, payload);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        
        if (headers != null) {
            headers.forEach((k, v) -> 
                record.headers().add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8)))
            );
        }
        
        kafkaTemplate.send(record);
    }
}
