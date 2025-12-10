package com.quizgenix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.quizgenix.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Autowired
        private CustomLoginSuccessHandler successHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. PRICING FEATURE: Disable CSRF for Payment Endpoints
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/create-order", "/update-payment"))

                                .authorizeHttpRequests((requests) -> requests
                                                // 2. Static Resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/logo.png",
                                                                "/profile.png")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/dashboard", "/my-quizzes", "/quiz/**", "/result/**",
                                                                "/settings/**")
                                                .hasRole("USER")
                                                // 3. Public Pages
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
                                                // 5. Use Custom Handler to check Plan Expiry
                                                .successHandler(successHandler)
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

        // REMOVED: passwordEncoder() bean is now in AppConfig.java
}