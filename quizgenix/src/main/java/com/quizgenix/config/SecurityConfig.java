package com.quizgenix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.quizgenix.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. PRICING FEATURE: Disable CSRF for Payment Endpoints
                                // This allows the Razorpay AJAX calls to POST without getting blocked
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/create-order", "/update-payment"))

                                .authorizeHttpRequests((requests) -> requests
                                                // 2. Static Resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/logo.png",
                                                                "/profile.png")
                                                .permitAll()

                                                // 3. Public Pages (Added "/pricing")
                                                .requestMatchers(
                                                                "/",
                                                                "/login",
                                                                "/register",
                                                                "/verify",
                                                                "/forgot-password/**",
                                                                "/new-password",
                                                                "/pricing")
                                                .permitAll()

                                                // 4. Protected Pages
                                                .anyRequest().authenticated())

                                .formLogin((form) -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .permitAll())

                                .logout((logout) -> logout
                                                // Your original logic preserved
                                                .logoutRequestMatcher(
                                                                request -> request.getRequestURI().endsWith("/logout"))
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())

                                .userDetailsService(userDetailsService);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}