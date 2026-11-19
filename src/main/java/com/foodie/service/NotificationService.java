package com.foodie.service;

import com.foodie.model.Notification;
import com.foodie.model.User;
import com.foodie.repository.NotificationRepository;
import com.foodie.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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
    private UserRepository userRepository;

    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    public Notification sendAndBroadcastNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);

        String target = saved.getTargetAudience();
        String msgContent = saved.getMessage();
        String imgUrl = saved.getImageUrl();

        if (target != null) {
            if (target.equalsIgnoreCase("All Users")) {
                messagingTemplate.convertAndSend("/topic/notifications/all", saved);
                sendPushNotificationToAllCustomers("Foodiee Alert", msgContent, imgUrl);
            } else if (target.equalsIgnoreCase("Customers") || target.equalsIgnoreCase("All Customers")) {
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

    // నిర్దిష్టమైన కస్టమర్‌కి (ఆర్డర్ అప్‌డేట్స్ కోసం) పుష్ నోటిఫికేషన్ పంపే మెథడ్
    public void sendPushNotificationToCustomer(String fcmToken, String title, String body, String imageUrl) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                System.err.println("❌ FirebaseApp is not initialized! Check serviceAccountKey.json.");
                return;
            }
            if (fcmToken == null || fcmToken.trim().isEmpty()) {
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
                    .setToken(fcmToken.trim())
                    .setNotification(notifBuilder.build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setColor("#FC8019")
                                    .setSound("default")
                                    .setDefaultSound(true)
                                    .setDefaultVibrateTimings(true)
                                    .build())
                            .build())
                    .putData("click_action", "https://foodiee.shop")
                    .putData("image", imageUrl != null ? imageUrl : "")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Individual push notification sent: " + response);
        } catch (Exception e) {
            System.err.println("❌ Individual push error: " + e.getMessage());
        }
    }

    // Order status కి తగ్గట్టు కస్టమర్‌కి నోటిఫికేషన్ పంపే హెల్పర్ మెథడ్
    public void sendOrderStatusNotification(Long orderId, String customerMobile, String status, String imageUrl) {
        String cleanMobile = normalizeMobile(customerMobile);
        if (cleanMobile.isEmpty()) return;

        User customer = userRepository.findFirstByMobile(cleanMobile);
        if (customer == null) {
            customer = userRepository.findByMobile(cleanMobile);
        }

        if (customer != null && customer.getFcmToken() != null && !customer.getFcmToken().trim().isEmpty()) {
            String title = "🔥 Foodiee • Order #" + orderId;
            String body = "📦 Order status updated to: " + status;

            if ("Pending".equalsIgnoreCase(status) || "Placed".equalsIgnoreCase(status)) {
                body = "🛒 Your order #" + orderId + " has been placed successfully!";
            } else if ("ACCEPTED".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) {
                body = "✅ Shop accepted your order #" + orderId + "!";
            } else if ("Preparing".equalsIgnoreCase(status)) {
                body = "👨‍🍳 Your food for order #" + orderId + " is being prepared!";
            } else if ("Out for Delivery".equalsIgnoreCase(status) || "Picked Up".equalsIgnoreCase(status)) {
                body = "🛵 Your order #" + orderId + " is on the way!";
            } else if ("Delivered".equalsIgnoreCase(status)) {
                body = "🎉 Order #" + orderId + " delivered. Enjoy your meal!";
            }

            sendPushNotificationToCustomer(customer.getFcmToken(), title, body, imageUrl);
        }
    }

    // users టేబుల్‌లోని కస్టమర్లందరి FCM టోకెన్లకు పుష్ నోటిఫికేషన్ పంపే మెథడ్
    private void sendPushNotificationToAllCustomers(String title, String body, String imageUrl) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                System.err.println("❌ FirebaseApp is not initialized! Check serviceAccountKey.json.");
                return;
            }

            List<User> customers = userRepository.findAll();
            for (User customer : customers) {
                if (customer.getFcmToken() != null && !customer.getFcmToken().trim().isEmpty()) {
                    com.google.firebase.messaging.Notification.Builder notifBuilder =
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body != null ? body : "New update from Foodiee!");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        notifBuilder.setImage(imageUrl);
                    }

                    Message message = Message.builder()
                            .setToken(customer.getFcmToken().trim())
                            .setNotification(notifBuilder.build())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(AndroidNotification.builder()
                                            .setColor("#FC8019")
                                            .setSound("default")
                                            .setDefaultSound(true)
                                            .setDefaultVibrateTimings(true)
                                            .build())
                                    .build())
                            .putData("click_action", "https://foodiee.shop")
                            .putData("image", imageUrl != null ? imageUrl : "")
                            .build();

                    FirebaseMessaging.getInstance().send(message);
                }
            }
            System.out.println("✅ Firebase push notifications sent to users table customers!");
        } catch (Exception e) {
            System.err.println("❌ Bulk FCM error: " + e.getMessage());
        }
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }
}