package com.mukha.paymentservice.mapper;

import com.mukha.paymentservice.dto.request.CreatePaymentRequest;
import com.mukha.paymentservice.dto.response.PaymentResponse;
import com.mukha.paymentservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Payment toEntity(CreatePaymentRequest request);
}
