package com.quizgenix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quizgenix.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrderId(String orderId);
}