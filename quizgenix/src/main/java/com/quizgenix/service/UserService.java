package com.quizgenix.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects; // Import Objects
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

    // ... (Your existing methods remain the same) ...
    // Copy content from previous file...
    // The only change is in the save() method below:

    public void register(User user, String siteURL) throws Exception {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new Exception("There is already an account registered with the email " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (userRepository.count() == 0) {
            user.setRole("ADMIN");
            user.setEnabled(true);
        } else {
            user.setRole("USER");
            user.setEnabled(false);
        }
        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);
        user.setActivePlan("Free");
        user.setCurrentPlanPrice(0.0);
        user.setPlanStartDate(LocalDateTime.now());
        user.setPlanExpiryDate(null);

        userRepository.save(user);

        if (!user.isEnabled()) {
            emailService.sendVerificationEmail(user, siteURL);
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Inside public void save(User user)
    public void save(User user) {
        // FIX: Ensure User is not null
        userRepository.save(Objects.requireNonNull(user, "User cannot be null"));
    }

    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);
        if (user == null || user.isEnabled()) {
            return false;
        } else {
            user.setVerificationCode(null);
            user.setEnabled(true);
            userRepository.save(user);
            return true;
        }
    }

    public String updateResetPasswordToken(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setTokenCreationDate(LocalDateTime.now());
            userRepository.save(user);
            return token;
        } else {
            throw new UsernameNotFoundException("Could not find any customer with the email " + email);
        }
    }

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

    public boolean isTokenExpired(LocalDateTime tokenCreationDate) {
        if (tokenCreationDate == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration diff = Duration.between(tokenCreationDate, now);
        return diff.toMinutes() >= EXPIRE_TOKEN_AFTER_MINUTES;
    }

    public User checkAndExpirePlan(User user) {
        if (user.getPlanExpiryDate() != null && LocalDateTime.now().isAfter(user.getPlanExpiryDate())) {
            user.setActivePlan("Free");
            user.setCurrentPlanPrice(0.0);
            user.setPlanExpiryDate(null);
            user.setPlanStartDate(null);
            return userRepository.save(user);
        }
        return user;
    }
}