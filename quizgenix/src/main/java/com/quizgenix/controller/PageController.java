package com.quizgenix.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired; // 1. Import Autowired
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizgenix.model.User; // 2. Import User Model
import com.quizgenix.service.UserService; // 3. Import User Service

@Controller
public class PageController {

    @Autowired
    private UserService userService; // 4. Inject UserService

    @GetMapping("/")
    public String home() {
        return "index";
    }

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
    @SuppressWarnings("null")
    @GetMapping("/pricing")
    public String pricing(Model model, Principal principal) {
        boolean isLoggedIn = (principal != null);
        String activePlan = "Free";
        int activePlanRank = 0;

        // Default Prices
        int costMonthly = 199;
        int cost6Month = 899;
        int costYearly = 1499;

        if (isLoggedIn) {
            User user = userService.findByEmail(principal.getName());

            if (user != null) {
                // --- 1. CRITICAL CHECK: Expire plan if date has passed ---
                // This updates the user object to "Free" immediately if expired
                user = userService.checkAndExpirePlan(user);

                if (user.getActivePlan() != null) {
                    activePlan = user.getActivePlan();

                    // 2. Determine Rank
                    if (activePlan.contains("Year"))
                        activePlanRank = 3;
                    else if (activePlan.contains("6-Month"))
                        activePlanRank = 2;
                    else if (activePlan.contains("Month"))
                        activePlanRank = 1;

                    // 3. Calculate Credit
                    if (user.isPaidSubscriptionActive()) {
                        double currentPrice = (user.getCurrentPlanPrice() != null) ? user.getCurrentPlanPrice() : 0.0;

                        // Use Java Time Duration for exact day difference
                        long daysLeft = java.time.Duration
                                .between(java.time.LocalDateTime.now(), user.getPlanExpiryDate()).toDays();

                        if (daysLeft > 0) {
                            double dailyRate = 0.0;
                            if (activePlan.contains("Month") && !activePlan.contains("6"))
                                dailyRate = currentPrice / 30.0;
                            else if (activePlan.contains("Year"))
                                dailyRate = currentPrice / 365.0;
                            else if (activePlan.contains("6-Month"))
                                dailyRate = currentPrice / 180.0;

                            int credit = (int) (dailyRate * daysLeft);

                            // 4. Apply Credit to Upgrade Prices (ensure not negative)
                            costMonthly = Math.max(0, 199 - credit);
                            cost6Month = Math.max(0, 899 - credit);
                            costYearly = Math.max(0, 1499 - credit);
                        }
                    }
                }
            }
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("activePlan", activePlan);
        model.addAttribute("activePlanRank", activePlanRank);

        // Pass Calculated Prices to View
        model.addAttribute("costMonthly", costMonthly);
        model.addAttribute("cost6Month", cost6Month);
        model.addAttribute("costYearly", costYearly);

        return "pricing";
    }
    // public String pricing(Model model, Principal principal) {
    // boolean isLoggedIn = (principal != null);
    // String activePlan = "Free";
    // int activePlanRank = 0;

    // // Default Prices
    // int costMonthly = 199;
    // int cost6Month = 899;
    // int costYearly = 1499;

    // if (isLoggedIn) {
    // User user = userService.findByEmail(principal.getName());

    // if (user != null && user.getActivePlan() != null) {
    // activePlan = user.getActivePlan();

    // // 1. Determine Rank
    // if (activePlan.contains("Year"))
    // activePlanRank = 3;
    // else if (activePlan.contains("6-Month"))
    // activePlanRank = 2;
    // else if (activePlan.contains("Month"))
    // activePlanRank = 1;

    // // 2. Calculate Credit (Exact logic from PaymentController)
    // if (user.isPaidSubscriptionActive()) {
    // double currentPrice = (user.getCurrentPlanPrice() != null) ?
    // user.getCurrentPlanPrice() : 0.0;
    // long daysLeft = java.time.Duration.between(java.time.LocalDateTime.now(),
    // user.getPlanExpiryDate())
    // .toDays();

    // if (daysLeft > 0) {
    // double dailyRate = 0.0;
    // if (activePlan.contains("Month") && !activePlan.contains("6"))
    // dailyRate = currentPrice / 30.0;
    // else if (activePlan.contains("Year"))
    // dailyRate = currentPrice / 365.0;
    // else if (activePlan.contains("6-Month"))
    // dailyRate = currentPrice / 180.0;

    // int credit = (int) (dailyRate * daysLeft);

    // // 3. Apply Credit to Upgrade Prices (ensure not negative)
    // costMonthly = Math.max(0, 199 - credit);
    // cost6Month = Math.max(0, 899 - credit);
    // costYearly = Math.max(0, 1499 - credit);
    // }
    // }
    // }
    // }

    // model.addAttribute("isLoggedIn", isLoggedIn);
    // model.addAttribute("activePlan", activePlan);
    // model.addAttribute("activePlanRank", activePlanRank);

    // // Pass Calculated Prices to View
    // model.addAttribute("costMonthly", costMonthly);
    // model.addAttribute("cost6Month", cost6Month);
    // model.addAttribute("costYearly", costYearly);

    // return "pricing";
    // }

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