package com.quizgenix.controller; // Make sure this package line is correct!

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index"; // This loads index.html
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

    @GetMapping("/pricing")
    public String pricing() {
        return "pricing";
    }

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