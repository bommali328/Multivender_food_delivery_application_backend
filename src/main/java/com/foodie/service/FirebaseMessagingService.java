package com.foodie.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;
import com.foodie.model.User;
import com.foodie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class FirebaseMessagingService {

    @Autowired
    private UserRepository userRepository;

    public void sendNotificationToUser(String mobile, String title, String body) {
        try {
            User user = userRepository.findByMobile(mobile);
            if (user != null && user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                
                Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .build();

                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("Successfully sent message: " + response);
            }
        } catch (Exception e) {
            System.err.println("Error sending FCM notification: " + e.getMessage());
        }
    }
}