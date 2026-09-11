package com.foodie.service;

import com.foodie.model.Notification;
import com.foodie.repository.NotificationRepository;
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

    public Notification sendAndBroadcastNotification(Notification notification) {
        // డేటాబేస్‌లో సేవ్ చేయడం
        Notification saved = notificationRepository.save(notification);

        // టార్గెట్ ఆడియన్స్ బట్టి వెబ్‌సాకెట్ ద్వారా రియల్-టైమ్‌లో పంపడం
        String target = saved.getTargetAudience();
        if (target != null) {
            if (target.equalsIgnoreCase("All Users")) {
                messagingTemplate.convertAndSend("/topic/notifications/all", saved);
            } else if (target.equalsIgnoreCase("Customers")) {
                messagingTemplate.convertAndSend("/topic/notifications/customers", saved);
            } else if (target.equalsIgnoreCase("Delivery Partners")) {
                messagingTemplate.convertAndSend("/topic/notifications/partners", saved);
            } else if (target.equalsIgnoreCase("Shop Owners")) {
                messagingTemplate.convertAndSend("/topic/notifications/shops", saved);
            }
        }

        return saved;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }
}