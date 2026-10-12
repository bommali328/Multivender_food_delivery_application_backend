package com.foodie.controller;

import com.foodie.model.Customer;
import com.foodie.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*") // అన్ని సోర్సెస్ నుండి రిక్వెస్ట్‌లను అనుమతించడానికి
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    // కస్టమర్ FCM టోకెన్‌ను సేవ్ లేదా అప్‌డేట్ చేయడానికి API
    @PostMapping("/update-fcm-token")
    public ResponseEntity<?> updateFcmToken(@RequestBody Map<String, String> payload) {
        String mobile = payload.get("mobile");
        String fcmToken = payload.get("fcmToken");

        if (mobile == null || fcmToken == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Mobile and fcmToken are required"));
        }

        Customer customer = customerRepository.findByMobile(mobile);
        if (customer != null) {
            customer.setFcmToken(fcmToken);
            customerRepository.save(customer);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "FCM Token updated successfully"));
        }
        
        return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "Customer not found with this mobile"));
    }
}