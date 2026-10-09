package com.foodie.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.foodie.model.DeliveryPartner;
import com.foodie.model.Order;
import com.foodie.repository.DeliveryPartnerRepository;
import com.foodie.repository.OrderRepository;

import java.util.List;

@Service
public class OrderAssignmentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ప్రతి 30 సెకన్లకు ఒకసారి పెండింగ్ ఆర్డర్‌లను చెక్ చేసి, ఎవరూ యాక్సెప్ట్ చేయకపోతే వేరే వారికి పంపడం
    @Scheduled(fixedRate = 30000) 
    public void reassignUnacceptedOrders() {
        try {
            // "Pending Approval" లేదా ఇంకా డెలివరీ పార్టనర్ రెస్పాండ్ కాని ఆర్డర్‌లను తీసుకోవడం
            List<Order> pendingOrders = orderRepository.findAll()
                .stream()
                .filter(o -> "Pending Approval".equals(o.getStatus()))
                .toList();

            for (Order order : pendingOrders) {
                // ఆన్‌లైన్‌లో మరియు ఫ్రీగా ఉన్న పార్టనర్లను వెతకడం
                List<DeliveryPartner> availablePartners = deliveryPartnerRepository.findByIsOnlineTrueAndIsBusyFalse();

                if (availablePartners != null && !availablePartners.isEmpty()) {
                    // ప్రస్తుత పార్టనర్ కాకుండా వేరే పార్టనర్‌ని ఎంచుకోవడం
                    DeliveryPartner nextPartner = availablePartners.get(0);
                    
                    // ఒకవేళ ఆల్రెడీ అదే పార్టనర్‌కి అసైన్ అయి ఉంటే వేరేవారిని చూడాలి
                    if (nextPartner.getId().equals(order.getDeliveryPartnerId())) {
                        if (availablePartners.size() > 1) {
                            nextPartner = availablePartners.get(1);
                        } else {
                            continue; // వేరే పార్టనర్ లేకపోతే అలాగే ఉంచడం
                        }
                    }

                    // కొత్త పార్టనర్‌కి ఆర్డర్ అప్‌డేట్ చేయడం
                    order.setDeliveryPartnerId(nextPartner.getId());
                    orderRepository.save(order);

                    // కొత్త పార్టనర్‌కి వెబ్‌సాకెట్ ద్వారా నోటిఫికేషన్ పంపడం
                    messagingTemplate.convertAndSend("/topic/delivery/orders/" + nextPartner.getId(), order);
                    
                    System.out.println("Order reassigned to next partner ID: " + nextPartner.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Order reassignment error: " + e.getMessage());
        }
    }
}