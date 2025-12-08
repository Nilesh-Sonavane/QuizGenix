package com.quizgenix.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId; // Razorpay Order ID (e.g., order_Kz123...)
    private String paymentId; // Razorpay Payment ID (e.g., pay_Kz456...)
    private String status; // created, paid, failed
    private String amount; // Amount paid
    private String receiptEmail; // User's email
    private String planName; // "Monthly Plan", "Yearly Plan"

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}