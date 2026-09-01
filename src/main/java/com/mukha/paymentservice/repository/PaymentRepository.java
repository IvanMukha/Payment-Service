package com.mukha.paymentservice.repository;

import com.mukha.paymentservice.model.Payment;
import com.mukha.paymentservice.model.status.PaymentStatus;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepository extends MongoRepository<Payment, ObjectId> {

    Page<Payment> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, PaymentStatus status, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'user_id': ?0, 'status': 'SUCCESS', 'timestamp': { '$gte': ?1, '$lte': ?2 } } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$payment_amount' } } }"
    })
    BigDecimal getTotalSumForUser(Long userId, Instant from, Instant to);

    @Aggregation(pipeline = {
            "{ '$match': { 'status': 'SUCCESS', 'timestamp': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$payment_amount' } } }"
    })
    BigDecimal getTotalSumForAllUsers(Instant from, Instant to);
}
