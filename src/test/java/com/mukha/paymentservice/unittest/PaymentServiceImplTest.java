package com.mukha.paymentservice.unittest;

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
import com.mukha.paymentservice.service.impl.PaymentServiceImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private CreatePaymentRequest createPaymentRequest;
    private Payment payment;
    private Payment savedPayment;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        createPaymentRequest = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(100));

        payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(2L);
        payment.setPaymentAmount(BigDecimal.valueOf(100));

        savedPayment = new Payment();
        savedPayment.setId(new ObjectId());
        savedPayment.setOrderId(1L);
        savedPayment.setUserId(2L);
        savedPayment.setPaymentAmount(BigDecimal.valueOf(100));
        savedPayment.setCreatedAt(Instant.now());

        paymentResponse = new PaymentResponse(new ObjectId(), 1L, 2L, PaymentStatus.SUCCESS, Instant.now(), BigDecimal.valueOf(100));
    }

    @Test
    void createPaymentShouldSetSuccessStatusWhenRandomNumberIsEven() {
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("42");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(createPaymentRequest);

        assertThat(result).isEqualTo(paymentResponse);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void createPaymentShouldSetFailedStatusWhenRandomNumberIsOdd() {
        savedPayment.setStatus(PaymentStatus.FAILED);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("41");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        paymentService.createPayment(createPaymentRequest);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void createPaymentShouldTrimRandomNumberResponseBeforeParsing() {
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("  42  \n");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(createPaymentRequest);

        assertThat(result).isEqualTo(paymentResponse);
    }

    @Test
    void createPaymentShouldSetTimestampBeforeSaving() {
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("42");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        paymentService.createPayment(createPaymentRequest);

        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void createPaymentShouldSendPaymentCreatedEventWithSavedPaymentData() {
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("42");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        paymentService.createPayment(createPaymentRequest);

        ArgumentCaptor<PaymentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
        verify(paymentEventProducer, times(1)).sendPaymentCreatedEvent(eventCaptor.capture());

        PaymentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.orderId()).isEqualTo(savedPayment.getOrderId());
        assertThat(capturedEvent.userId()).isEqualTo(savedPayment.getUserId());
        assertThat(capturedEvent.status()).isEqualTo(savedPayment.getStatus().name());
        assertThat(capturedEvent.paymentAmount()).isEqualTo(savedPayment.getPaymentAmount());
        assertThat(capturedEvent.createdAt()).isEqualTo(savedPayment.getCreatedAt());
    }

    @Test
    void createPaymentShouldReturnMappedResponse() {
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("42");
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(createPaymentRequest);

        assertThat(result).isSameAs(paymentResponse);
        verify(paymentMapper).toResponse(savedPayment);
    }

    @Test
    void findByUserIdOrOrderIdOrStatusShouldReturnMappedPage() {
        Long userId = 2L;
        Long orderId = 1L;
        PaymentStatus status = PaymentStatus.SUCCESS;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(savedPayment));

        when(paymentRepository.findByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toResponse(savedPayment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.findByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);

        assertThat(result.getContent()).containsExactly(paymentResponse);
    }

    @Test
    void findByUserIdOrOrderIdOrStatusShouldReturnEmptyPageWhenNoResultsFound() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> emptyPage = Page.empty(pageable);

        when(paymentRepository.findByUserIdOrOrderIdOrStatus(null, null, null, pageable)).thenReturn(emptyPage);

        Page<PaymentResponse> result = paymentService.findByUserIdOrOrderIdOrStatus(null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getTotalSumForUserShouldReturnTotalFromRepository() {
        Long userId = 2L;
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-02-01T00:00:00Z");
        BigDecimal expectedTotal = BigDecimal.valueOf(500);

        when(paymentRepository.getTotalSumForUser(userId, from, to)).thenReturn(expectedTotal);

        BigDecimal result = paymentService.getTotalSumForUser(userId, from, to);

        assertThat(result).isEqualTo(expectedTotal);
    }

    @Test
    void getTotalSumForUserShouldReturnZeroWhenRepositoryReturnsNull() {
        Long userId = 2L;
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-02-01T00:00:00Z");

        when(paymentRepository.getTotalSumForUser(userId, from, to)).thenReturn(null);

        BigDecimal result = paymentService.getTotalSumForUser(userId, from, to);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSumForUserShouldThrowExceptionWhenFromIsNull() {
        Instant to = Instant.now();

        assertThatThrownBy(() -> paymentService.getTotalSumForUser(2L, null, to))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(paymentRepository, never()).getTotalSumForUser(any(), any(), any());
    }

    @Test
    void getTotalSumForUserShouldThrowExceptionWhenToIsNull() {
        Instant from = Instant.now();

        assertThatThrownBy(() -> paymentService.getTotalSumForUser(2L, from, null))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(paymentRepository, never()).getTotalSumForUser(any(), any(), any());
    }

    @Test
    void getTotalSumForUserShouldThrowExceptionWhenFromIsAfterTo() {
        Instant from = Instant.parse("2024-02-01T00:00:00Z");
        Instant to = Instant.parse("2024-01-01T00:00:00Z");

        assertThatThrownBy(() -> paymentService.getTotalSumForUser(2L, from, to))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(paymentRepository, never()).getTotalSumForUser(any(), any(), any());
    }

    @Test
    void getTotalSumForAllUsersShouldReturnTotalFromRepository() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-02-01T00:00:00Z");
        BigDecimal expectedTotal = BigDecimal.valueOf(1000);

        when(paymentRepository.getTotalSumForAllUsers(from, to)).thenReturn(expectedTotal);

        BigDecimal result = paymentService.getTotalSumForAllUsers(from, to);

        assertThat(result).isEqualTo(expectedTotal);
    }

    @Test
    void getTotalSumForAllUsersShouldReturnZeroWhenRepositoryReturnsNull() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-02-01T00:00:00Z");

        when(paymentRepository.getTotalSumForAllUsers(from, to)).thenReturn(null);

        BigDecimal result = paymentService.getTotalSumForAllUsers(from, to);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSumForAllUsersShouldThrowExceptionWhenDateRangeIsInvalid() {
        Instant from = Instant.parse("2024-02-01T00:00:00Z");
        Instant to = Instant.parse("2024-01-01T00:00:00Z");

        assertThatThrownBy(() -> paymentService.getTotalSumForAllUsers(from, to))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(paymentRepository, never()).getTotalSumForAllUsers(any(), any());
    }
}
