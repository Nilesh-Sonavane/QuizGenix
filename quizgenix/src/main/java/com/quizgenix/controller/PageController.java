package com.quizgenix.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // 🟢 1. UPDATED HELPER: Returns 'User' so we can use it in logic
    private User addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            // This ensures the plan is checked/expired on every page load
            user = userService.checkAndExpirePlan(user);

            model.addAttribute("user", user);
            model.addAttribute("isLoggedIn", true);
            return user; // Return the user object for method use
        } else {
            model.addAttribute("isLoggedIn", false);
            return null;
        }
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "index";
    }

    // 🟢 2. UPDATED DASHBOARD (Now checks Plan Expiry too)
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // Use helper to get User + Handle Security/Plan Check
        User user = addUserToModel(model, principal);
        if (user == null)
            return "redirect:/login";

        // 2. Fetch Stats
        long quizCount = quizRepository.countByUser(user);
        Double avg = quizRepository.findAverageScoreByUser(user);
        int avgScore = (avg != null) ? (int) Math.round(avg) : 0;
        long rank = userRepository.countUsersWithMoreXp(user.getTotalXp()) + 1;

        // 3. Monthly Limit
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        long monthlyCount = quizRepository.countByUserAndCreatedAtAfter(user, firstDayOfMonth);

        // 4. Fetch Lists
        List<User> topPlayers = userRepository.findTop5ByOrderByTotalXpDesc();
        List<Quiz> recentQuizzes = quizRepository.findTop5ByUserOrderByCreatedAtDesc(user);

        // 5. Add Attributes
        model.addAttribute("quizzesCompleted", quizCount);
        model.addAttribute("averageScore", avgScore);
        model.addAttribute("globalRank", rank);
        model.addAttribute("quizzesThisMonth", monthlyCount);
        model.addAttribute("topPlayers", topPlayers);
        model.addAttribute("recentQuizzes", recentQuizzes);

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

    // 🟢 3. UPDATED LEADERBOARD
    @GetMapping("/leaderboard")
    public String leaderboard(Model model, Principal principal) {
        // Use helper to get current user & add to model
        User currentUser = addUserToModel(model, principal);
        if (currentUser == null)
            return "redirect:/login";

        List<User> leaderboard = userRepository.findTop20ByOrderByTotalXpDesc();
        long myRank = userRepository.countUsersWithMoreXp(currentUser.getTotalXp()) + 1;

        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("myRank", myRank);

        return "leaderboard";
    }

    // 🟢 4. UPDATED MY-QUIZZES
    @GetMapping("/my-quizzes")
    public String myQuizzes(Model model, Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 1. Get User
        User user = addUserToModel(model, principal);
        if (user == null)
            return "redirect:/login";

        // 2. Check Plan Status
        String currentPlan = (user.getActivePlan() != null) ? user.getActivePlan() : "Free";
        boolean isFree = "Free".equalsIgnoreCase(currentPlan);

        // 3. Determine Date Range Logic
        LocalDateTime start;
        LocalDateTime end = (endDate != null) ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        if (startDate != null) {
            // If User selected a date
            start = startDate.atStartOfDay();
            // SECURITY: If Free user tries to access data older than 7 days, clamp it
            if (isFree) {
                LocalDateTime limitDate = LocalDateTime.now().minusDays(7);
                if (start.isBefore(limitDate)) {
                    start = limitDate;
                }
            }
        } else {
            // Default View (No selection)
            if (isFree) {
                start = LocalDateTime.now().minusDays(7); // Free: Last 7 Days
            } else {
                start = LocalDateTime.of(1970, 1, 1, 0, 0); // Premium: All Time
            }
        }

        // 4. Fetch Data with Pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Quiz> quizPage = quizRepository.findByUserAndCreatedAtBetween(user, start, end, pageable);

        // 5. Add Attributes to Model
        model.addAttribute("quizzes", quizPage.getContent()); // The List
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizPage.getTotalPages());
        model.addAttribute("totalItems", quizPage.getTotalElements());

        // Pass filter values back to view so inputs don't reset
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // Dropdowns & Alerts
        model.addAttribute("topicList", quizRepository.findDistinctTopicsByUser(user));
        model.addAttribute("isHistoryLimited", isFree);

        return "my-quizzes";
    }

    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "settings";
    }

    @GetMapping("/pricing")
    public String pricing(Model model, Principal principal) {
        User user = addUserToModel(model, principal); // Optimized

        String activePlan = "Free";
        int activePlanRank = 0;
        int costMonthly = 199;
        int cost6Month = 899;
        int costYearly = 1499;

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
                long daysLeft = java.time.Duration.between(LocalDateTime.now(), user.getPlanExpiryDate()).toDays();

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

        model.addAttribute("activePlan", activePlan);
        model.addAttribute("activePlanRank", activePlanRank);
        model.addAttribute("costMonthly", costMonthly);
        model.addAttribute("cost6Month", cost6Month);
        model.addAttribute("costYearly", costYearly);

        return "pricing";
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