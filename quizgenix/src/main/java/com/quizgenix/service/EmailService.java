package com.quizgenix.service;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.quizgenix.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // ============================================================
    // 1. REGISTRATION VERIFICATION EMAIL
    // ============================================================
    public void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = "no-reply@quizgenix.com";
        String senderName = "QuizGenix Team";
        String subject = "Verify your QuizGenix Account";

        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();

        // FIX: Capitalize the first name (e.g. "alex" -> "Alex")
        String firstName = capitalize(user.getFirstName());

        String content = getEmailTemplate(
                "Welcome to QuizGenix! 👋",
                "Hello, " + firstName + "!",
                "Thank you for joining <strong>QuizGenix</strong>. We are excited to have you on board!<br><br>" +
                        "To get started with AI-powered learning, please verify your email address by clicking the button below.",
                "Verify My Account",
                verifyURL);

        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // ============================================================
    // 2. FORGOT PASSWORD EMAIL
    // ============================================================
    public void sendResetPasswordEmail(User user, String resetURL)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = "contact@quizgenix.com";
        String senderName = "QuizGenix Support";
        String subject = "Reset Your Password";

        // FIX: Capitalize the first name here too
        String firstName = capitalize(user.getFirstName());

        String content = getEmailTemplate(
                "Password Reset Request 🔒",
                "Hello, " + firstName,
                "We received a request to reset the password for your QuizGenix account.<br><br>" +
                        "If you requested this, please click the button below to create a new password. " +
                        "This link will expire in 30 minutes.",
                "Reset Password",
                resetURL);

        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // ============================================================
    // HELPER: Capitalize First Letter
    // ============================================================
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // Takes first char, uppercases it, then appends the rest of the string in
        // lowercase
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // ============================================================
    // HELPER: CENTRALIZED EMAIL SENDING LOGIC
    // ============================================================
    private void sendEmail(String to, String from, String senderName, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(from, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true = sends as HTML

        mailSender.send(message);
    }

    // ============================================================
    // HELPER: HTML TEMPLATE BUILDER
    // ============================================================
    private String getEmailTemplate(String headerTitle, String greeting, String messageBody, String buttonText,
            String linkUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0B0E14; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background-color: #161618; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.5); border: 1px solid #333; }
                        .header { background: linear-gradient(90deg, #8b5cf6, #38bdf8); padding: 30px; text-align: center; }
                        .header h1 { margin: 0; color: white; font-size: 28px; letter-spacing: 1px; }
                        .content { padding: 40px 30px; text-align: center; color: #e2e8f0; }
                        .welcome-text { font-size: 18px; margin-bottom: 10px; color: #ffffff; font-weight: 600; }
                        .description { font-size: 15px; line-height: 1.6; color: #94a3b8; margin-bottom: 30px; }
                        .btn { display: inline-block; padding: 14px 32px; background: linear-gradient(90deg, #8b5cf6, #38bdf8); color: white; text-decoration: none; border-radius: 50px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(139, 92, 246, 0.4); transition: 0.3s; }
                        .btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(139, 92, 246, 0.6); }
                        .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; background-color: #0f0f11; border-top: 1px solid #222; }
                        .footer a { color: #8b5cf6; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>"""
                + headerTitle + """
                            </h1>
                        </div>
                        <div class="content">
                            <div class="welcome-text">""" + greeting + """
                        </div>
                        <p class="description">
                            """ + messageBody + """
                        </p>

                        <a href=\"""" + linkUrl + """
                        " class="btn" target="_blank">""" + buttonText + """
                                    </a>

                                    <p style="margin-top: 30px; font-size: 13px; color: #555;">
                                        If you didn't request this, you can safely ignore this email.
                                    </p>
                                </div>
                                <div class="footer">
                                    &copy; 2025 QuizGenix. All rights reserved.<br>
                                    <a href="#">Privacy Policy</a> | <a href="#">Support</a>
                                </div>
                            </div>
                        </body>
                        </html>
                        """;
    }
}