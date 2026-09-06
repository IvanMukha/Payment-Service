package com.mukha.paymentservice.dto.response;

import com.mukha.paymentservice.model.status.PaymentStatus;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(

        ObjectId id,

        Long orderId,

        Long userId,

        PaymentStatus status,

        Instant createdAt,

        BigDecimal paymentAmount) {

}
