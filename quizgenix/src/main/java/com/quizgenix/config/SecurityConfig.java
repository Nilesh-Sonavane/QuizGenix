package com.quizgenix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter; // ✅ Import this
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                                // 1. CSRF CONFIGURATION
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .ignoringRequestMatchers("/create-order", "/update-payment",
                                                                "/webhook/**"))

                                // 🟢 CRITICAL FIX: Register the CsrfCookieFilter to force the cookie generation
                                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

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
                                                .requestMatchers("/", "/login", "/register", "/verify",
                                                                "/forgot-password/**",
                                                                "/new-password", "/pricing", "/about", "/contact")
                                                .permitAll()
                                                .anyRequest().authenticated())

                                .formLogin((form) -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(successHandler)
                                                .failureUrl("/login?error=true")
                                                .permitAll())

                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .permitAll())

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .invalidSessionUrl("/login?expired"))

                                .userDetailsService(userDetailsService);

                return http.build();
        }
}