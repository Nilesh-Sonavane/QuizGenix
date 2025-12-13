package com.quizgenix.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.QuizRepository; // 1. Added Import
import com.quizgenix.service.QuizService;
import com.quizgenix.service.UserService;

@Controller
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserService userService;

    // 2. Inject Repository to check counts
    @Autowired
    private QuizRepository quizRepository;

    // 1. Generate Quiz (POST form submission)
    @PostMapping("/generate-quiz")
    public String generateQuiz(@RequestParam String topic,
            @RequestParam String difficulty,
            @RequestParam int count,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByEmail(principal.getName());

            // --- 3. SECURITY CHECK: ENFORCE MONTHLY LIMIT ---
            String plan = user.getActivePlan();
            // Handle null plan safely, default to "free"
            if (plan == null)
                plan = "free";

            if (plan.equalsIgnoreCase("free")) {
                // Calculate Start of Month
                LocalDateTime firstDayOfMonth = LocalDateTime.now()
                        .withDayOfMonth(1)
                        .with(LocalTime.MIN);

                // Check Count
                long monthlyCount = quizRepository.countByUserAndCreatedAtAfter(user, firstDayOfMonth);

                if (monthlyCount >= 10) {
                    // STOP GENERATION
                    redirectAttributes.addFlashAttribute("error",
                            "🔒 Limit Reached: You have used your 10 free quizzes for this month. Upgrade to create more!");
                    return "redirect:/dashboard";
                }
            }
            // --------------------------------------------------

            Quiz quiz = quizService.createQuiz(topic, difficulty, count, user);

            // Check if questions were actually generated
            if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
                throw new RuntimeException("AI returned no questions.");
            }

            return "redirect:/quiz/" + quiz.getId();

        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Send a user-friendly error message back to the dashboard
            redirectAttributes.addFlashAttribute("error",
                    "⚠️ AI Service is busy or Rate Limited. Please wait 30 seconds and try again.");
            return "redirect:/dashboard";
        }
    }

    // 2. Take Quiz Page (Displays the generated questions)
    @GetMapping("/quiz/{id}")
    public String showQuizPage(@PathVariable Long id, Model model, Principal principal) {
        Quiz quiz = quizService.getQuizById(id);

        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }

        model.addAttribute("quiz", quiz);
        return "quiz";
    }

    // 3. Handle Quiz Submission
    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam Long quizId,
            @RequestParam Map<String, String> allParams,
            Model model) {

        // Calculate the score using the service
        int score = quizService.calculateScore(quizId, allParams);

        // Get quiz details to display on result page
        Quiz quiz = quizService.getQuizById(quizId);

        // Send data to the result page
        model.addAttribute("score", score);
        model.addAttribute("total", quiz.getTotalQuestions());
        model.addAttribute("quiz", quiz);

        return "result";
    }
}