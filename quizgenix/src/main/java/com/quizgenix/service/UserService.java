package com.quizgenix.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizgenix.model.User;
import com.quizgenix.repository.UserRepository;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private static final long EXPIRE_TOKEN_AFTER_MINUTES = 30;

    // ========================================================================
    // 1. REGISTRATION LOGIC (First User = ADMIN & ENABLED)
    // ========================================================================

    public void register(User user, String siteURL) throws Exception {
        // 1. Check if email exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new Exception("There is already an account registered with the email " + user.getEmail());
        }

        // 2. Encode Password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. ASSIGN ROLE & STATUS
        if (userRepository.count() == 0) {
            // FIRST USER: Admin and Active immediately
            user.setRole("ADMIN");
            user.setEnabled(true);
        } else {
            // OTHER USERS: Regular User and Disabled (must verify email)
            user.setRole("USER");
            user.setEnabled(false);
        }

        // 4. Generate Verification Code (Needed for regular users)
        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);

        // 5. Save User
        userRepository.save(user);

        // 6. Send Verification Email (ONLY if the user is NOT enabled)
        // We don't send this email to the Admin since they are already active.
        if (!user.isEnabled()) {
            emailService.sendVerificationEmail(user, siteURL);
        }
    }
    // ========================================================================
    // 2. EMAIL VERIFICATION LOGIC
    // ========================================================================

    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);

        if (user == null || user.isEnabled()) {
            return false;
        } else {
            user.setVerificationCode(null); // Clear code after use
            user.setEnabled(true); // Enable the account
            userRepository.save(user);
            return true;
        }
    }

    // ========================================================================
    // 3. FORGOT PASSWORD LOGIC (Token Generation)
    // ========================================================================

    public String updateResetPasswordToken(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setTokenCreationDate(LocalDateTime.now()); // Capture timestamp
            userRepository.save(user);
            return token;
        } else {
            throw new UsernameNotFoundException("Could not find any customer with the email " + email);
        }
    }

    // ========================================================================
    // 4. FORGOT PASSWORD LOGIC (Validation & Update)
    // ========================================================================

    public User getByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token);
    }

    public void updatePassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        user.setResetPasswordToken(null);
        user.setTokenCreationDate(null);

        userRepository.save(user);
    }

    // Check if token is older than 30 minutes
    public boolean isTokenExpired(LocalDateTime tokenCreationDate) {
        if (tokenCreationDate == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration diff = Duration.between(tokenCreationDate, now);
        return diff.toMinutes() >= EXPIRE_TOKEN_AFTER_MINUTES;
    }

    // Inside UserService.java
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}