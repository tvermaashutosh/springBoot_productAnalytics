package com.example.productAnalytics.producer;

import com.example.productAnalytics.dto.ViewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, ViewEvent> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String topic;

    public void produce(ViewEvent viewEvent) {
        kafkaTemplate.send(topic, viewEvent)
                .whenComplete((result, e) -> {
                    if (e != null)
                        log.info("Error producing 1 record, id: {} ", viewEvent.getId(), e);
                    else
                        log.info("Produced 1 record, id: {} ", viewEvent.getId());
                });
    }
}
