package com.quizgenix.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quizgenix.model.Quiz;
import com.quizgenix.repository.QuizRepository;

@RestController
@RequestMapping("/admin/api")
public class AdminRestController {

    @Autowired
    private QuizRepository quizRepository;

    @GetMapping("/quiz/{id}")
    public ResponseEntity<?> getQuizDetails(@PathVariable Long id) {
        Quiz quiz = quizRepository.findById(id).orElse(null);

        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }

        // Create a simple Map to avoid recursion/infinite loops with JSON
        Map<String, Object> data = new HashMap<>();
        data.put("topic", quiz.getTopic());
        data.put("difficulty", quiz.getDifficulty());
        data.put("score", quiz.getScore()); // Assuming you have a score field
        data.put("totalQuestions", quiz.getQuestions() != null ? quiz.getQuestions().size() : 0);

        // If you want to list questions in the modal:
        /*
         * List<String> questions = quiz.getQuestions().stream()
         * .map(q -> q.getQuestionText())
         * .collect(Collectors.toList());
         * data.put("questionsList", questions);
         */

        return ResponseEntity.ok(data);
    }
}