package com.quizgenix.controller;

import java.io.ByteArrayInputStream; // Import for PDF Stream
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource; // Import for File Download
import org.springframework.http.HttpHeaders; // Import for Headers
import org.springframework.http.MediaType; // Import for Content Type
import org.springframework.http.ResponseEntity; // Import for Response
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // 2. Inject PDF Service
    @Autowired
    private QuizPdfService pdfService;

    // --- 1. Generate Quiz (POST) ---
    @PostMapping("/generate-quiz")
    public String generateQuiz(@RequestParam String topic,
            @RequestParam String difficulty,
            @RequestParam int count,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByEmail(principal.getName());

            // SECURITY: Monthly Limit Check
            String plan = user.getActivePlan();
            if (plan == null)
                plan = "free";

            if (plan.equalsIgnoreCase("free")) {
                LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
                long monthlyCount = quizRepository.countByUserAndCreatedAtAfter(user, firstDayOfMonth);

                if (monthlyCount >= 10) {
                    redirectAttributes.addFlashAttribute("error",
                            "🔒 Limit Reached: You have used your 10 free quizzes for this month. Upgrade to create more!");
                    return "redirect:/dashboard";
                }
            }

            Quiz quiz = quizService.createQuiz(topic, difficulty, count, user);

            if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
                throw new RuntimeException("AI returned no questions.");
            }

            return "redirect:/quiz/" + quiz.getId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "⚠️ AI Service is busy. Please wait 30 seconds and try again.");
            return "redirect:/dashboard";
        }
    }

    // --- 2. Take Quiz Page ---
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

    // --- 3. Handle Quiz Submission ---
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

    // --- 4. NEW: DOWNLOAD PDF ENDPOINT ---
    @GetMapping("/quiz/download-pdf/{id}")
    public ResponseEntity<InputStreamResource> downloadQuizPdf(@PathVariable Long id, Principal principal) {

        // A. Get User
        User user = userService.findByEmail(principal.getName());

        // B. Get Quiz
        Quiz quiz = quizRepository.findById(id).orElse(null);

        // C. Security Checks
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }

        // Check 1: Ownership (Did this user take this quiz?)
        if (!quiz.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        // Check 2: Premium Plan Validation
        // Even if they have the direct link, we reject it here if they are Free
        String plan = user.getActivePlan();
        boolean isPremium = "6-Month Plan".equals(plan) || "Yearly Plan".equals(plan);

        if (!isPremium) {
            return ResponseEntity.status(403).build(); // Forbidden for Free users
        }

        // D. Generate PDF
        ByteArrayInputStream bis = pdfService.generateQuizPdf(quiz);

        // E. Return File Download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=quiz-result-" + id + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    // --- 5. REVIEW QUIZ (Secured) ---
    @GetMapping("/quiz/review/{id}")
    public String reviewQuiz(@PathVariable Long id,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Fetch User & Add to Model (Fixes Sidebar Login Button)
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user); // This line fixes the sidebar

            // 2. Fetch the Quiz
            Quiz quiz = quizService.getQuizById(id);

            // 3. 🔒 SECURITY CHECK: Ownership
            if (!quiz.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "⛔ Unauthorized: You do not have permission to view this quiz.");
                return "redirect:/dashboard";
            }

            // 4. Proceed
            model.addAttribute("quiz", quiz);
            return "review";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/dashboard";
        }
    }
}