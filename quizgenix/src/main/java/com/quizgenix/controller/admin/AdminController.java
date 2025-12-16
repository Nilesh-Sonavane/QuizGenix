package com.quizgenix.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizgenix.model.User;
import com.quizgenix.service.admin.AdminUserService;
import com.quizgenix.service.admin.DashboardService;
import com.quizgenix.service.admin.DashboardService.ChartDataDTO;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", dashboardService.getTotalUsers());
        model.addAttribute("totalRevenue", dashboardService.getTotalRevenue());
        model.addAttribute("aiQuizzesGenerated", dashboardService.getAiQuizzesGenerated());
        model.addAttribute("serverUptime", dashboardService.getServerUptime());

        model.addAttribute("chartLabels", dashboardService.getChartLabels());
        model.addAttribute("chartData", dashboardService.getUserGrowthData());

        ChartDataDTO revenueChart = dashboardService.getRevenueChartData();
        model.addAttribute("revenueLabels", revenueChart.labels);
        model.addAttribute("revenueData", revenueChart.data);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        // Fetch ALL users and send to view
        List<User> allUsers = adminUserService.getAllUsers();
        model.addAttribute("users", allUsers);
        return "admin/users";
    }

    @GetMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id) {
        adminUserService.toggleUserStatus(id);
        return "redirect:/admin/users";
    }
}