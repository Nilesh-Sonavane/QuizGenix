package com.quizgenix.controller;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Needed for error messages

import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.service.QuizPdfService;
import com.quizgenix.service.QuizService;
import com.quizgenix.service.UserService;

@Controller
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizPdfService pdfService;

    // --- 1. GENERATE QUIZ ---
    @PostMapping("/generate-quiz")
    public String generateQuiz(@RequestParam String topic,
            @RequestParam String difficulty,
            @RequestParam int count,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByEmail(principal.getName());

            // Check Plan Limits
            String plan = user.getActivePlan();
            if (plan == null)
                plan = "free";

            if (plan.equalsIgnoreCase("free")) {
                LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
                long monthlyCount = quizRepository.countByUserAndCreatedAtAfter(user, firstDayOfMonth);

                if (monthlyCount >= 10) {
                    redirectAttributes.addFlashAttribute("error",
                            "🔒 Limit Reached: You have used your 10 free quizzes for this month.");
                    return "redirect:/dashboard";
                }
            }

            Quiz quiz = quizService.createQuiz(topic, difficulty, count, user);
            return "redirect:/quiz/" + quiz.getId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "⚠️ AI Service is busy. Please try again.");
            return "redirect:/dashboard";
        }
    }

    // --- 2. SHOW QUIZ ---
    @GetMapping("/quiz/{id}")
    public String showQuizPage(@PathVariable Long id, Model model, Principal principal) {
        try {
            Quiz quiz = quizService.getQuizById(id);
            if (principal != null) {
                User user = userService.findByEmail(principal.getName());
                model.addAttribute("user", user);

                // Optional: Security check for viewing active quiz
                if (!quiz.getUser().getId().equals(user.getId())) {
                    return "redirect:/dashboard";
                }
            }
            model.addAttribute("quiz", quiz);
            return "quiz";
        } catch (Exception e) {
            return "redirect:/dashboard";
        }
    }

    // --- 3. SUBMIT QUIZ ---
    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam Long quizId,
            @RequestParam Map<String, String> allParams,
            Model model) {
        int score = quizService.calculateScore(quizId, allParams);
        Quiz quiz = quizService.getQuizById(quizId);

        model.addAttribute("score", score);
        model.addAttribute("total", quiz.getTotalQuestions());
        model.addAttribute("quiz", quiz);

        return "result";
    }

    // =========================================================
    // 4. SECURE PDF DOWNLOAD (With User-Friendly Redirect)
    // =========================================================
    // Note: Return type is 'Object' so we can return either a File OR a String
    // (Redirect)
    @GetMapping("/quiz/download-pdf/{id}")
    public Object downloadQuizPdf(@PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Identify the requester
            User requester = userService.findByEmail(principal.getName());

            // 2. Find the resource
            Quiz quiz = quizRepository.findById(id).orElse(null);

            if (quiz == null) {
                redirectAttributes.addFlashAttribute("error", "⚠️ Quiz not found.");
                return "redirect:/dashboard";
            }

            // 3. 🔒 SECURITY CHECK (Ownership)
            // If user tries to download someone else's PDF -> Redirect with Error
            if (!quiz.getUser().getId().equals(requester.getId())) {
                redirectAttributes.addFlashAttribute("error", "⛔ Unauthorized access: You cannot view this report.");
                return "redirect:/dashboard";
            }

            // 4. Safe to Generate PDF
            ByteArrayInputStream bis = pdfService.generateQuizPdf(quiz);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=quiz-result-" + id + ".pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "⚠️ Error generating PDF. Please try again.");
            return "redirect:/dashboard";
        }
    }

    // --- 5. REVIEW QUIZ ---
    @GetMapping("/quiz/review/{id}")
    public String reviewQuiz(@PathVariable Long id,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user);

            Quiz quiz = quizService.getQuizById(id);

            // 🔒 SECURITY CHECK
            if (!quiz.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "⛔ Unauthorized access.");
                return "redirect:/dashboard";
            }

            model.addAttribute("quiz", quiz);
            return "review";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/dashboard";
        }
    }
}