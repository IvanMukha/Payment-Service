package com.mukha.paymentservice.repository.impl;

import com.mukha.paymentservice.model.Payment;
import com.mukha.paymentservice.model.status.PaymentStatus;
import com.mukha.paymentservice.repository.PaymentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public Page<Payment> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, PaymentStatus status, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (userId != null) {
            criteriaList.add(Criteria.where("user_id").is(userId));
        }
        if (orderId != null) {
            criteriaList.add(Criteria.where("order_id").is(orderId));
        }
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status.name()));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Payment.class);
        query.with(pageable);
        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return new PageImpl<>(payments, pageable, total);
    }
}

