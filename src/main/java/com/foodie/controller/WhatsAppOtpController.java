package com.foodie.controller;

import com.foodie.service.OtpService;
import com.foodie.model.User;
import com.foodie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class WhatsAppOtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    // 1. WhatsApp OTP పంపడానికి API
    @PostMapping("/send-whatsapp-otp")
    public ResponseEntity<?> sendWhatsAppOtp(@RequestBody Map<String, String> payload) {
        String mobile = payload.get("mobile");
        if (mobile == null || mobile.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
        }

        otpService.generateAndSaveOtp(mobile);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "OTP sent to WhatsApp successfully!"));
    }

    // 2. OTP వెరిఫై చేసి లాగిన్ అవ్వడానికి API
    @PostMapping("/verify-whatsapp-otp")
    public ResponseEntity<?> verifyWhatsAppOtp(@RequestBody Map<String, String> payload) {
        String mobile = payload.get("mobile");
        String enteredOtp = payload.get("otp");

        if (mobile == null || enteredOtp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mobile and OTP are required"));
        }

        boolean isValid = otpService.verifyOtp(mobile, enteredOtp);
        if (isValid) {
            User user = userRepository.findFirstByMobile(mobile);
            if (user == null) {
                // నంబర్ లేకపోతే ఆటోమేటిక్‌గా రిజిస్టర్ చేయవచ్చు లేదా ఎర్రర్ ఇవచ్చు
                user = new User();
                user.setMobile(mobile);
                user.setName("Foodie Customer");
                user.setRole("Customer");
                user.setActive(true);
                userRepository.save(user);
            }
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Login successful", "data", user));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired OTP!"));
        }
    }
}