package com.foodie.service;

import com.foodie.model.Order;
import com.foodie.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // 👈 WebSocket నోటిఫికేషన్ల కోసం

    public Order placeOrder(Order order) {
        order.setOrderId("#ORD-" + (int)(Math.random() * 9000 + 1000));
        order.setStatus("Pending Approval");
        Order savedOrder = orderRepository.save(order);

        // 🔔 కొత్త ఆర్డర్ ప్లేస్ అయినప్పుడు షాప్ ఓనర్‌కి లైవ్ అలర్ట్ పంపడం
        if (savedOrder.getShopId() != null) {
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "New Order Received!");
            payload.put("message", "New order " + savedOrder.getOrderId() + " placed.");
            
            messagingTemplate.convertAndSend("/topic/shop/" + savedOrder.getShopId(), payload);
        }

        return savedOrder;
    }

    public List<Order> getOrdersByCustomerMobile(String mobile) {
        return orderRepository.findByCustomerMobile(mobile);
    }

    // షాప్ అప్రూవ్ చేసిన లేదా రెడీగా ఉన్న ఆర్డర్‌లను డెలివరీ పార్టనర్ కోసం ఫిల్టర్ చేయడం
    public List<Order> getAvailableOrdersForDelivery() {
        return orderRepository.findAll().stream()
                .filter(o -> "Ready".equalsIgnoreCase(o.getStatus()) || "Preparing".equalsIgnoreCase(o.getStatus()) || "Out for Delivery".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.toList());
    }

    // డెలివరీ పార్టనర్ ఆర్డర్ యాక్సెప్ట్ చేయడానికి
    public Order acceptOrderByPartner(Long orderId, Long partnerId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus("Out for Delivery");
            // order.setPartnerId(partnerId); // ఒకవేళ ఫీల్డ్ ఉంటే అన్‌కమೆಂಟ್ చేయండి
            Order updatedOrder = orderRepository.save(order);

            updateOrderStatus(
                    updatedOrder.getId(), 
                    "Out for Delivery", 
                    updatedOrder.getCustomerId(), 
                    updatedOrder.getShopId(), 
                    partnerId
                );

                return updatedOrder;
            }
            return null;
        }

    // ==========================================
    // 🚀 LIVE WEBSOCKET NOTIFICATION METHOD
    // ==========================================
    public void updateOrderStatus(Long orderId, String newStatus, Long customerId, Long shopId, Long partnerId) {
        // 1. కస్టమర్‌కి నోటిఫికేషన్ పంపడం
        if (customerId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Order Status Update");
            payload.put("message", "Your order #" + orderId + " status is now: " + newStatus);
            messagingTemplate.convertAndSend("/topic/customer/" + customerId, payload);
        }

        // 2. షాప్ ఓనర్‌కి నోటిఫికేషన్ పంపడం
        if (shopId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Order Alert");
            payload.put("message", "Order #" + orderId + " status updated to: " + newStatus);
            messagingTemplate.convertAndSend("/topic/shop/" + shopId, payload);
        }

        // 3. డెలివరీ పార్టనర్‌కి నోటిఫికేషన్ పంపడం (అసైన్ అయినట్లయితే)
        if (partnerId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Delivery Assignment");
            payload.put("message", "You have been assigned to order #" + orderId);
            messagingTemplate.convertAndSend("/topic/partner/" + partnerId, payload);
        }
    }
}