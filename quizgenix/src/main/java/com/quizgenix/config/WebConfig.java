package com.quizgenix.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Logic to find the folder "one level up" from the project
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path uploadPath = projectRoot.resolve("../user_uploads").normalize();

        // 2. Convert path to a URL format (file:///C:/...)
        String uploadPathUri = uploadPath.toUri().toString();

        // 3. Register the handler
        // Any request for /user-photos/xyz.jpg -> loads from
        // C:/.../user_uploads/xyz.jpg
        registry.addResourceHandler("/user-photos/**")
                .addResourceLocations(uploadPathUri);
    }
}