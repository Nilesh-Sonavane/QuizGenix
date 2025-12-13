package com.quizgenix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // --- EXISTING METHODS (KEPT) ---

    // Used for Plan Restrictions (e.g., checking last 24h)
    long countByUserAndCreatedAtAfter(User user, LocalDateTime date);

    // Used for Total Quizzes Card
    long countByUser(User user);

    // Used for Average Score Card
    @Query("SELECT AVG(q.score) FROM Quiz q WHERE q.user = :user")
    Double findAverageScoreByUser(@Param("user") User user);

    // --- NEW METHODS FOR DASHBOARD ---

    // 1. Monthly Limit Check (For the "10 Free Quizzes" badge logic)
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.user = :user AND MONTH(q.createdAt) = MONTH(CURRENT_DATE) AND YEAR(q.createdAt) = YEAR(CURRENT_DATE)")
    int countQuizzesThisMonth(@Param("user") User user);

    // 2. Recent Activity Timeline (Fetches the last 5 quizzes)
    List<Quiz> findTop5ByUserOrderByCreatedAtDesc(User user);
}