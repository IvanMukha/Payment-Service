package com.mukha.paymentservice.service;

import com.mukha.paymentservice.dto.request.CreatePaymentRequest;
import com.mukha.paymentservice.dto.response.PaymentResponse;
import com.mukha.paymentservice.model.status.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);

    Page<PaymentResponse> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, PaymentStatus status, Pageable pageable);

    BigDecimal getTotalSumForUser(Long userId, Instant from, Instant to);

    BigDecimal getTotalSumForAllUsers(Instant from, Instant to);
}

