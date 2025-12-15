package com.quizgenix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // --- EXISTING METHODS (KEPT) ---
    long countByUserAndCreatedAtAfter(User user, LocalDateTime date);

    long countByUser(User user);

    @Query("SELECT AVG(q.score) FROM Quiz q WHERE q.user = :user")
    Double findAverageScoreByUser(@Param("user") User user);

    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.user = :user AND MONTH(q.createdAt) = MONTH(CURRENT_DATE) AND YEAR(q.createdAt) = YEAR(CURRENT_DATE)")
    int countQuizzesThisMonth(@Param("user") User user);

    List<Quiz> findTop5ByUserOrderByCreatedAtDesc(User user);

    // Old list method (can be kept or removed if unused)
    List<Quiz> findByUserOrderByCreatedAtDesc(User user);

    List<Quiz> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime date);

    @Query("SELECT DISTINCT q.topic FROM Quiz q WHERE q.user = :user ORDER BY q.topic ASC")
    List<String> findDistinctTopicsByUser(@Param("user") User user);

    // NEW: PAGINATION & DATE RANGE ---
    Page<Quiz> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // This fetches all quizzes so we can delete them before deleting the user
    List<Quiz> findByUser(User user);

}