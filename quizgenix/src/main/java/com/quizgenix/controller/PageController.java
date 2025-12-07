package com.quizgenix.controller;

import java.security.Principal; // Import Principal

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Import Model
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // @GetMapping("/login")
    // public String login() {
    // return "login";
    // }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/quiz")
    public String quiz() {
        return "quiz";
    }

    @GetMapping("/result")
    public String result() {
        return "result";
    }

    @GetMapping("/leaderboard")
    public String leaderboard() {
        return "leaderboard";
    }

    @GetMapping("/my-quizzes")
    public String myQuizzes() {
        return "my-quizzes";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    /* --- UPDATED METHOD --- */
    @GetMapping("/pricing")
    public String pricing(Model model, Principal principal) {
        // Check if user is logged in (principal will be null if not logged in)
        boolean isLoggedIn = (principal != null);

        // Pass this true/false value to the HTML
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "pricing";
    }
    /* --------------------- */

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/quiz-logs")
    public String adminLogs() {
        return "admin/quiz-logs";
    }

    @GetMapping("/admin/reports")
    public String adminReports() {
        return "admin/reports";
    }
}