package com.quizgenix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizgenix.model.Question;
import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.repository.UserRepository;

@Service
public class QuizService {

    @Autowired
    private AiService aiService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    public Quiz createQuiz(String topic, String difficulty, int count, User user) {
        List<Question> questions = aiService.generateQuestions(topic, difficulty, count);

        Quiz quiz = new Quiz();
        quiz.setTopic(topic);
        quiz.setDifficulty(difficulty);
        quiz.setTotalQuestions(count);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUser(user);

        for (Question q : questions) {
            q.setQuiz(quiz);
        }
        quiz.setQuestions(questions);

        return quizRepository.save(quiz);
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    @Transactional
    public int calculateScore(Long quizId, Map<String, String> allParams) {
        Quiz quiz = getQuizById(quizId);
        List<Question> questions = quiz.getQuestions();

        int correctAnswers = 0;

        // 1. Calculate Correct Answers
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String selectedOption = allParams.get("answer_" + i);

            if (selectedOption != null && selectedOption.equalsIgnoreCase(q.getCorrectAnswer())) {
                correctAnswers++;
            }
        }

        int percentage = 0;
        if (questions.size() > 0) {
            percentage = (int) ((correctAnswers * 100.0) / questions.size());
        }
        quiz.setScore(percentage);
        quizRepository.save(quiz); // Update quiz with the score
        // -------------------------------------

        // 2. Calculate XP (Based on your custom difficulty rules)
        int xpPerQuestion = 1; // Default: Easy
        String difficulty = quiz.getDifficulty();

        if ("Medium".equalsIgnoreCase(difficulty)) {
            xpPerQuestion = 2;
        } else if ("Hard".equalsIgnoreCase(difficulty)) {
            xpPerQuestion = 3;
        }

        int xpEarned = correctAnswers * xpPerQuestion;

        // 3. Update User XP
        if (xpEarned > 0) {
            User user = quiz.getUser();
            int currentTotal = user.getTotalXp();
            user.setTotalXp(currentTotal + xpEarned);
            userRepository.save(user);
        }

        return correctAnswers; // Return raw score (e.g., 8 out of 10)
    }
}