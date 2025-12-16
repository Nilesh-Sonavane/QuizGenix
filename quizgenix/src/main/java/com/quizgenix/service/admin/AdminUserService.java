package com.quizgenix.service.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizgenix.model.User;
import com.quizgenix.repository.UserRepository;
import com.quizgenix.service.EmailService; // Import

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService; // Inject Email Service

    public List<User> getAllUsers() {
        return userRepository.findByRoleNotOrderByCreatedAtDesc("ADMIN");
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Change Status
        boolean newStatus = !user.isEnabled();
        user.setEnabled(newStatus);
        userRepository.save(user);

        // 2. Send Notification Email
        try {
            emailService.sendAccountStatusEmail(user, newStatus);
        } catch (Exception e) {
            System.err.println("Error sending ban/unban email: " + e.getMessage());
            // We catch exception so functionality doesn't break if SMTP is down
        }
    }
}