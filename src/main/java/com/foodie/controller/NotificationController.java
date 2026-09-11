package com.foodie.controller;

import com.foodie.model.Notification;
import com.foodie.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // బ్రాడ్‌కాస్ట్ పంపడానికి
    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcastNotification(@RequestBody Notification notification) {
        try {
            Notification saved = notificationRepository.save(notification);

            // 👈 వెబ్‌సాకెట్ ద్వారా రియల్-టైమ్‌లో కస్టమర్లు/పార్టనర్స్‌కి బ్రాడ్‌కాస్ట్ చేయడం
            if ("All Users".equalsIgnoreCase(saved.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/notifications/all", saved);
            } else if ("Customers".equalsIgnoreCase(saved.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/notifications/customers", saved);
            } else if ("Delivery Partners".equalsIgnoreCase(saved.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/notifications/partners", saved);
            } else if ("Shop Owners".equalsIgnoreCase(saved.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/notifications/shops", saved);
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification broadcasted successfully!", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // యాక్టివ్ నోటిఫికేషన్స్ ఫెచ్ చేయడానికి
    @GetMapping("/active")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }
}