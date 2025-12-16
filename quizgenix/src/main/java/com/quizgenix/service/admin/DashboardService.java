package com.quizgenix.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.repository.UserRepository;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private QuizRepository quizRepository;

    // ========================================================================
    // 1. CARD STATS (Used by AdminController)
    // ========================================================================

    public long getTotalUsers() {
        return userRepository.countByRoleNot("ADMIN");
    }

    public double getTotalRevenue() {
        Double revenue = paymentRepository.sumTotalRevenue();
        return (revenue != null) ? revenue : 0.0;
    }

    public long getAiQuizzesGenerated() {
        return quizRepository.count();
    }

    public String getServerUptime() {
        return "99.9%";
    }

    // ========================================================================
    // 2. INITIAL PAGE LOAD METHODS (Used by AdminController)
    // ========================================================================

    // Returns data for the "Last Year" view by default
    public List<Integer> getUserGrowthData() {
        Map<String, Object> chart = getUserGrowthChart("lastyear");
        return (List<Integer>) chart.get("data");
    }

    // Returns labels for the "Last Year" view by default
    public List<String> getChartLabels() {
        Map<String, Object> chart = getUserGrowthChart("lastyear");
        return (List<String>) chart.get("labels");
    }

    // Returns Revenue data for "All Time" by default
    public ChartDataDTO getRevenueChartData() {
        // Reuse the logic from the dynamic chart method, default to "yearly"
        Map<String, Object> chartData = getRevenueSourceChart("yearly");

        return new ChartDataDTO(
                (List<String>) chartData.get("labels"),
                (List<Long>) chartData.get("data"));
    }

    // DTO Class required by AdminController
    public static class ChartDataDTO {
        public List<String> labels;
        public List<Long> data;

        public ChartDataDTO(List<String> labels, List<Long> data) {
            this.labels = labels;
            this.data = data;
        }
    }

    // ========================================================================
    // 3. DYNAMIC AJAX METHODS (Used by DashboardRestController)
    // ========================================================================

    public Map<String, Object> getUserGrowthChart(String range) {
        List<Object[]> results;
        boolean isYearlyView = false;

        // 1. Select Query based on Range
        if ("alltime".equalsIgnoreCase(range)) {
            // New Query: Group by Year
            results = userRepository.countUsersByYear();
            isYearlyView = true;
        } else {
            // Existing Query: Group by Month
            LocalDateTime startDate;
            if ("last6months".equalsIgnoreCase(range)) {
                startDate = LocalDateTime.now().minusMonths(6);
            } else {
                startDate = LocalDateTime.now().minusYears(1); // Default Last Year
            }
            results = userRepository.countUsersByMonthFromDate(startDate);
        }

        // 2. Process Data
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        for (Object[] row : results) {
            String label;
            Number count = (Number) row[1];

            if (isYearlyView) {
                // Label is just the Year (e.g. "2024")
                label = String.valueOf(row[0]);
            } else {
                // Label is Month (e.g. "Jan")
                String dateStr = (String) row[0]; // "2025-02"
                LocalDate date = LocalDate.parse(dateStr + "-01");
                label = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            }

            labels.add(label);
            data.add(count.intValue());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("data", data);
        return response;
    }

    public Map<String, Object> getRevenueSourceChart(String range) {
        // Fetch raw data from the database (Free, Monthly Plan, Yearly Plan...)
        List<Object[]> results = userRepository.countUsersByActivePlan();

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (Object[] row : results) {
            String dbPlanName = (String) row[0]; // Raw DB value
            long count = ((Number) row[1]).longValue();

            String displayLabel = "Unknown";

            // --- LABEL MAPPING LOGIC ---
            if (dbPlanName != null) {
                String normalized = dbPlanName.trim();

                if ("Free".equalsIgnoreCase(normalized)) {
                    displayLabel = "Basic";
                } else if ("Monthly Plan".equalsIgnoreCase(normalized)) {
                    displayLabel = "Pro";
                } else if ("6-Month Plan".equalsIgnoreCase(normalized)) {
                    displayLabel = "Plus";
                } else if ("Yearly Plan".equalsIgnoreCase(normalized)) {
                    displayLabel = "Premium";
                } else {
                    displayLabel = normalized; // Fallback if a new plan is added later
                }
            }

            labels.add(displayLabel);
            data.add(count);
        }

        // Handle empty database case
        if (labels.isEmpty()) {
            labels.add("No Users Yet");
            data.add(0L);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("data", data);
        return response;
    }

}