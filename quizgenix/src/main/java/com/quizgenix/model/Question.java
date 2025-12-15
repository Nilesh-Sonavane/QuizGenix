package com.quizgenix.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // 🟢 Fixes Duplicate Issue
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // 🟢 Only ID determines uniqueness
    private Long id;

    @Column(length = 1000)
    private String text;

    private String correctAnswer; // e.g., "A"
    private String userAnswer; // e.g., "A programming language"

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ElementCollection(fetch = FetchType.EAGER) // Ensure options are loaded
    private List<String> options;

    // Helper to find the index of the correct answer (0=A, 1=B, etc.)
    public int getCorrectAnswerIndex() {
        if (options == null || correctAnswer == null)
            return -1;

        // 1. Try matching full text
        int idx = options.indexOf(correctAnswer);
        if (idx != -1)
            return idx;

        // 2. Try matching Letter (A, B, C, D)
        switch (correctAnswer.trim().toUpperCase()) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            case "D":
                return 3;
            default:
                return -1;
        }
    }
}