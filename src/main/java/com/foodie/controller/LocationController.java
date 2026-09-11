package com.foodie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class LocationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/track/delivery/{orderId}")
    public void updateLocation(@DestinationVariable String orderId, LocationDto location) {
        // కస్టమర్ & అడ్మిన్‌కి లైవ్ లొకేషన్ పంపడం
        messagingTemplate.convertAndSend("/topic/location/" + orderId, location);
    }
}

class LocationDto {
    private double latitude;
    private double longitude;
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}