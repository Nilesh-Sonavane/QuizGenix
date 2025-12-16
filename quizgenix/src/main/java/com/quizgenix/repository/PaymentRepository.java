package com.quizgenix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizgenix.model.Payment;
import com.quizgenix.model.User;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrderId(String orderId);

    List<Payment> findByUser(User user);

    // admin

    // 1. Total Revenue: Cast String amount to Double for summation
    // We only count 'paid' status
    @Query("SELECT SUM(CAST(p.amount AS double)) FROM Payment p WHERE p.status = 'paid'")
    Double sumTotalRevenue();

    // 2. For Pie Chart: Group by planName (e.g., 'Monthly Plan', 'Yearly Plan')
    @Query("SELECT p.planName, COUNT(p) FROM Payment p WHERE p.status = 'paid' GROUP BY p.planName")
    List<Object[]> countByPlanNameGrouped();

    // Group Plans by Name, filtered by date
    @Query("SELECT p.planName, COUNT(p) FROM Payment p " +
            "WHERE p.status = 'paid' AND p.createdAt >= :startDate " +
            "GROUP BY p.planName")
    List<Object[]> countByPlanNameGroupedFromDate(@Param("startDate") LocalDateTime startDate);

}