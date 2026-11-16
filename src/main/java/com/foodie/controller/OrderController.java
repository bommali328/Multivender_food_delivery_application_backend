package com.foodie.controller;

import com.foodie.model.Order;
import com.foodie.model.User;
import com.foodie.repository.CustomerRepository;
import com.foodie.repository.DeliveryPartnerRepository; // ఇది ఇంపార్టెంట్
import com.foodie.repository.OrderRepository;
import com.foodie.repository.UserRepository;
import com.foodie.service.FirebaseMessagingService;
import com.foodie.service.NotificationService;
import com.foodie.service.OrderService;
import com.foodie.service.WhatsAppNotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


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
	private WhatsAppNotificationService whatsAppNotificationService; 
	// వాట్సాప్ సర్వీస్ ఆటో-వైరింగ్
	
	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private NotificationService notificationService;
	
	@Autowired
    private FirebaseMessagingService firebaseMessagingService;
	
	@Autowired
	private OrderService orderAssignmentService;
	


	@Autowired
	private com.foodie.repository.ShopRepository shopRepository; // షాప్ పేరు కరెక్ట్‌గా తెచ్చుకోవడానికి

	// కొత్త ఆర్డర్ క్రియేట్ చేయడానికి / ప్లేస్ చేయడానికి (POST API)
	@PostMapping({ "/create", "/place" })
	public ResponseEntity<Order> createOrder(@RequestBody Order order) {
		
		// 🛡️ 1. డెలివరీ అడ్రస్ లేకపోతే (Null లేదా Empty అయితే) ఆర్డర్ రిజెక్ట్ చేయడం
		if (order.getDeliveryAddress() == null || order.getDeliveryAddress().trim().isEmpty()) {
			System.err.println("❌ Order Creation Failed: Delivery Address is missing!");
			return ResponseEntity.status(400).body(null); 
		}

		if (order.getStatus() == null) {
			order.setStatus("Pending"); // 👈 మొదట Pending లో ఉంచాలి
		}

		// 🏪 2. షాప్ ఐడీ ఉండి, డేటాబేస్ నుండి కరెక్ట్ షాప్ పేరు తెచ్చి సెట్ చేయడం (డిఫాల్ట్ పేర్లు ఉండవు)
		if (order.getShopId() != null) {
			com.foodie.model.Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
			if (shop != null && shop.getShopName() != null && !shop.getShopName().trim().isEmpty()) {
				order.setShopName(shop.getShopName().trim());
			} else {
				System.err.println("❌ Order Failed: Valid Shop not found for Shop ID: " + order.getShopId());
				return ResponseEntity.status(400).body(null); // సరైన షాప్ లేకపోతే ఆర్డర్ రిజెక్ట్ అవుతుంది
			}
		} else {
			System.err.println("❌ Order Failed: Shop ID is missing!");
			return ResponseEntity.status(400).body(null); // షాప్ ఐడీ లేకపోయినా ఆర్డర్ తీసుకోకూడదు
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

		// 3. మొదటి ఆర్డర్ అయితే డెలివరీ ఫీజు ఫ్రీ చేయడం, లేదంటే దూరం (Distance) బట్టి ఫీజు క్యాలెక్యులేట్ చేయడం
				String customerMobile = order.getCustomerMobile();
				long previousOrdersCount = orderRepository.countByCustomerMobile(customerMobile);
				
				if (previousOrdersCount == 0) {
					order.setDeliveryFee(0.0);
				} else {
					// 📐 షాప్ మరియు కస్టమర్ మధ్య దూరం (Distance) ఆధారంగా డెలివరీ ఫీజు లెక్కించడం
					double shopLat = order.getShopLat();
					double shopLng = order.getShopLng();
					double customerLat = order.getCustomerLat();
					double customerLng = order.getCustomerLng();

					if (shopLat != 0.0 && shopLng != 0.0 && customerLat != 0.0 && customerLng != 0.0) {
						double earthRadius = 6371; // కిలోమీటర్లలో భూమి వ్యాసార్థం
						double dLat = Math.toRadians(customerLat - shopLat);
						double dLon = Math.toRadians(customerLng - shopLng);
						double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
						           Math.cos(Math.toRadians(shopLat)) * Math.cos(Math.toRadians(customerLat)) *
						           Math.sin(dLon / 2) * Math.sin(dLon / 2);
						double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
						double distanceKm = earthRadius * c;

						// 💰 ఫీజు లాజిక్: 2 కి.మీ వరకు రూ. 20, ఆపై ప్రతి అదనపు కిమీకి రూ. 10 అదనం
						double calculatedFee = 20.0;
						if (distanceKm > 2.0) {
							calculatedFee = 20.0 + Math.ceil(distanceKm - 2.0) * 10.0;
						}
						order.setDeliveryFee(calculatedFee);
					} else {
						order.setDeliveryFee(20.0); // డిఫాల్ట్ ఫీజు
					}
				}
		

		// 4. ప్రోమో కోడ్ వాలిడేషన్
		String promoCode = order.getPromoCode();
		if (promoCode != null && !promoCode.trim().isEmpty()) {
			String cleanPromo = promoCode.trim().toUpperCase();
			order.setPromoCode(cleanPromo);

			boolean alreadyUsed = orderRepository.existsByCustomerMobileAndPromoCode(customerMobile, cleanPromo);
			if (alreadyUsed) {
				return ResponseEntity.status(400).body(null); 
			}
		}

		// 5. 4-అంకెల డెలివరీ OTP ఆటోమేటిక్‌గా క్రియేట్ అవ్వడం
		if (order.getDeliveryOtp() == null || order.getDeliveryOtp().trim().isEmpty()) {
			String randomOtp = String.format("%04d", (int)(Math.random() * 10000));
			order.setDeliveryOtp(randomOtp);
		}

		// 6. ఆర్డర్‌ని డేటాబేస్‌లో సేవ్ చేయడం
		Order savedOrder = orderRepository.save(order);
		
		System.out.println("📌 [DEBUG] Order Saved Successfully with ID: " + savedOrder.getId());
		System.out.println("📌 [DEBUG] Customer Mobile: " + savedOrder.getCustomerMobile());
		System.out.println("📌 [DEBUG] Shop Name: " + savedOrder.getShopName());
		System.out.println("📌 [DEBUG] Delivery Address: " + savedOrder.getDeliveryAddress());

		
		// ========================================================
				// 📱 వాట్సాప్ ఆటోమేటిక్ నోటిఫికేషన్స్ & OTP పంపడం (Async Background Call)
				// ========================================================
				try {
				    // ఇక్కడ మెసేజ్ పంపే పనులు అన్నీ @Async ద్వారా బ్యాక్‌గ్రౌండ్‌లో రన్ అవుతాయి కాబట్టి ఆర్డర్ ఇన్‌స్టెంట్‌గా సక్సెస్ అవుతుంది
				    whatsAppNotificationService.sendOrderConfirmationToWhatsApp(
				        savedOrder.getCustomerMobile(),
				        savedOrder.getId(),
				        savedOrder.getShopName(), 
				        savedOrder.getItems(), 
				        savedOrder.getDeliveryAddress(),
				        savedOrder.getTotalAmount(),
				        savedOrder.getTransactionId()
				    );

				    if (savedOrder.getDeliveryOtp() != null) {
				        whatsAppNotificationService.sendOtpToWhatsApp(
				            savedOrder.getCustomerMobile(),
				            savedOrder.getDeliveryOtp(),
				            savedOrder.getId()
				        );
				    }
				} catch (Exception e) {
				    System.err.println("WhatsApp background trigger warning: " + e.getMessage());
				}

		// ========================================================
		// 🚀 SMART 1-BY-1 DELIVERY PARTNER ASSIGNMENT
		// ========================================================
		try {
			orderAssignmentService.assignOrderToAppropriatePartner(savedOrder.getId());
		} catch (Exception e) {
			System.err.println("Smart partner assignment failed: " + e.getMessage());
		}

		// షాప్ ఓనర్‌కి పాప్-అప్ నోటిఫికేషన్ పంపడం
		if (savedOrder.getShopId() != null) {
			messagingTemplate.convertAndSend("/topic/shop/" + savedOrder.getShopId(), savedOrder);
			messagingTemplate.convertAndSend("/topic/shop/orders/" + savedOrder.getShopId(), savedOrder);
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
	        // 🔴 స్టేటస్‌ను స్పష్టంగా ACCEPTED అని మార్చాలి
	        order.setStatus("ACCEPTED");
	        
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

	// ✅ డెలివరీ యాప్ ఆటో-పోలింగ్ కోసం పెండింగ్ ఆర్డర్స్ బ్యాకప్ API (Fixed)
		@GetMapping("/pending-delivery/{partnerId}")
		public ResponseEntity<List<Order>> getPendingDeliveriesForPartner(@PathVariable Long partnerId) {
		    try {
		        List<Order> pendingOrders = orderRepository.findAll()
		            .stream()
		            .filter(o -> partnerId.equals(o.getDeliveryPartnerId()) 
		                && ("Pending".equalsIgnoreCase(o.getStatus()) 
		                    || "Ready".equalsIgnoreCase(o.getStatus()) 
		                    || "Pending Approval".equalsIgnoreCase(o.getStatus()) 
		                    || "Assigned".equalsIgnoreCase(o.getStatus()) 
		                    || "Accepted by Delivery Partner".equalsIgnoreCase(o.getStatus())))
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
	
	
	// ఆర్డర్ స్టేటస్ అప్‌డేట్ చేయడానికి మరియు కస్టమర్‌కి పుష్ నోటిఫికేషన్ పంపడానికి (PUT API)
	@PutMapping({"/status/{orderId}", "/update-status/{orderId}"})
	public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
	    Order order = orderRepository.findById(orderId).orElse(null);
	    if (order == null) {
	        return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
	    }

	    order.setStatus(status); // ఉదాహరణకు: "Out for Delivery"
	    orderRepository.save(order);

	    // వెబ్‌సాకెట్ ద్వారా రియల్-టైమ్ అప్‌డేట్ పంపడం
	    messagingTemplate.convertAndSend("/topic/order/status/" + orderId, status);

	    // 🌟 కస్టమర్‌కి ఆటోమేటిక్‌గా పుష్ నోటిఫికేషన్ పంపడం (UserRepository ఉపయోగించి)
	    String customerMobile = order.getCustomerMobile();
	    if (customerMobile != null && !customerMobile.isEmpty()) {
	        User customer = userRepository.findFirstByMobile(customerMobile); // 👈 Customer బదులుగా User వాడాలి
	        if (customer != null && customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
	        	String title = "🔥 𝙁𝙤𝙤𝙙𝙞𝙚𝙚 • Order Ready! 🚀";
	        	String body = "🍔 Your order #" + orderId + " is freshly prepared & ready for delivery! 🎉";
	            // NotificationService లోని మెథడ్‌ని కాల్ చేయడం
	            notificationService.sendPushNotificationToCustomer(customer.getFcmToken(), title, body, order.getImageUrl());
	        }
	    }

	    return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Order status updated & notification sent"));
	}
}