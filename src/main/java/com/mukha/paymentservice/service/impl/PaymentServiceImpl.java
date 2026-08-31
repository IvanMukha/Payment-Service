package com.mukha.paymentservice.service.impl;

import com.mukha.paymentservice.client.RandomNumberClient;
import com.mukha.paymentservice.dto.request.CreatePaymentRequest;
import com.mukha.paymentservice.dto.response.PaymentResponse;
import com.mukha.paymentservice.exception.InvalidDateRangeException;
import com.mukha.paymentservice.kafka.PaymentEventProducer;
import com.mukha.paymentservice.kafka.event.PaymentCreatedEvent;
import com.mukha.paymentservice.mapper.PaymentMapper;
import com.mukha.paymentservice.model.Payment;
import com.mukha.paymentservice.model.status.PaymentStatus;
import com.mukha.paymentservice.repository.PaymentRepository;
import com.mukha.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        String randomNumber = randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain").trim();
        PaymentStatus paymentStatus = Integer.parseInt(randomNumber) % 2 == 0 ?
                PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        log.debug("Attempting to save new Payment");
        Payment payment = paymentMapper.toEntity(createPaymentRequest);
        payment.setStatus(paymentStatus);
        payment.setTimestamp(Instant.now());

        Payment result = paymentRepository.save(payment);
        log.debug("Successfully create payment with id: {}", result.getId());

        paymentEventProducer.sendPaymentCreatedEvent(new PaymentCreatedEvent(
                result.getOrderId(),
                result.getUserId(),
                result.getStatus().name(),
                result.getPaymentAmount(),
                result.getTimestamp()));

        return paymentMapper.toResponse(result);
    }

    public Page<PaymentResponse> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);
        return payments.map(paymentMapper::toResponse);
    }

    @Override
    public BigDecimal getTotalSumForUser(Long userId, Instant from, Instant to) {
        validateDateRange(from, to);
        BigDecimal total = paymentRepository.getTotalSumForUser(userId, from, to);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalSumForAllUsers(Instant from, Instant to) {
        validateDateRange(from, to);
        BigDecimal total = paymentRepository.getTotalSumForAllUsers(from, to);
        return total != null ? total : BigDecimal.ZERO;
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidDateRangeException();
        }
    }
}

