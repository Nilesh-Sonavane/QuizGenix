package com.quizgenix.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "contact_messages")
@Data
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime submittedAt;

    // --- NEW FIELDS ---
    private boolean responded = false;

    @Column(columnDefinition = "TEXT")
    private String responseContent;

    private LocalDateTime respondedAt;

    public ContactMessage() {
        this.submittedAt = LocalDateTime.now();
    }
}