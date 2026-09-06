package com.mukha.paymentservice.kafka;

import com.mukha.paymentservice.kafka.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${payment-service.kafka.topics.payment-created}")
    private String topic;

    public void sendPaymentCreatedEvent(PaymentCreatedEvent event) {
        log.debug("Sending {} event for orderId: {}", event.eventType(), event.orderId());

        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send payment event for orderId: {}", event.orderId(), ex);
                    } else {
                        log.debug("Sent payment event for orderId: {}, offset: {}",
                                event.orderId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
