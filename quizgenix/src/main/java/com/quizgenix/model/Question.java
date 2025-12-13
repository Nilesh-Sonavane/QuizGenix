package com.quizgenix.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String text;

    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ElementCollection
    private List<String> options;

    // ---------------------------------------------------------
    // MANUALLY ADD THIS METHOD TO FIX THE THYMELEAF ERROR
    // ---------------------------------------------------------
    public int getCorrectAnswerIndex() {
        if (options == null || correctAnswer == null) {
            return -1;
        }

        // Logic 1: If correctAnswer is the full text (e.g., "Paris"), find its index
        int index = options.indexOf(correctAnswer);
        if (index != -1) {
            return index;
        }

        // Logic 2: If correctAnswer is just a letter "A", "B", "C", "D"
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