package com.quizgenix.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.quizgenix.model.Payment;
import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.service.InvoiceService;
import com.quizgenix.service.PaymentService;
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
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private QuizRepository quizRepository;

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

    @GetMapping("/payments")
    public String listPayments(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        model.addAttribute("payments", payments);
        return "admin/payments";
    }

    // 1. DOWNLOAD INVOICE
    @GetMapping("/payments/invoice/{id}")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable Long id) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            byte[] data = invoiceService.generateInvoicePdf(payment);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment;filename=Invoice_" + payment.getPaymentId() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace(); // Good for debugging
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. SEND INVOICE VIA EMAIL (AJAX)
    @PostMapping("/payments/invoice/send/{id}")
    @ResponseBody
    public ResponseEntity<String> sendInvoice(@PathVariable Long id) {
        boolean sent = invoiceService.sendInvoiceEmail(id);
        if (sent) {
            return ResponseEntity.ok("Invoice sent successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send invoice. No email found or error occurred.");
        }
    }

    @GetMapping("/quiz-logs")
    public String quizLogs(Model model) {
        // Fetch all quizzes sorted by newest first
        List<Quiz> logs = quizRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        model.addAttribute("logs", logs);
        model.addAttribute("pageTitle", "Quiz Logs");

        return "admin/quiz-logs";
    }
}