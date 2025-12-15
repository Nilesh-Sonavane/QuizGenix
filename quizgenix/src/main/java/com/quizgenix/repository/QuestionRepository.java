package com.quizgenix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quizgenix.model.Question;
import com.quizgenix.model.Quiz;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // This allows us to fetch all questions for a list of quizzes at once
    List<Question> findByQuizIn(List<Quiz> quizzes);
}