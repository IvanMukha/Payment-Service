package com.mukha.paymentservice.unittest;

import com.mukha.paymentservice.kafka.PaymentEventProducer;
import com.mukha.paymentservice.kafka.event.PaymentCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    private final String topic = "test-payment-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentEventProducer, "topic", topic);
    }

    @Test
    void shouldSendEventSuccessfully() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                123L,
                456L,
                "PENDING",
                new BigDecimal("100.00"),
                Instant.now()
        );

        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentCreatedEvent.class)))
                .thenReturn(future);

        paymentEventProducer.sendPaymentCreatedEvent(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaymentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals(topic, topicCaptor.getValue());
        assertEquals("123", keyCaptor.getValue());
        assertEquals(event, eventCaptor.getValue());
        assertEquals("CREATE_PAYMENT", eventCaptor.getValue().eventType());
    }

    @Test
    void shouldHandleExceptionWhenSendFails() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                123L,
                456L,
                "PENDING",
                new BigDecimal("100.00"),
                Instant.now()
        );

        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker is down"));

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentCreatedEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> paymentEventProducer.sendPaymentCreatedEvent(event));

        verify(kafkaTemplate).send(eq(topic), eq("123"), eq(event));
    }
}