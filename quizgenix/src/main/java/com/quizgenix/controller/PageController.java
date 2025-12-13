package com.quizgenix.controller;

import java.security.Principal;
import java.time.LocalDateTime; // Import needed for date math
import java.time.LocalTime; // Import needed for time math
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.repository.UserRepository;
import com.quizgenix.service.UserService;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper method to add user to model
    private void addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            user = userService.checkAndExpirePlan(user);
            model.addAttribute("user", user);
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }

    // --- NEW DASHBOARD METHOD START ---
    // Inside PageController.java

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // 1. Fetch Current User
        User user = userService.findByEmail(principal.getName());

        // REMOVED: if (user.getTotalXp() == null) ... because int cannot be null.

        // 2. Fetch Stats
        long quizCount = quizRepository.countByUser(user);
        Double avg = quizRepository.findAverageScoreByUser(user);
        int avgScore = (avg != null) ? (int) Math.round(avg) : 0;

        // Calculate Rank
        long rank = userRepository.countUsersWithMoreXp(user.getTotalXp()) + 1;

        // 3. Monthly Limit Logic
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        long monthlyCount = quizRepository.countByUserAndCreatedAtAfter(user, firstDayOfMonth);

        // 4. Fetch Lists (Leaderboard & Timeline)
        List<User> topPlayers = userRepository.findTop5ByOrderByTotalXpDesc();
        List<Quiz> recentQuizzes = quizRepository.findTop5ByUserOrderByCreatedAtDesc(user);

        // 5. Add Attributes to Model
        model.addAttribute("user", user);
        model.addAttribute("quizzesCompleted", quizCount);
        model.addAttribute("averageScore", avgScore);
        model.addAttribute("globalRank", rank);
        model.addAttribute("quizzesThisMonth", monthlyCount);
        model.addAttribute("topPlayers", topPlayers);
        model.addAttribute("recentQuizzes", recentQuizzes);

        return "dashboard";
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "index";
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
        addUserToModel(model, principal);

        String activePlan = "Free";
        int activePlanRank = 0;
        int costMonthly = 199;
        int cost6Month = 899;
        int costYearly = 1499;

        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            if (user != null && user.getActivePlan() != null) {
                activePlan = user.getActivePlan();

                if (activePlan.contains("Year"))
                    activePlanRank = 3;
                else if (activePlan.contains("6-Month"))
                    activePlanRank = 2;
                else if (activePlan.contains("Month"))
                    activePlanRank = 1;

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

    // --- Admin Pages ---
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