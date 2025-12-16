package com.quizgenix.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern; // Use Jakarta, NOT Hibernate
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Column(unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    // STRICT PASSWORD VALIDATION
    @Column(nullable = false)
    @Pattern(
            // Regex: 1 digit, 1 lower, 1 upper, 1 special char, 8+ length
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*\\-+=?_]).{8,}$", message = "Password must contain 8+ chars, 1 uppercase, 1 lowercase, 1 digit, and 1 special char")
    private String password;

    private String role;
    private boolean enabled = false;

    @CreationTimestamp // Automatically sets date when user registers
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verification_code", length = 64)
    private String verificationCode;

    private String profileImage = "profile.png";

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "token_creation_date")
    private LocalDateTime tokenCreationDate; // New field for expiration

    // --- NEW FIELD: TOTAL XP ---
    @Column(columnDefinition = "int default 0")
    private int totalXp = 0; // Stores the user's total experience points

    public int getLevel() {
        return (this.totalXp / 1000) + 1;
    }
    // --- SUBSCRIPTION FIELDS ---

    // Default to "Free"
    @Column(columnDefinition = "varchar(255) default 'Free'")
    private String activePlan = "Free";

    private Double currentPlanPrice = 0.0; // Free plan value is 0.0

    private LocalDateTime planStartDate;
    private LocalDateTime planExpiryDate;

    // Helper: Check if user has a PAID active subscription
    public boolean isPaidSubscriptionActive() {
        return !"Free".equalsIgnoreCase(activePlan)
                && planExpiryDate != null
                && planExpiryDate.isAfter(LocalDateTime.now());
    }

}