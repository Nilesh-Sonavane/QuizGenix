package com.quizgenix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.quizgenix.model.User;
import com.quizgenix.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // ==========================================
    // 1. LOGIN PAGE
    // ==========================================
    @GetMapping("/login")
    public String login() {
        return "login"; // Renders login.html
    }

    // ==========================================
    // 2. REGISTRATION (GET)
    // ==========================================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // We must pass an empty User object for the Thymeleaf form to bind to
        model.addAttribute("user", new User());
        return "register"; // Renders register.html
    }

    // ==========================================
    // 3. REGISTRATION (POST)
    // ==========================================
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("user") User user,
            BindingResult result,
            HttpServletRequest request,
            Model model) {

        // 1. Check for Validation Errors (e.g., blank fields, bad email format)
        if (result.hasErrors()) {
            return "register"; // Return to form with error messages
        }

        // 2. Try to Register User
        try {
            String siteURL = getSiteURL(request);
            userService.register(user, siteURL);

            // Redirect to login with a prompt to check email
            return "redirect:/login?verify_email=true";

        } catch (Exception e) {
            // Handle "Email already exists" or other service errors
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // ==========================================
    // 4. EMAIL VERIFICATION
    // ==========================================
    @GetMapping("/verify")
    public String verifyUser(@Param("code") String code) {
        if (userService.verify(code)) {
            return "redirect:/login?verified=true";
        } else {
            return "redirect:/login?verify_error=true";
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }
}