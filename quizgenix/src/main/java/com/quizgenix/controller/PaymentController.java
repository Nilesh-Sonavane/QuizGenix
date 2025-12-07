package com.quizgenix.controller;

import java.security.Principal;
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
import com.razorpay.RazorpayException;

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

    @PostMapping("/create-order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data) throws RazorpayException {
        int amount = Integer.parseInt(data.get("amount").toString());
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = client.orders.create(orderRequest);
        return order.toString();
    }

    @PostMapping("/update-payment")
    @ResponseBody
    public ResponseEntity<?> updatePayment(@RequestBody Map<String, String> data, Principal principal) {
        // 1. Extract Data
        String paymentId = data.get("payment_id");
        String orderId = data.get("order_id");
        String status = data.get("status");
        String amount = data.get("amount");
        String planName = data.get("plan_name");

        if (principal != null) {
            String userEmail = principal.getName();
            String userName = "Subscriber";

            try {
                // 2. Fetch the full User Entity
                User user = userService.findByEmail(userEmail);

                if (user != null) {
                    userName = user.getFirstName();

                    // 3. Create & Populate Payment Entity
                    Payment payment = new Payment();
                    payment.setOrderId(orderId);
                    payment.setPaymentId(paymentId);
                    payment.setStatus(status);
                    payment.setAmount(amount);
                    payment.setReceiptEmail(userEmail); // Matches your model field
                    payment.setUser(user); // Sets the relationship

                    // 4. Save to Database
                    paymentRepository.save(payment);
                    System.out.println("Payment saved successfully: " + paymentId);
                }

                // 5. Send Dynamic Email
                emailService.sendPaymentSuccessEmail(userEmail, userName, paymentId, amount, planName);

            } catch (Exception e) {
                System.err.println("Error processing payment: " + e.getMessage());
                return ResponseEntity.internalServerError().body(Map.of("error", "Server Error"));
            }
        }

        return ResponseEntity.ok(Map.of("msg", "updated"));
    }
}