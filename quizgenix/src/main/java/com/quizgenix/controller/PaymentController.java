package com.quizgenix.controller;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.quizgenix.model.Payment;
import com.quizgenix.model.User;
import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.service.EmailService;
import com.quizgenix.service.UserService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

@Controller
public class PaymentController {

    @Autowired
    private EmailService emailService;
    @Autowired
    private UserService userService;
    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String keyId;
    @Value("${razorpay.key.secret}")
    private String keySecret;

    // ========================================================================
    // 1. CREATE ORDER (Fix: Force Base Price based on Plan Name)
    // ========================================================================
    @PostMapping("/create-order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception {

        String planName = data.get("info").toString();

        // --- CRITICAL FIX START ---
        // Ignore the 'amount' sent from frontend (which is 0).
        // Set the Price based on the TARGET PLAN.
        int baseAmount = 0;
        if (planName.contains("Year")) {
            baseAmount = 1499;
        } else if (planName.contains("6-Month")) {
            baseAmount = 899;
        } else if (planName.contains("Month")) {
            baseAmount = 199;
        }
        // --- CRITICAL FIX END ---

        int finalAmount = baseAmount;

        if (principal != null) {
            User user = userService.findByEmail(principal.getName());

            // Check if user has an active paid plan to calculate credit
            if (user != null && user.isPaidSubscriptionActive()) {

                double currentPrice = (user.getCurrentPlanPrice() != null) ? user.getCurrentPlanPrice() : 0.0;
                long daysLeft = Duration.between(LocalDateTime.now(), user.getPlanExpiryDate()).toDays();

                if (daysLeft > 0) {
                    double dailyRate = 0.0;

                    // Determine daily value of CURRENT plan
                    if (user.getActivePlan().contains("Month") && !user.getActivePlan().contains("6")) {
                        dailyRate = currentPrice / 30.0;
                    } else if (user.getActivePlan().contains("Year")) {
                        dailyRate = currentPrice / 365.0;
                    } else if (user.getActivePlan().contains("6-Month")) {
                        dailyRate = currentPrice / 180.0;
                    }

                    int credit = (int) (dailyRate * daysLeft);

                    // Calculation: Target Price (1499) - Credit (e.g. 600) = Final (899)
                    finalAmount = baseAmount - credit;
                }
            }
        }

        // Logic for Free Upgrade (Only if credit covers the full cost)
        if (finalAmount <= 0) {
            JSONObject freeResponse = new JSONObject();
            freeResponse.put("status", "free_upgrade");
            freeResponse.put("amount", 0);
            freeResponse.put("id", "free_" + System.currentTimeMillis());
            return freeResponse.toString();
        }

        // Create Razorpay Order
        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", finalAmount * 100); // Convert to paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = client.orders.create(orderRequest);
        return order.toString();
    }

    // ========================================================================
    // 2. UPDATE PAYMENT (Standard Logic)
    // ========================================================================
    @PostMapping("/update-payment")
    @ResponseBody
    public ResponseEntity<?> updatePayment(@RequestBody Map<String, String> data, Principal principal) {
        String paymentId = data.get("payment_id");
        String orderId = data.get("order_id");
        String status = data.get("status");
        String amountPaidStr = data.get("amount");
        String planName = data.get("plan_name");

        if (principal != null) {
            try {
                User user = userService.findByEmail(principal.getName());
                if (user != null) {

                    int daysToAdd = 30;
                    double actualPlanValue = 199.0;

                    if (planName.contains("Year")) {
                        daysToAdd = 365;
                        actualPlanValue = 1499.0;
                    } else if (planName.contains("6-Month")) {
                        daysToAdd = 180;
                        actualPlanValue = 899.0;
                    }

                    user.setPlanStartDate(LocalDateTime.now());
                    user.setPlanExpiryDate(LocalDateTime.now().plusDays(daysToAdd));
                    user.setActivePlan(planName);
                    user.setCurrentPlanPrice(actualPlanValue); // Store REAL value

                    userService.save(user);

                    Payment payment = new Payment();
                    payment.setOrderId(orderId);
                    payment.setPaymentId(paymentId);
                    payment.setStatus(status);
                    payment.setAmount(amountPaidStr);
                    payment.setPlanName(planName);
                    payment.setReceiptEmail(user.getEmail());
                    payment.setUser(user);
                    paymentRepository.save(payment);

                    String finalPaid = (amountPaidStr != null) ? amountPaidStr : "0";
                    emailService.sendPaymentSuccessEmail(user.getEmail(), user.getFirstName(), paymentId, finalPaid,
                            planName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.ok(Map.of("msg", "updated"));
    }
}