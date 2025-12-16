package com.quizgenix.controller.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quizgenix.service.admin.DashboardService;

@RestController
@RequestMapping("/admin/api/dashboard")
public class DashboardRestController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/growth-chart")
    public Map<String, Object> getGrowthChart(@RequestParam(defaultValue = "lastyear") String range) {
        return dashboardService.getUserGrowthChart(range);
    }

    @GetMapping("/revenue-chart")
    public Map<String, Object> getRevenueChart(@RequestParam(defaultValue = "yearly") String range) {
        return dashboardService.getRevenueSourceChart(range);
    }
}