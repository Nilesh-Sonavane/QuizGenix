package com.quizgenix.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

    // ⚡ IMPORTANT: This must be your verified Brevo Login Email
    private final String SENDER_EMAIL = "userlocal634@gmail.com";

    // --- 1. VERIFICATION EMAIL ---
    public void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Team";
        String subject = "Verify your QuizGenix Account";
        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();
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

    // --- 2. RESET PASSWORD EMAIL ---
    public void sendResetPasswordEmail(User user, String resetURL)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Support";
        String subject = "Reset Your Password";
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

    // --- 3. PAYMENT SUCCESS EMAIL ---
    public void sendPaymentSuccessEmail(String toAddress, String name, String paymentId, String amount, String planName)
            throws MessagingException, UnsupportedEncodingException {
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Billing";
        String subject = "Payment Receipt - " + planName;
        String displayName = capitalize(name);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String messageBody = "Your subscription to the <strong>" + planName
                + "</strong> is successfully confirmed.<br><br>" +
                "Here are your transaction details:<br><br>" +
                "<div style='background-color:#1e293b; padding:20px; border-radius:12px; border:1px solid #334155; text-align:left;'>"
                +
                "  <p style='margin:8px 0; color:#94a3b8; font-size:14px;'>Date: <span style='color:#fff; float:right;'>"
                + date + "</span></p>" +
                "  <p style='margin:8px 0; color:#94a3b8; font-size:14px;'>Plan: <span style='color:#fff; float:right;'>"
                + planName + "</span></p>" +
                "  <p style='margin:8px 0; color:#94a3b8; font-size:14px;'>Transaction ID: <span style='color:#fbbf24; float:right; font-family:monospace;'>"
                + paymentId + "</span></p>" +
                "  <hr style='border:0; border-top:1px solid #334155; margin:15px 0;'>" +
                "  <p style='margin:8px 0; color:#fff; font-size:18px; font-weight:bold;'>Total Paid: <span style='color:#38bdf8; float:right;'>₹"
                + amount + "</span></p>" +
                "</div><br>" +
                "Thank you for investing in your learning journey!";
        String content = getEmailTemplate(
                "Payment Successful! 🎉",
                "Hello, " + displayName,
                messageBody,
                "Go to Dashboard",
                "http://localhost:8080/dashboard");
        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // --- 4. DELETE ACCOUNT OTP EMAIL ---
    public void sendDeleteOtpEmail(User user, String otp)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Security";
        String subject = "Delete Account Verification Code";
        String firstName = capitalize(user.getFirstName());
        String messageBody = "<span style='color: #ef4444; font-weight: bold;'>⚠️ Warning:</span> You have requested to permanently delete your account.<br><br>"
                +
                "If you proceed, all your quizzes, history, and profile data will be lost immediately.<br><br>" +
                "Use the code below to confirm this action:<br><br>" +
                "<div style='background: #1e293b; padding: 15px; border-radius: 8px; font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #fff; display: inline-block; border: 1px dashed #ef4444;'>"
                +
                otp +
                "</div>";
        String content = getEmailTemplate(
                "Account Deletion ⚠️",
                "Hello, " + firstName,
                messageBody,
                "Cancel Request",
                "http://localhost:8080/settings");
        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // --- 5. ADMIN BAN/UNBAN EMAIL ---
    public void sendAccountStatusEmail(User user, boolean isEnabled)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Support";
        String firstName = capitalize(user.getFirstName());

        String subject;
        String headerTitle;
        String messageBody;
        String buttonText;
        String linkUrl;

        if (isEnabled) {
            subject = "Account Reactivated - QuizGenix";
            headerTitle = "Welcome Back! 🔓";
            messageBody = "Good news! Your account has been reviewed and <strong>reactivated</strong> by our administration.<br><br>"
                    +
                    "You now have full access to your quizzes, history, and profile.";
            buttonText = "Login Now";
            linkUrl = "http://localhost:8080/login";
        } else {
            subject = "Account Suspended - QuizGenix";
            headerTitle = "Account Suspended ⚠️";
            messageBody = "<span style='color: #ef4444; font-weight: bold;'>Notice:</span> Your account has been suspended by the administrator.<br><br>"
                    +
                    "This action was taken due to a violation of our policies or security concerns.<br><br>" +
                    "If you believe this is a mistake, please contact our support team immediately.";
            buttonText = "Contact Support";
            linkUrl = "http://localhost:8080/contact";
        }

        String content = getEmailTemplate(headerTitle, "Hello, " + firstName, messageBody, buttonText, linkUrl);
        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // --- 6. SEND INVOICE WITH PDF ---
    public void sendInvoiceEmail(User user, String archivedEmail, String paymentId, byte[] pdfData)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = (user != null) ? user.getEmail() : archivedEmail;
        if (toAddress == null || toAddress.isEmpty())
            return;

        String firstName = (user != null) ? capitalize(user.getFirstName()) : "Valued Customer";
        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Billing";
        String subject = "Invoice #" + paymentId;

        String messageBody = "Please find attached the official invoice for your recent transaction.<br><br>" +
                "Transaction ID: <span style='font-family:monospace; color:#8b5cf6;'>" + paymentId + "</span><br><br>" +
                "Thank you for choosing QuizGenix!";

        String content = getEmailTemplate(
                "Your Invoice 📄",
                "Hello, " + firstName,
                messageBody,
                "View Dashboard",
                "http://localhost:8080/admin/dashboard");

        sendEmailWithAttachment(toAddress, fromAddress, senderName, subject, content, "Invoice_" + paymentId + ".pdf",
                pdfData);
    }

    // --- 7. ADMIN CONTACT REPLY EMAIL ---
    public void sendAdminReplyEmail(String toAddress, String replyContent, String originalMessage)
            throws MessagingException, UnsupportedEncodingException {

        String fromAddress = SENDER_EMAIL; // ✅ Fixed
        String senderName = "QuizGenix Support";
        String subject = "Response to your inquiry - QuizGenix";

        // 1. Format Original Message (Gray Block)
        String formattedOriginal = "<div style='background-color:rgba(255,255,255,0.05); border-left: 4px solid #64748b; padding: 15px; color: #94a3b8; font-style: italic; margin-bottom: 20px; font-size: 14px;'>"
                + "<strong style='color:#cbd5e1;'>You wrote:</strong><br>"
                + originalMessage.replace("\n", "<br>")
                + "</div>";

        // 2. Format Admin Reply (Purple Block)
        String formattedReply = "<div style='background-color:rgba(139, 92, 246, 0.1); border-left: 4px solid #8b5cf6; padding: 15px; color: #e2e8f0; margin-bottom: 20px;'>"
                + "<strong style='color:#a78bfa;'>Our Response:</strong><br>"
                + replyContent.replace("\n", "<br>")
                + "</div>";

        String messageBody = "Hello,<br><br>"
                + "Thank you for contacting us. We have reviewed your query.<br><br>"
                + formattedOriginal // Show User Message
                + formattedReply // Show Admin Reply
                + "If you have further questions, feel free to reply to this email.";

        String content = getEmailTemplate(
                "Support Response 💬",
                "Response to your Message",
                messageBody,
                "Visit QuizGenix",
                "http://localhost:8080");

        sendEmail(toAddress, fromAddress, senderName, subject, content);
    }

    // --- HELPERS ---

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Standard Sender
    private void sendEmail(String to, String from, String senderName, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(Objects.requireNonNull(from), Objects.requireNonNull(senderName));
        helper.setTo(Objects.requireNonNull(to));
        helper.setSubject(Objects.requireNonNull(subject));
        helper.setText(Objects.requireNonNull(content), true);

        mailSender.send(message);
    }

    // Attachment Sender (Multipart)
    private void sendEmailWithAttachment(String to, String from, String senderName, String subject, String content,
            String attachmentName, byte[] attachmentData)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // True = Multipart

        helper.setFrom(Objects.requireNonNull(from), Objects.requireNonNull(senderName));
        helper.setTo(Objects.requireNonNull(to));
        helper.setSubject(Objects.requireNonNull(subject));
        helper.setText(Objects.requireNonNull(content), true);

        // Add Attachment
        helper.addAttachment(attachmentName, new ByteArrayResource(attachmentData));

        mailSender.send(message);
    }

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
