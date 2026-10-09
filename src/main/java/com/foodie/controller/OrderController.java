package com.foodie.controller;

import com.foodie.model.DeliveryPartner; // మీ మోడల్ ప్యాకేజీ ప్రకారం
import com.foodie.model.Order;
import com.foodie.model.User;
import com.foodie.repository.DeliveryPartnerRepository; // ఇది ఇంపార్టెంట్
import com.foodie.repository.OrderRepository;
import com.foodie.repository.UserRepository;
import com.foodie.service.WhatsAppNotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
	
	
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DeliveryPartnerRepository deliveryPartnerRepository; // ✅ డెలివరీ పార్టనర్ రిపాజిటరీ ఇంజెక్ట్ చేయబడింది

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private WhatsAppNotificationService whatsAppNotificationService; // వాట్సాప్ సర్వీస్ ఆటో-వైరింగ్

	// కొత్త ఆర్డర్ క్రియేట్ చేయడానికి / ప్లేస్ చేయడానికి (POST API)
		@PostMapping({ "/create", "/place" })
		public ResponseEntity<Order> createOrder(@RequestBody Order order) {
			if (order.getStatus() == null) {
				order.setStatus("Pending Approval");
			}

			// కస్టమర్ పేరు లేకపోతే యూజర్ రిపాజిటరీ నుండి ఫెచ్ చేసి సెట్ చేయడం
			if (order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()) {
				User existingUser = userRepository.findFirstByMobile(order.getCustomerMobile());
				if (existingUser != null && existingUser.getName() != null) {
					order.setCustomerName(existingUser.getName());
				} else {
					order.setCustomerName("Guest Customer");
				}
			}

			// ========================================================
			// ✅ 1. FIRST ORDER FREE & PROMO CODE VALIDATION
			// ========================================================
			String customerMobile = order.getCustomerMobile();
			String promoCode = order.getPromoCode();

			// 1. మొదటి ఆర్డర్ అయితే డెలివరీ ఫీజు ఫ్రీ చేయడం
			long previousOrdersCount = orderRepository.countByCustomerMobile(customerMobile);
			if (previousOrdersCount == 0) {
			    order.setDeliveryFee(0.0);
			}

			// 2. ప్రోమో కోడ్ వాలిడేషన్ (ఒక యూజర్‌కి ఒక కోడ్ ఒక్కసారే వాడాలి)
			if (promoCode != null && !promoCode.trim().isEmpty()) {
			    String cleanPromo = promoCode.trim().toUpperCase();
			    order.setPromoCode(cleanPromo);

			    boolean alreadyUsed = orderRepository.existsByCustomerMobileAndPromoCode(customerMobile, cleanPromo);
			    if (alreadyUsed) {
			        // కూపన్ ఇప్పటికే వాడి ఉంటే ఎర్రర్ రెస్పాన్స్ పంపడం
			        return ResponseEntity.status(400).body(null); 
			    }
			}

			// 3. 4-అంకెల డెలివరీ OTP ఆటోమేటిక్‌గా క్రియేట్ అవ్వడం
			if (order.getDeliveryOtp() == null || order.getDeliveryOtp().trim().isEmpty()) {
			    String randomOtp = String.format("%04d", (int)(Math.random() * 10000));
			    order.setDeliveryOtp(randomOtp);
			}

			// 4. అన్నీ సరిగ్గా ఉంటే ఒకేసారి ఆర్డర్‌ని డేటాబేస్‌లో సేవ్ చేయడం
			Order savedOrder = orderRepository.save(order);

			// ========================================================
			// ✅ 2. WHATSAPP NOTIFICATION INTEGRATION (DUAL MESSAGES)
			// ========================================================
			try {
			    whatsAppNotificationService.sendOrderConfirmationToWhatsApp(
			        savedOrder.getCustomerMobile(), 
			        savedOrder.getId(), 
			        savedOrder.getShopName(), 
			        savedOrder.getItems(), 
			        savedOrder.getDeliveryAddress(), 
			        savedOrder.getTotalAmount(), 
			        savedOrder.getTransactionId()
			    );

			    whatsAppNotificationService.sendOtpToWhatsApp(
			        savedOrder.getCustomerMobile(), 
			        savedOrder.getDeliveryOtp(), 
			        savedOrder.getId()
			    );
			} catch (Exception e) {
			    System.err.println("Instant WhatsApp notification failed: " + e.getMessage());
			}

			// నిర్దిష్టమైన షాప్ ఓనర్‌కి పాప్-అప్ నోటిఫికేషన్ పంపడం
			if (savedOrder.getShopId() != null) {
				messagingTemplate.convertAndSend("/topic/shop/" + savedOrder.getShopId(), savedOrder);
				messagingTemplate.convertAndSend("/topic/shop/orders/" + savedOrder.getShopId(), savedOrder);
			}

			// ========================================================
			// ✅ 3. SMART NEARBY DELIVERY ASSIGNMENT (Using Haversine Formula)
			// ========================================================
			try {
				Double shopLat = savedOrder.getShopLat();
				Double shopLng = savedOrder.getShopLng();

				List<DeliveryPartner> availablePartners = null;

				if (shopLat != null && shopLng != null) {
					double radiusInKm = 10.0;
					availablePartners = deliveryPartnerRepository.findAvailablePartnersNearby(shopLat, shopLng, radiusInKm);
				}

				if (availablePartners == null || availablePartners.isEmpty()) {
					availablePartners = deliveryPartnerRepository.findByIsOnlineTrueAndIsBusyFalse();
				}

				if (availablePartners != null && !availablePartners.isEmpty()) {
					DeliveryPartner assignedPartner = availablePartners.get(0);

					savedOrder.setDeliveryPartnerId(assignedPartner.getId());
					orderRepository.save(savedOrder);

					messagingTemplate.convertAndSend("/topic/delivery/orders/" + assignedPartner.getId(), savedOrder);
					
					System.out.println("Order assigned exclusively to nearby partner ID: " + assignedPartner.getId());
				} else {
					System.out.println("No delivery partners available nearby right now.");
				}
			} catch (Exception e) {
				System.err.println("Smart partner assignment failed: " + e.getMessage());
			}

			return ResponseEntity.ok(savedOrder);
		}
	// కస్టమర్ మొబైల్ నంబర్ ఆధారంగా ఆర్డర్ హిస్టరీ చూడటానికి (GET API)
	@GetMapping("/customer/{mobile}")
	public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String mobile) {
		List<Order> orders = orderRepository.findByCustomerMobileOrderByIdDesc(mobile);
		return ResponseEntity.ok(orders);
	}

	// డెలివరీ పార్టనర్ ఐడీ ఆధారంగా ఆర్డర్ హిస్టరీ తెచ్చుకోవడానికి (GET API)
	@GetMapping("/partner/history/{partnerId}")
	public ResponseEntity<List<Order>> getPartnerOrderHistory(@PathVariable Long partnerId) {
		List<Order> partnerOrders = orderRepository.findByDeliveryPartnerId(partnerId);
		return ResponseEntity.ok(partnerOrders);
	}
	
	// షాప్ ఐడీ ఆధారంగా ఆర్డర్స్ ఫెచ్ చేయడానికి
	@GetMapping("/shop/{shopId}")
	public ResponseEntity<List<Order>> getOrdersByShop(@PathVariable Long shopId) {
		List<Order> orders = orderRepository.findByShopIdOrderByIdDesc(shopId);
		return ResponseEntity.ok(orders);
	}

	// 🛡️ ADMIN PANEL ENDPOINT
	@GetMapping({ "/all", "/admin/all" })
	public ResponseEntity<List<Order>> getAllOrdersForAdmin() {
		List<Order> allOrders = orderRepository.findAll();
		return ResponseEntity.ok(allOrders);
	}

	@PostMapping("/accept/{id}")
	public ResponseEntity<Order> acceptOrder(@PathVariable Long id,
			@RequestBody(required = false) Order deliveryPayload) {
		Order order = orderRepository.findById(id).orElse(null);
		if (order != null) {
			order.setStatus("Accepted by Delivery Partner");
			
			if (deliveryPayload != null) {
				if (deliveryPayload.getDeliveryFee() > 0) {
					order.setDeliveryFee(deliveryPayload.getDeliveryFee());
				}
				if (deliveryPayload.getDeliveryPartnerId() != null) {
					order.setDeliveryPartnerId(deliveryPayload.getDeliveryPartnerId());
				}
			}
			
			Order updatedOrder = orderRepository.save(order);
			return ResponseEntity.ok(updatedOrder);
		}
		return ResponseEntity.notFound().build();
	}
	
	@GetMapping("/customer-history/{mobile}")
	public ResponseEntity<?> getCustomerOrderHistory(@PathVariable String mobile) {
		try {
			List<Order> orders = orderRepository.findByCustomerMobile(mobile);
			return ResponseEntity.ok(orders);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(List.of());
		}
	}

	// ✅ డెలివరీ యాప్ ఆటో-పోలింగ్ కోసం పెండింగ్ ఆర్డర్స్ బ్యాకప్ API
	@GetMapping("/pending-delivery/{partnerId}")
	public ResponseEntity<List<Order>> getPendingDeliveriesForPartner(@PathVariable Long partnerId) {
	    try {
	        List<Order> pendingOrders = orderRepository.findAll()
	            .stream()
	            .filter(o -> partnerId.equals(o.getDeliveryPartnerId()) 
	                && ("Pending Approval".equals(o.getStatus()) || "Assigned".equals(o.getStatus()) || "Accepted by Delivery Partner".equals(o.getStatus())))
	            .toList();
	        return ResponseEntity.ok(pendingOrders);
	    } catch (Exception e) {
	        return ResponseEntity.ok(List.of());
	    }
	}
	// డెలివరీ కంప్లీట్ చేయడానికి OTP వెరిఫై చేసే API (POST API)
	@PostMapping("/verify-delivery/{id}")
	public ResponseEntity<?> verifyAndCompleteDelivery(@PathVariable Long id, @RequestBody Map<String, String> payload) {
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) {
			return ResponseEntity.status(404).body("Order not found");
		}

		String enteredOtp = payload.get("otp");
		String partnerIdStr = payload.get("partnerId");

		if (enteredOtp != null && enteredOtp.equals(order.getDeliveryOtp())) {
			order.setStatus("Delivered");
			
			if (partnerIdStr != null && !partnerIdStr.isEmpty()) {
				order.setDeliveryPartnerId(Long.valueOf(partnerIdStr));
			}
			
			orderRepository.save(order);
			
			messagingTemplate.convertAndSend("/topic/order/status/" + id, "Delivered");

			return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Delivery completed successfully!"));
		} else {
			return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid Delivery OTP!"));
		}
	}
	
	@PostMapping("/decline/{id}")
	public ResponseEntity<?> declineOrder(@PathVariable Long id, @RequestBody Map<String, Long> payload) {
	    Order order = orderRepository.findById(id).orElse(null);
	    if (order != null) {
	        // Decline chesinappudu delivery partner assignment ni remove cheyali
	        order.setDeliveryPartnerId(null);
	        order.setStatus("Declined");
	        orderRepository.save(order);
	        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Order declined successfully"));
	    }
	    return ResponseEntity.status(404).body("Order not found");
	}
	
	
	
	// ఆర్డర్ స్టేటస్ అప్‌డేట్ చేయడానికి (PUT API)
	@PutMapping("/status/{orderId}")
	public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
		Optional<Order> orderOpt = orderRepository.findById(orderId);
		if (orderOpt.isPresent()) {
			Order order = orderOpt.get();
			order.setStatus(status); 
			orderRepository.save(order);

			messagingTemplate.convertAndSend("/topic/order/status/" + orderId, status);

			return ResponseEntity.ok(Map.of("status", "success", "message", "Order status updated to " + status));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
	}
	
	
	
}