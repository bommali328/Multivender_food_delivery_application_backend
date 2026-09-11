package com.foodie.controller;

import com.foodie.model.Payment;
import com.foodie.repository.PaymentRepository;
import com.foodie.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final String RAZORPAY_KEY_ID = "rzp_test_TTbofIebO8RB99"; // మీ లైవ్/టెస్ట్ కీ
    private static final String RAZORPAY_KEY_SECRET = "YOUR_RAZORPAY_SECRET";

    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody Payment payment) {
        Payment savedPayment = paymentService.savePayment(payment);
        return ResponseEntity.ok(savedPayment);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Payment>> getPaymentsByShop(@PathVariable Long shopId) {
        List<Payment> payments = paymentService.getPaymentsByShop(shopId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/customer/{mobile}")
    public ResponseEntity<List<Payment>> getPaymentsByCustomer(@PathVariable String mobile) {
        List<Payment> payments = paymentService.getPaymentsByCustomer(mobile);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody Map<String, Object> data) {
        try {
            int amount = Integer.parseInt(data.get("amount").toString()) * 100; // పైసలలోకి మార్చడం (₹1 = 100 పైసలు)

            RazorpayClient client = new RazorpayClient(RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET);
            JSONObject options = new JSONObject();
            options.put("amount", amount);
            options.put("currency", "INR");
            options.put("receipt", "txn_123456");

            Order order = client.orders.create(options);
            return ResponseEntity.ok(order.toString());
        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 🛡️ ADMIN PANEL ENDPOINT (NEW ADDITION)
    // ==========================================

    // ప్లాట్‌ఫార్మ్ మొత్తానికి సంబంధించిన అన్ని పేమెంట్స్ / ట్రాన్సాక్షన్లను అడ్మిన్ ప్యానెల్ కోసం ఫెచ్ చేయడానికి
    @GetMapping("/admin/all")
    public ResponseEntity<List<Payment>> getAllPaymentsForAdmin() {
        List<Payment> allPayments = paymentRepository.findAll();
        return ResponseEntity.ok(allPayments);
    }
}