package com.quizgenix.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
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

        // 2. Check Plan Expiry Immediately (Your existing safety check)
        if (user != null) {
            userService.checkAndExpirePlan(user);
        }

        // 3. Get User Roles
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // 4. Redirect based on Role
        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/dashboard");
        }
    }
}