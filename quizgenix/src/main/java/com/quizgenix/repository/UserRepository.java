package com.quizgenix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quizgenix.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user by their email (for login and registration checks)
    User findByEmail(String email);

    // Finds a user by the verification code (Fixes the error in UserService)
    User findByVerificationCode(String code);

    // Finds a user by the reset password token (For Forgot Password)
    User findByResetPasswordToken(String token);

    // Check if email exists (Optional, but good for validation)
    boolean existsByEmail(String email);
}