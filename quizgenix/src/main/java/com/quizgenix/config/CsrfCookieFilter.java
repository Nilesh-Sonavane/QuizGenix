package com.quizgenix.config;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Load the CSRF Token from the request attribute
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        // 2. Explicitly call getToken() to force Spring to write the Cookie immediately
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        // 3. Continue the request chain
        filterChain.doFilter(request, response);
    }
}