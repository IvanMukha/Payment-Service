package com.mukha.paymentservice.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedEvent(
        String eventType,
        Long orderId,
        Long userId,
        String status,
        BigDecimal paymentAmount,
        Instant createdAt) {

    public static final String EVENT_TYPE = "CREATE_PAYMENT";

    public PaymentCreatedEvent(Long orderId, Long userId, String status, BigDecimal paymentAmount, Instant createdAt) {
        this(EVENT_TYPE, orderId, userId, status, paymentAmount, createdAt);
    }
}