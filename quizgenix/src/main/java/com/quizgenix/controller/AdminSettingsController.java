package com.quizgenix.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.quizgenix.model.User;
import com.quizgenix.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class AdminSettingsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "../user_uploads";

    // Exact Regex from your User.java
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*\\-+=?_]).{8,}$";

    @GetMapping("/settings")
    public String showSettings(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email);

        if (user != null) {
            user.setFirstName(toTitleCase(user.getFirstName()));
            user.setLastName(toTitleCase(user.getLastName()));
        }

        model.addAttribute("user", user);
        return "admin/settings";
    }

    // --- 1. Update Profile ---
    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute User formData,
            @RequestParam("imageFile") MultipartFile file,
            Principal principal) {

        User currentUser = userRepository.findByEmail(principal.getName());
        currentUser.setFirstName(formData.getFirstName());
        currentUser.setLastName(formData.getLastName());

        if (!file.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath))
                    Files.createDirectories(uploadPath);

                if (currentUser.getProfileImage() != null) {
                    try {
                        Files.deleteIfExists(uploadPath.resolve(currentUser.getProfileImage()));
                    } catch (Exception e) {
                    }
                }

                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), uploadPath.resolve(uniqueFileName),
                        StandardCopyOption.REPLACE_EXISTING);
                currentUser.setProfileImage(uniqueFileName);
            } catch (IOException e) {
                return "redirect:/admin/settings?error=upload_failed";
            }
        }
        userRepository.save(currentUser);
        return "redirect:/admin/settings?success=profile";
    }

    // --- 2. Update Password ---
    @PostMapping("/update-password")
    public String updatePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName());

        // 1. Verify Current Password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "redirect:/admin/settings?error=invalid_current";
        }

        // 2. Validate Password Complexity (Regex)
        // We must check this BEFORE encoding!
        if (!newPassword.matches(PASSWORD_REGEX)) {
            return "redirect:/admin/settings?error=weak_password";
        }

        // 3. Check if New Password matches Confirmation
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/admin/settings?error=mismatch";
        }

        // 4. Hash and Save
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/admin/settings?success=password";
    }

    // Helper
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty())
            return "";
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
                if (i < words.length - 1)
                    result.append(" ");
            }
        }
        return result.toString();
    }
}