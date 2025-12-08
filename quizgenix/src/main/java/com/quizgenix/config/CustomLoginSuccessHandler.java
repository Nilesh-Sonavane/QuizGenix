package com.quizgenix.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.quizgenix.model.User;
import com.quizgenix.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // 1. Get the logged-in user's email
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        // 2. Check Plan Expiry Immediately
        if (user != null) {
            // This checks the date and downgrades to "Free" if expired
            userService.checkAndExpirePlan(user);
        }

        // 3. Redirect to Dashboard (or wherever you want them to go)
        response.sendRedirect("/dashboard");
    }
}