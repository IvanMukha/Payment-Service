package com.mukha.paymentservice.controller;

import com.mukha.paymentservice.dto.request.CreatePaymentRequest;
import com.mukha.paymentservice.dto.response.PaymentResponse;
import com.mukha.paymentservice.model.status.PaymentStatus;
import com.mukha.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'user')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin', 'user')")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {
        Page<PaymentResponse> payments = paymentService.findByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}/total")
    @PreAuthorize("hasAnyAuthority('admin', 'user')")
    public ResponseEntity<BigDecimal> getTotalForCurrentUser(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        BigDecimal total = paymentService.getTotalSumForUser(userId, from, to);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/total")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<BigDecimal> getTotalForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        BigDecimal total = paymentService.getTotalSumForAllUsers(from, to);
        return ResponseEntity.ok(total);
    }
}

