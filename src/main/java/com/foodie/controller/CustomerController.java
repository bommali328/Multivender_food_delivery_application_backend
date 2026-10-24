package com.foodie.controller;

import com.foodie.model.User;
import com.foodie.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*") // అన్ని సోర్సెస్ నుండి రిక్వెస్ట్‌లను అనుమతించడానికి
public class CustomerController {

    @Autowired
    private UserRepository userRepository;

    // కస్టమర్ FCM టోకెన్‌ను సేవ్ లేదా అప్‌డేట్ చేయడానికి API
    @PostMapping("/update-fcm-token")
    public ResponseEntity<?> updateFcmToken(@RequestBody Map<String, String> payload) {
        String mobile = payload.get("mobile");
        String fcmToken = payload.get("fcmToken");

        if (mobile == null || fcmToken == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Mobile and fcmToken are required"));
        }

        // ఒకవేళ కస్టమర్స్ అందరూ 'users' టేబుల్‌లో ఉంటే ఇక్కడ యూజర్‌ని వెతకాలి
        User user = userRepository.findByMobile(mobile); // లేదా UserRepository వాడండి
        if (user != null) {
            user.setFcmToken(fcmToken);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "FCM Token updated successfully"));
        } else {
            return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "User not found"));
        }
    }
}