package com.quizgenix.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        Quiz quiz = quizRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 🟢 PRE-CLEANUP: Remove duplicates immediately when fetching
        Set<Question> uniqueQs = new LinkedHashSet<>(quiz.getQuestions());
        quiz.setQuestions(new ArrayList<>(uniqueQs));

        return quiz;
    }

    @Transactional
    public int calculateScore(Long quizId, Map<String, String> allParams) {
        Quiz quiz = getQuizById(quizId);

        // 🟢 FIX 1: Ensure we iterate over unique questions only
        Set<Question> uniqueQuestions = new LinkedHashSet<>(quiz.getQuestions());
        List<Question> questions = new ArrayList<>(uniqueQuestions);

        int correctAnswers = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);

            // Get user input (from hidden fields answer_0, answer_1...)
            String userSelectedText = allParams.get("answer_" + i);

            // A. Normalize and Save User Answer
            if (userSelectedText != null) {
                userSelectedText = userSelectedText.trim();
                q.setUserAnswer(userSelectedText);
            } else {
                q.setUserAnswer("");
            }

            // B. Resolve the "Correct Text" from the DB "Correct Letter"
            String dbAnswerRaw = q.getCorrectAnswer(); // e.g., "A"
            String actualCorrectText = dbAnswerRaw; // Default fallback

            int correctIndex = q.getCorrectAnswerIndex(); // 0 for A, 1 for B...
            if (correctIndex != -1 && q.getOptions() != null && correctIndex < q.getOptions().size()) {
                // If DB says "A", grab options.get(0) -> "Java is a language"
                actualCorrectText = q.getOptions().get(correctIndex);
            }

            // C. Compare (Case Insensitive)
            // We compare: User's Text VS Actual Correct Text
            if (!q.getUserAnswer().isEmpty() && actualCorrectText != null) {
                if (q.getUserAnswer().equalsIgnoreCase(actualCorrectText.trim())) {
                    correctAnswers++;
                }
            }
        }

        // 2. Calculate Percentage
        int percentage = 0;
        if (questions.size() > 0) {
            percentage = (int) Math.round(((double) correctAnswers / questions.size()) * 100);
        }

        // 🟢 FIX 2: Safety Cap (Never exceed 100%)
        if (percentage > 100)
            percentage = 100;
        if (correctAnswers > questions.size())
            correctAnswers = questions.size();

        quiz.setScore(percentage);

        // 3. XP Calculation
        int xpPerQuestion = 1;
        if ("Medium".equalsIgnoreCase(quiz.getDifficulty()))
            xpPerQuestion = 2;
        if ("Hard".equalsIgnoreCase(quiz.getDifficulty()))
            xpPerQuestion = 3;
        if ("Expert".equalsIgnoreCase(quiz.getDifficulty()))
            xpPerQuestion = 5;

        int xpEarned = correctAnswers * xpPerQuestion;

        if (xpEarned > 0) {
            User user = quiz.getUser();
            user.setTotalXp(user.getTotalXp() + xpEarned);
            userRepository.save(user);
        }

        quizRepository.save(quiz);

        return percentage;
    }
}