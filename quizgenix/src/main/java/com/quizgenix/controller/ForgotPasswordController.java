package com.quizgenix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quizgenix.model.User;
import com.quizgenix.service.EmailService;
import com.quizgenix.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // ========================================================================
    // 1. SHOW THE "FORGOT PASSWORD" PAGE (Enter Email)
    // ========================================================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password"; // Renders forgot-password.html
    }

    // ========================================================================
    // 2. PROCESS EMAIL SUBMISSION (Generate Token & Send Email)
    // ========================================================================
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model, HttpServletRequest request) {
        try {
            // 1. Generate Token via Service
            String token = userService.updateResetPasswordToken(email);

            // 2. Fetch the User object so we can get their Name
            // You might need to expose findByEmail in UserService if it's not there,
            // OR retrieve it using the token we just generated.
            User user = userService.getByResetPasswordToken(token);

            // 3. Generate Reset Link
            String siteURL = getSiteURL(request);
            String resetUrl = siteURL + "/forgot-password/reset?token=" + token;

            // 4. Send Email (Pass the 'user' object now)
            emailService.sendResetPasswordEmail(user, resetUrl);

            model.addAttribute("success", "We have sent a reset link to your email. Please check.");

        } catch (Exception e) {
            model.addAttribute("error", "" + e.getMessage());
        }

        return "forgot-password";
    }

    // ========================================================================
    // 3. SHOW "SET NEW PASSWORD" PAGE (User clicks link in Email)
    // ========================================================================
    @GetMapping("/forgot-password/reset")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {

        // 1. Find User by Token
        User user = userService.getByResetPasswordToken(token);

        // 2. Validate Token (Check existence and expiration)
        if (user == null || userService.isTokenExpired(user.getTokenCreationDate())) {
            model.addAttribute("error", "Invalid or expired reset token.");

            // CRITICAL FIX: Add empty user so Thymeleaf form (th:object="${user}") doesn't
            // crash
            model.addAttribute("user", new User());

            return "new-password"; // Shows the page with the error message
        }

        // 3. Token is Valid: Prepare Form
        User userModel = new User();
        userModel.setResetPasswordToken(token); // Pass token to the hidden input field
        model.addAttribute("user", userModel);

        return "new-password";
    }

    // ========================================================================
    // 4. PROCESS NEW PASSWORD (Update DB)
    // ========================================================================
    @PostMapping("/forgot-password/reset")
    public String processResetPassword(
            @Valid @ModelAttribute("user") User user,
            BindingResult result,
            Model model) {

        // 1. Check Password Regex Validation (Ignore Name/Email errors)
        if (result.hasFieldErrors("password")) {
            return "new-password";
        }

        // 2. Find Real User by Token (Security Check)
        User userInDb = userService.getByResetPasswordToken(user.getResetPasswordToken());

        if (userInDb == null) {
            model.addAttribute("error", "Invalid token. Please request a new link.");
            model.addAttribute("user", new User()); // Prevent crash
            return "new-password";
        }

        // 3. Check Expiration again
        if (userService.isTokenExpired(userInDb.getTokenCreationDate())) {
            model.addAttribute("error", "Token has expired. Please request a new link.");
            model.addAttribute("user", new User());
            return "new-password";
        }

        // 4. Update Password using Service (Handles encoding & clearing token)
        userService.updatePassword(userInDb, user.getPassword());

        return "redirect:/login?resetSuccess";
    }

    // ========================================================================
    // UTILITY
    // ========================================================================
    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }
}