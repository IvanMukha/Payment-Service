package com.mukha.paymentservice.repository;

import com.mukha.paymentservice.model.Payment;
import com.mukha.paymentservice.model.status.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepositoryCustom {

    Page<Payment> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, PaymentStatus status, Pageable pageable);
}

