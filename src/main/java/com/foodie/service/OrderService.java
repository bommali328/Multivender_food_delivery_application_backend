package com.foodie.service;

import com.foodie.model.DeliveryPartner;
import com.foodie.model.Order;
import com.foodie.repository.DeliveryPartnerRepository;
import com.foodie.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryPartnerRepository partnerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 30 seconds timeout kosam scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public Order placeOrder(Order order) {
        order.setOrderId("#ORD-" + (int)(Math.random() * 9000 + 1000));
        order.setStatus("Pending"); // 👈 Modata Pending lo undali
        Order savedOrder = orderRepository.save(order);

        // 1 & 4. Kotha order raagane andarki pamputhunda kakunda, lokation based khaaliga unna okka rider ki matrame pampadam
        assignOrderToAppropriatePartner(savedOrder.getId());

        // Shop owner ki live alert
        if (savedOrder.getShopId() != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "New Order Received!");
            payload.put("message", "New order " + savedOrder.getOrderId() + " placed.");
            messagingTemplate.convertAndSend("/topic/shop/" + savedOrder.getShopId(), payload);
        }

        return savedOrder;
    }

    /**
     * Online lo undi, busy ga leni (isBusy = false) saraina oka delivery partner ki matrame assign chesthundi.
     */
    public void assignOrderToAppropriatePartner(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !"Pending".equals(order.getStatus())) {
            return;
        }

        // Online lo unna, alane isBusy = false ga unna partners ni techukovadam
        List<DeliveryPartner> availablePartners = partnerRepository.findByIsOnlineTrueAndIsBusyFalse();

        if (availablePartners.isEmpty()) {
            order.setStatus("No Partner Available");
            orderRepository.save(order);
            return;
        }

        // 3. Same location / proximity logic (Daggardlo unna varini enchukovadam)
        DeliveryPartner selectedPartner = findNearestPartner(availablePartners, order.getShopLat(), order.getShopLng());

        // Order ni ee partner ki lock cheyadam
        order.setDeliveryPartnerId(selectedPartner.getId());
        order.setStatus("Assigned");
        orderRepository.save(order);

        // Kevalam ee nirdishta partner ki matrame WebSocket dwara notification pampadam
        Map<String, String> payload = new java.util.HashMap<>();
        payload.put("title", "New Delivery Request 🚀");
        payload.put("message", "Order #" + order.getOrderId() + " assigned to you.");
        messagingTemplate.convertAndSend("/topic/partner/" + selectedPartner.getMobile(), payload);

        // Timeout set cheyadam: 30 seconds lo accept cheyakapothe automatic ga vere variki mallinchadam
        scheduler.schedule(() -> checkTimeoutAndReassign(orderId, selectedPartner.getId()), 30, TimeUnit.SECONDS);
    }

    /**
     * Partner order ni accept leda reject chesinappudu
     */
    public boolean handlePartnerResponse(Long orderId, Long partnerId, String action) {
        Order order = orderRepository.findById(orderId).orElse(null);
        DeliveryPartner partner = partnerRepository.findById(partnerId).orElse(null);

        if (order == null || partner == null) return false;

        if (!"Assigned".equals(order.getStatus()) || (order.getDeliveryPartnerId() != null && !order.getDeliveryPartnerId().equals(partnerId))) {
            return false;
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            // 2. Order complete ayye varaku vere order raakunda partner ni 'Busy' cheyadam
            order.setStatus("Accepted");
            orderRepository.save(order);

            partner.setBusy(true);
            partnerRepository.save(partner);

            updateOrderStatus(order.getId(), "Accepted", order.getCustomerId(), order.getShopId(), partnerId);
            return true;

        } else if ("REJECT".equalsIgnoreCase(action)) {
            // 1. Reject chesthe vere delivery partner ki pampadam
            order.setDeliveryPartnerId(null);
            order.setStatus("Pending");
            orderRepository.save(order);

            assignOrderToAppropriatePartner(orderId);
            return false;
        }
        return false;
    }

    private void checkTimeoutAndReassign(Long orderId, Long partnerId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && "Assigned".equals(order.getStatus()) && order.getDeliveryPartnerId() != null && order.getDeliveryPartnerId().equals(partnerId)) {
            order.setDeliveryPartnerId(null);
            order.setStatus("Pending");
            orderRepository.save(order);

            assignOrderToAppropriatePartner(orderId);
        }
    }

    // Order complete ayyaka partner ni free (isBusy = false) cheyadam
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus("Completed");
            orderRepository.save(order);

            if (order.getDeliveryPartnerId() != null) {
                DeliveryPartner partner = partnerRepository.findById(order.getDeliveryPartnerId()).orElse(null);
                if (partner != null) {
                    partner.setBusy(false); // 2. Thadupari order ravatanki free cheyadam
                    partnerRepository.save(partner);
                }
            }
        }
    }

    private DeliveryPartner findNearestPartner(List<DeliveryPartner> partners, double shopLat, double shopLng) {
        DeliveryPartner nearest = partners.get(0);
        double minDistance = Double.MAX_VALUE;

        for (DeliveryPartner p : partners) {
            if (p.getLatitude() == null || p.getLongitude() == null) continue;
            double dist = Math.pow(p.getLatitude() - shopLat, 2) + Math.pow(p.getLongitude() - shopLng, 2);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    public List<Order> getOrdersByCustomerMobile(String mobile) {
        return orderRepository.findByCustomerMobile(mobile);
    }

    public List<Order> getAvailableOrdersForDelivery() {
        return orderRepository.findAll().stream()
                .filter(o -> "Ready".equalsIgnoreCase(o.getStatus()) || "Preparing".equalsIgnoreCase(o.getStatus()) || "Out for Delivery".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.toList());
    }

    public void updateOrderStatus(Long orderId, String newStatus, Long customerId, Long shopId, Long partnerId) {
        if (customerId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Order Status Update");
            payload.put("message", "Your order #" + orderId + " status is now: " + newStatus);
            messagingTemplate.convertAndSend("/topic/customer/" + customerId, payload);
        }

        if (shopId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Order Alert");
            payload.put("message", "Order #" + orderId + " status updated to: " + newStatus);
            messagingTemplate.convertAndSend("/topic/shop/" + shopId, payload);
        }

        if (partnerId != null) {
            Map<String, String> payload = new java.util.HashMap<>();
            payload.put("title", "Delivery Assignment");
            payload.put("message", "Order status: " + newStatus);
            messagingTemplate.convertAndSend("/topic/partner/" + partnerId, payload);
        }
    }
}