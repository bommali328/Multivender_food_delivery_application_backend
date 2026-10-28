package com.foodie.service;

import com.foodie.model.Customer;
import com.foodie.model.Notification;
import com.foodie.repository.CustomerRepository;
import com.foodie.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    public Notification sendAndBroadcastNotification(Notification notification) {
        // 1. డేటాబేస్‌లో సేవ్ చేయడం
        Notification saved = notificationRepository.save(notification);

        // 2. టార్గెట్ ఆడియన్స్ బట్టి వెబ్‌సాకెట్ ద్వారా రియల్-టైమ్‌లో పంపడం
        String target = saved.getTargetAudience();
        String msgContent = saved.getMessage();
        String imgUrl = saved.getImageUrl();

        if (target != null) {
            if (target.equalsIgnoreCase("All Users")) {
                messagingTemplate.convertAndSend("/topic/notifications/all", saved);
                sendPushNotificationToAllCustomers("Foodiee Alert", msgContent, imgUrl);
            } else if (target.equalsIgnoreCase("Customers")) {
                messagingTemplate.convertAndSend("/topic/notifications/customers", saved);
                sendPushNotificationToAllCustomers("Foodiee Special Offer", msgContent, imgUrl);
            } else if (target.equalsIgnoreCase("Delivery Partners")) {
                messagingTemplate.convertAndSend("/topic/notifications/partners", saved);
            } else if (target.equalsIgnoreCase("Shop Owners")) {
                messagingTemplate.convertAndSend("/topic/notifications/shops", saved);
            }
        }

        return saved;
    }
    
 // 🌟 నిర్దిష్టమైన కస్టమర్‌కి (ఆర్డర్ అప్‌డేట్స్ కోసం) పుష్ నోటిఫికేషన్ పంపే మెథడ్
    public void sendPushNotificationToCustomer(String fcmToken, String title, String body, String imageUrl) {
        try {
            if (fcmToken == null || fcmToken.isEmpty()) {
                System.out.println("FCM Token is missing for this customer!");
                return;
            }

            com.google.firebase.messaging.Notification.Builder notifBuilder = 
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(title != null ? title : "Foodiee Order Update")
                    .setBody(body != null ? body : "Your order status has been updated!");

            if (imageUrl != null && !imageUrl.isEmpty()) {
                notifBuilder.setImage(imageUrl);
            }

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notifBuilder.build())
                    .putData("click_action", "https://foodiee.shop")
                    .putData("image", imageUrl != null ? imageUrl : "") // 👈 ఇక్కడ డేటా కీ యాడ్ చేయాలి[cite: 16]
                    .build();

            FirebaseMessaging.getInstance().send(message);
            System.out.println("Individual push notification sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // కస్టమర్లందరి FCM టోకెన్లకు ఫైర్‌బేస్ ద్వారా పుష్ నోటిఫికేషన్ పంపే మెథడ్
 // కస్టమర్లందరి FCM టోకెన్లకు ఫైర్‌బేస్ ద్వారా పుష్ నోటిఫికేషన్ పంపే మెథడ్
    private void sendPushNotificationToAllCustomers(String title, String body, String imageUrl) {
        try {
            List<Customer> customers = customerRepository.findAll();
            for (Customer customer : customers) {
                // 🌟 ఇక్కడ customer.getFcmToken() అని వాడాలి
                if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
                    
                    com.google.firebase.messaging.Notification.Builder notifBuilder = 
                        com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body != null ? body : "New update from Foodiee!");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        notifBuilder.setImage(imageUrl);
                    }

                    Message message = Message.builder()
                            .setToken(customer.getFcmToken()) 
                            .setNotification(notifBuilder.build())
                            .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                                .setNotification(com.google.firebase.messaging.AndroidNotification.builder()
                                    .setChannelId("foodiee_channel")
                                    .setSound("default")
                                    .setDefaultSound(true)
                                    .setDefaultVibrateTimings(true)
                                    .build())
                                .build())
                            .putData("click_action", "https://foodiee.shop")
                            .putData("image", imageUrl != null ? imageUrl : "") // 👈 ఇక్కడ కూడా డేటా కీ యాడ్ చేయాలి[cite: 16]
                            .build();

                    FirebaseMessaging.getInstance().send(message);
                }
            }
            System.out.println("Firebase push notifications sent successfully to all customers!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }
    
    
    
}