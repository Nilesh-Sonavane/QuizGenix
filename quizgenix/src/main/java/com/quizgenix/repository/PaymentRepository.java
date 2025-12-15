package com.quizgenix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quizgenix.model.Payment;
import com.quizgenix.model.User;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrderId(String orderId);

    List<Payment> findByUser(User user);
}