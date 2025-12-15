package com.quizgenix.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizgenix.model.Payment;
import com.quizgenix.model.Question;
import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.repository.QuestionRepository;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.repository.UserRepository;
import com.quizgenix.service.EmailService;
import com.quizgenix.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SettingsController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PaymentRepository paymentRepository; // <--- ADDED: To fix the SQL Error

    @Autowired
    private EmailService emailService;

    // ==========================================
    // 1. UPDATE PROFILE
    // ==========================================
    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam(value = "profileImage", required = false) MultipartFile file,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByEmail(principal.getName());

            // Update Text
            user.setFirstName(firstName);
            user.setLastName(lastName);

            // Handle Image Upload
            if (file != null && !file.isEmpty()) {
                if (!file.getContentType().startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Invalid file type. Please upload an image.");
                    return "redirect:/settings";
                }

                // Setup Path
                Path projectRoot = Paths.get(System.getProperty("user.dir"));
                Path uploadPath = projectRoot.resolve("../user_uploads").normalize();

                if (!Files.exists(uploadPath))
                    Files.createDirectories(uploadPath);

                // Cleanup Old Image
                String oldFileName = user.getProfileImage();
                if (oldFileName != null && !oldFileName.equals("profile.png")) {
                    try {
                        Files.deleteIfExists(uploadPath.resolve(oldFileName));
                    } catch (IOException ignored) {
                    }
                }

                // Save New Image (Native Resize)
                String newFileName = UUID.randomUUID().toString() + ".jpg";
                BufferedImage originalImage = ImageIO.read(file.getInputStream());

                int targetWidth = 600;
                int targetHeight = 600;
                double ratio = Math.min((double) targetWidth / originalImage.getWidth(),
                        (double) targetHeight / originalImage.getHeight());
                int width = (int) (originalImage.getWidth() * ratio);
                int height = (int) (originalImage.getHeight() * ratio);

                BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resizedImage.createGraphics();
                g.drawImage(originalImage, 0, 0, width, height, null);
                g.dispose();

                ImageIO.write(resizedImage, "jpg", uploadPath.resolve(newFileName).toFile());
                user.setProfileImage(newFileName);
            }

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating profile.");
        }

        return "redirect:/settings";
    }

    // ==========================================
    // 2. SEND OTP
    // ==========================================
    @PostMapping("/send-delete-otp")
    @ResponseBody
    public String sendDeleteOtp(Principal principal, HttpSession session) {
        try {
            User user = userService.findByEmail(principal.getName());
            String otp = String.format("%06d", new Random().nextInt(999999));
            session.setAttribute("deleteOtp", otp);

            emailService.sendDeleteOtpEmail(user, otp);

            return "sent";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // ==========================================
    // 3. VERIFY & DELETE ACCOUNT
    // ==========================================
    @PostMapping("/verify-delete-account")
    public String verifyAndDelete(
            @RequestParam("otp") String inputOtp,
            Principal principal,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();
        String sessionOtp = (String) session.getAttribute("deleteOtp");

        if (sessionOtp == null || !sessionOtp.equals(inputOtp)) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP.");
            return "redirect:/settings";
        }

        try {
            User user = userService.findByEmail(principal.getName());

            // ------------------------------------------------------------
            // STEP A: DELETE DATA (Quizzes -> Questions)
            // ------------------------------------------------------------
            List<Quiz> userQuizzes = quizRepository.findByUser(user);

            if (!userQuizzes.isEmpty()) {
                List<Question> userQuestions = questionRepository.findByQuizIn(userQuizzes);

                if (!userQuestions.isEmpty()) {
                    questionRepository.deleteAll(userQuestions);
                }
                quizRepository.deleteAll(userQuizzes);
            }

            // ------------------------------------------------------------
            // STEP B: UNLINK PAYMENTS (FIX FOR SQL CONSTRAINT ERROR)
            // ------------------------------------------------------------
            // We set the user_id to NULL instead of deleting the payment record.
            // This preserves the transaction history but allows the user to be deleted.
            List<Payment> userPayments = paymentRepository.findByUser(user);

            if (!userPayments.isEmpty()) {
                for (Payment payment : userPayments) {
                    payment.setUser(null); // Remove link to user
                    paymentRepository.save(payment); // Update database
                }
            }

            // ------------------------------------------------------------
            // STEP C: DELETE PROFILE IMAGE FILE
            // ------------------------------------------------------------
            String profileImage = user.getProfileImage();
            if (profileImage != null && !profileImage.equals("profile.png")) {
                Path uploadPath = Paths.get(System.getProperty("user.dir")).resolve("../user_uploads").normalize();
                Files.deleteIfExists(uploadPath.resolve(profileImage));
            }

            // ------------------------------------------------------------
            // STEP D: DELETE USER
            // ------------------------------------------------------------
            userRepository.delete(user);

            // Logout
            session.invalidate();
            request.logout();

            redirectAttributes.addFlashAttribute("success", "Account deleted successfully.");
            return "redirect:/login?deleted";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
            return "redirect:/settings";
        }
    }
}