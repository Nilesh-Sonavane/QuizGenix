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
                                .authorizeHttpRequests((requests) -> requests
                                                // 1. Static Resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/logo.png",
                                                                "/profile.png")
                                                .permitAll()

                                                // 2. Public Pages
                                                .requestMatchers("/", "/login", "/register", "/verify",
                                                                "/forgot-password/**", "/new-password")
                                                .permitAll()

                                                // 3. Protected Pages
                                                .anyRequest().authenticated())
                                .formLogin((form) -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .permitAll())
                                .logout((logout) -> logout
                                                // FIX: Use a Lambda Expression instead of AntPathRequestMatcher
                                                // This checks if the URL ends with "/logout" manually.
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