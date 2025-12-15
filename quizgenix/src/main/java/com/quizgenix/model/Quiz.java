package com.quizgenix.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private String difficulty;
    private int totalQuestions;
    private LocalDateTime createdAt;
    private int score; // Stores the percentage (0-100)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Question> questions;

    public int getCorrectAnswers() {
        // Calculate correct answers based on score percentage
        if (this.totalQuestions == 0)
            return 0;
        return (int) Math.round((this.score / 100.0) * this.totalQuestions);
    }

    public int getXpEarned() {
        // Example Rule: 10 XP per correct answer
        return getCorrectAnswers() * 10;
    }
}