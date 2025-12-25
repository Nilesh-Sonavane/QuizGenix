package com.quizgenix.dto;

import java.time.LocalDateTime;

public class SystemEventDTO {
    private String type; // e.g., "Payment", "User", "Quiz"
    private String description; // e.g., "New Subscription: Pro Plan"
    private LocalDateTime date;
    private String status; // "Success", "Pending", "Failed"

    // Constructor
    public SystemEventDTO(String type, String description, LocalDateTime date, String status) {
        this.type = type;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
}