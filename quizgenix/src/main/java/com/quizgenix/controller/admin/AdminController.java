package com.quizgenix.controller.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import this

import com.quizgenix.model.ContactMessage;
import com.quizgenix.model.Payment;
import com.quizgenix.model.User;
import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.service.ContactService;
import com.quizgenix.service.InvoiceService;
import com.quizgenix.service.PaymentService;
import com.quizgenix.service.ReportService;
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
    @Autowired
    private ReportService reportService;
    @Autowired
    private ContactService contactService;

    // --- DASHBOARD ---
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

    // --- USERS MANAGEMENT ---
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> allUsers = adminUserService.getAllUsers();
        model.addAttribute("users", allUsers);
        return "admin/users";
    }

    // --- TOGGLE STATUS (FIXED) ---
    @GetMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // DEBUG LOG: Look for this in your console!
        System.out.println(">>> REQUEST RECEIVED: Toggle status for User ID: " + id);

        try {
            adminUserService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
            System.out.println(">>> SUCCESS: User status updated.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // --- PAYMENTS ---
    @GetMapping("/payments")
    public String listPayments(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "admin/payments";
    }

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
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/payments/invoice/send/{id}")
    @ResponseBody
    public ResponseEntity<String> sendInvoice(@PathVariable Long id) {
        return invoiceService.sendInvoiceEmail(id) ? ResponseEntity.ok("Invoice sent!")
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send.");
    }

    // --- QUIZ LOGS ---
    @GetMapping("/quiz-logs")
    public String quizLogs(Model model) {
        model.addAttribute("logs", quizRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("pageTitle", "Quiz Logs");
        return "admin/quiz-logs";
    }

    // --- REPORTS ---
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("logs", reportService.getRecentSystemEvents());
        model.addAttribute("revenueData", reportService.getRevenueChartData());
        model.addAttribute("userData", reportService.getUserChartData());
        model.addAttribute("pageTitle", "System Reports");
        return "admin/reports";
    }

    @GetMapping("/reports/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] csvData = reportService.generateCsvReport(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=System_Report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] pdfData = reportService.generatePdfReport(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=System_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // --- CONTACT MESSAGES ---
    @GetMapping("/contacts")
    public String viewContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String keyword,
            Model model) {
        Page<ContactMessage> pageData = contactService.getMessages(page, 10, filter, keyword);
        model.addAttribute("messages", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword);
        return "admin/contacts";
    }

    @PostMapping("/contacts/reply")
    public String sendReply(@RequestParam("id") Long id, @RequestParam("replyMessage") String replyMessage,
            RedirectAttributes redirectAttributes) {
        contactService.respondToMessage(id, replyMessage);
        redirectAttributes.addFlashAttribute("successMessage", "Reply sent successfully!");
        return "redirect:/admin/contacts";
    }

    @GetMapping("/contacts/delete/{id}")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contactService.deleteMessage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Message deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting: " + e.getMessage());
        }
        return "redirect:/admin/contacts";
    }
}