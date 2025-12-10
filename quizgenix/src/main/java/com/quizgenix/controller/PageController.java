package com.quizgenix.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizgenix.model.User;
import com.quizgenix.service.UserService;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    // --- Helper Method to Load User ---
    private void addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName());

            // Auto-check expiry on every page load ensures data is always fresh
            user = userService.checkAndExpirePlan(user);

            model.addAttribute("user", user);
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "dashboard";
    }

    @GetMapping("/quiz")
    public String quiz(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "quiz";
    }

    @GetMapping("/result")
    public String result(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "result";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "leaderboard";
    }

    @GetMapping("/my-quizzes")
    public String myQuizzes(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "my-quizzes";
    }

    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "settings";
    }

    @GetMapping("/pricing")
    public String pricing(Model model, Principal principal) {
        // We call the helper to set 'user' and 'isLoggedIn'
        addUserToModel(model, principal);

        // Custom Logic for Pricing Page
        String activePlan = "Free";
        int activePlanRank = 0;
        int costMonthly = 199;
        int cost6Month = 899;
        int costYearly = 1499;

        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            // Note: Expiry check already done in addUserToModel

            if (user != null && user.getActivePlan() != null) {
                activePlan = user.getActivePlan();

                // Determine Rank
                if (activePlan.contains("Year"))
                    activePlanRank = 3;
                else if (activePlan.contains("6-Month"))
                    activePlanRank = 2;
                else if (activePlan.contains("Month"))
                    activePlanRank = 1;

                // Calculate Credit for Upgrade
                if (user.isPaidSubscriptionActive()) {
                    double currentPrice = (user.getCurrentPlanPrice() != null) ? user.getCurrentPlanPrice() : 0.0;
                    long daysLeft = java.time.Duration.between(java.time.LocalDateTime.now(), user.getPlanExpiryDate())
                            .toDays();

                    if (daysLeft > 0) {
                        double dailyRate = 0.0;
                        if (activePlan.contains("Month") && !activePlan.contains("6"))
                            dailyRate = currentPrice / 30.0;
                        else if (activePlan.contains("Year"))
                            dailyRate = currentPrice / 365.0;
                        else if (activePlan.contains("6-Month"))
                            dailyRate = currentPrice / 180.0;

                        int credit = (int) (dailyRate * daysLeft);
                        costMonthly = Math.max(0, 199 - credit);
                        cost6Month = Math.max(0, 899 - credit);
                        costYearly = Math.max(0, 1499 - credit);
                    }
                }
            }
        }

        model.addAttribute("activePlan", activePlan);
        model.addAttribute("activePlanRank", activePlanRank);
        model.addAttribute("costMonthly", costMonthly);
        model.addAttribute("cost6Month", cost6Month);
        model.addAttribute("costYearly", costYearly);

        return "pricing";
    }

    // --- Admin Pages (Assuming Admin has separate security checks) ---
    @GetMapping("/admin/")
    public String admin() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/payments")
    public String payments() {
        return "admin/payments";
    }

    @GetMapping("/admin/quiz-logs")
    public String adminLogs() {
        return "admin/quiz-logs";
    }

    @GetMapping("/admin/reports")
    public String adminReports() {
        return "admin/reports";
    }

    @GetMapping("/admin/settings")
    public String adminSettings(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return "admin/settings";
    }
}