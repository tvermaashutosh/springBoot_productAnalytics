package com.example.productAnalytics.consumer;

import com.example.productAnalytics.dto.ViewEvent;
import com.example.productAnalytics.model.ProductView;
import com.example.productAnalytics.repository.ProductViewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumer {
    private final ProductViewRepository productViewRepository;

    @Transactional
    @KafkaListener(
            topics = "${spring.kafka.topic}",
            groupId = "${spring.kafka.consumer." + ConsumerConfig.GROUP_ID_CONFIG + "}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "2"
    )
    public void consume(@Payload ViewEvent viewEvent,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {
        ProductView productView = productViewRepository
                .findByProductIdAndUserIp(viewEvent.getProductId(), viewEvent.getUserIp())
                .map(existingProductView -> {
                    existingProductView.setViewCount(existingProductView.getViewCount() + 1);
                    return existingProductView;
                })
                .orElseGet(() -> {
                    ProductView newProductView = new ProductView();
                    newProductView.setProductId(viewEvent.getProductId());
                    newProductView.setUserIp(viewEvent.getUserIp());
                    newProductView.setViewCount(1);
                    return productViewRepository.save(newProductView);
                });

        ack.acknowledge();

        log.info("Consumed 1 record, id: {}, partition: {}, offset: {}", viewEvent.getId(), partition, offset);
    }
}
