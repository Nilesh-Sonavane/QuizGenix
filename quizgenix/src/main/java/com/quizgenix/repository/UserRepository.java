package com.quizgenix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizgenix.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        User findByEmail(String email);

        User findByVerificationCode(String code);

        User findByResetPasswordToken(String token);

        boolean existsByEmail(String email);

        @Query("SELECT COUNT(u) FROM User u WHERE u.totalXp > :xp")
        long countUsersWithMoreXp(@Param("xp") int totalXp);

        List<User> findTop5ByOrderByTotalXpDesc();

        List<User> findTop20ByOrderByTotalXpDesc();

        // Dashboard Counts
        long countByRoleNot(String role);

        @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count FROM users WHERE created_at >= :startDate AND role != 'ADMIN' GROUP BY month ORDER BY month ASC", nativeQuery = true)
        List<Object[]> countUsersByMonthFromDate(@Param("startDate") LocalDateTime startDate);

        @Query(value = "SELECT YEAR(created_at) as year, COUNT(*) as count FROM users WHERE role != 'ADMIN' GROUP BY year ORDER BY year ASC", nativeQuery = true)
        List<Object[]> countUsersByYear();

        @Query("SELECT u.activePlan, COUNT(u) FROM User u WHERE u.role != 'ADMIN' GROUP BY u.activePlan")
        List<Object[]> countUsersByActivePlan();

        // --- USER MANAGEMENT ---
        // Just find all regular users sorted by newest first
        List<User> findByRoleNotOrderByCreatedAtDesc(String role);
}