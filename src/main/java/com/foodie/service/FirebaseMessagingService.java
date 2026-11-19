package com.foodie.service;

import com.foodie.model.User;
import com.foodie.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FirebaseMessagingService {

    @Autowired
    private UserRepository userRepository;

    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    public void sendNotificationToUser(String mobile, String title, String body) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                System.err.println("❌ FirebaseApp is not initialized! Check serviceAccountKey.json.");
                return;
            }

            String cleanMobile = normalizeMobile(mobile);
            if (cleanMobile.isEmpty()) {
                System.err.println("❌ Invalid customer mobile for FCM: " + mobile);
                return;
            }

            User user = userRepository.findByMobile(cleanMobile);
            if (user == null) {
                user = userRepository.findFirstByMobile(cleanMobile);
            }

            if (user == null || user.getFcmToken() == null || user.getFcmToken().trim().isEmpty()) {
                System.err.println("❌ No FCM token found for mobile: " + cleanMobile);
                return;
            }

            Message message = Message.builder()
                    .setToken(user.getFcmToken().trim())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setColor("#FC8019")
                                    .setChannelId("foodie_orders")
                                    .build())
                            .build())
                    .putData("title", title != null ? title : "")
                    .putData("body", body != null ? body : "")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Successfully sent FCM message to " + cleanMobile + ": " + response);
        } catch (Exception e) {
            System.err.println("❌ Error sending FCM notification: " + e.getMessage());
        }
    }
}