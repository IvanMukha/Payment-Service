package com.mukha.paymentservice.integrationtest;

import com.mukha.paymentservice.dto.request.CreatePaymentRequest;
import com.mukha.paymentservice.model.Payment;
import com.mukha.paymentservice.model.status.PaymentStatus;
import com.mukha.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createPaymentShouldReturnCreatedWhenRequestIsValid() throws Exception {
        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("42");
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(150));

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()))
                .andExpect(jsonPath("$.paymentAmount").value(150));

        verify(paymentEventProducer).sendPaymentCreatedEvent(any());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createPaymentShouldReturnFailedStatusWhenRandomNumberIsOdd() throws Exception {
        when(randomNumberClient.getRandomInteger(1, 1, 100, 1, 10, "plain")).thenReturn("41");
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(150));

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(PaymentStatus.FAILED.name()));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createPaymentShouldReturnBadRequestWhenPaymentAmountIsNegative() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(-10));

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.paymentAmount").exists());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void createPaymentShouldReturnBadRequestWhenOrderIdIsMissing() throws Exception {
        String requestJson = "{\"userId\":2,\"paymentAmount\":100}";

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.orderId").exists());
    }

    @Test
    void createPaymentShouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(150));

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "guest")
    void createPaymentShouldReturnForbiddenWhenUserLacksRequiredAuthority() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 2L, BigDecimal.valueOf(150));

        mockMvc.perform(post("/v1/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getPaymentsShouldReturnPageFilteredByUserId() throws Exception {
        savePayment(1L, 2L, PaymentStatus.SUCCESS, BigDecimal.valueOf(100));
        savePayment(3L, 4L, PaymentStatus.FAILED, BigDecimal.valueOf(200));

        mockMvc.perform(get("/v1/api/payments")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(2));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getPaymentsShouldReturnEmptyPageWhenNothingMatches() throws Exception {
        mockMvc.perform(get("/v1/api/payments")
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getTotalForCurrentUserShouldReturnSumOfSuccessfulPayments() throws Exception {
        savePayment(1L, 2L, PaymentStatus.SUCCESS, BigDecimal.valueOf(100));
        savePayment(2L, 2L, PaymentStatus.SUCCESS, BigDecimal.valueOf(50));
        savePayment(3L, 2L, PaymentStatus.FAILED, BigDecimal.valueOf(999));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/v1/api/payments/user/2/total")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(150));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getTotalForCurrentUserShouldReturnZeroWhenNoPaymentsFound() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/v1/api/payments/user/999/total")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getTotalForCurrentUserShouldReturnBadRequestWhenFromIsAfterTo() throws Exception {
        Instant from = Instant.now();
        Instant to = from.minus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/v1/api/payments/user/2/total")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "admin")
    void getTotalForAllUsersShouldReturnSumAcrossUsers() throws Exception {
        savePayment(1L, 2L, PaymentStatus.SUCCESS, BigDecimal.valueOf(100));
        savePayment(2L, 3L, PaymentStatus.SUCCESS, BigDecimal.valueOf(200));
        savePayment(3L, 4L, PaymentStatus.FAILED, BigDecimal.valueOf(999));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/v1/api/payments/total")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(300));
    }

    @Test
    @WithMockUser(authorities = "user")
    void getTotalForAllUsersShouldReturnForbiddenForNonAdminUser() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/v1/api/payments/total")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isForbidden());
    }

    private void savePayment(Long orderId, Long userId, PaymentStatus status, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setStatus(status);
        payment.setCreatedAt(Instant.now());
        payment.setPaymentAmount(amount);
        paymentRepository.save(payment);
    }
}
