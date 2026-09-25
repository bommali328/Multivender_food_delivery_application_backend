package com.foodie.controller;

import com.foodie.model.Order;
import com.foodie.model.User;
import com.foodie.repository.OrderRepository;
import com.foodie.repository.UserRepository;

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
	private SimpMessagingTemplate messagingTemplate;
	// కొత్త ఆర్డర్ క్రియేట్ చేయడానికి / ప్లేస్ చేయడానికి మరియు రియల్ టైమ్ పాపప్ పంపడానికి (POST API)
			@PostMapping({ "/create", "/place" })
			public ResponseEntity<?> createOrder(@RequestBody Order order) {
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
				// ✅ 1. FIRST ORDER FREE & PROMO CODE ONE-TIME USE VALIDATION
				// ========================================================
				String customerMobile = order.getCustomerMobile();
				String promoCode = order.getPromoCode();

				// ఈ మొబైల్ నంబర్‌పై ఇదివరకే ఏమైనా ఆర్డర్లు ఉన్నాయా అని కౌంట్ చేయడం
				long previousOrdersCount = orderRepository.countByCustomerMobile(customerMobile);

				// ఒకవేళ ఇది మొదటి ఆర్డర్ అయితే ఆటోమేటిక్‌గా డెలివరీ ఫీజు సున్నా (Free)
				if (previousOrdersCount == 0) {
					order.setDeliveryFee(0.0);
				}

				// ఒకవేళ కస్టమర్ ప్రొమో కోడ్ వాడు ఉంటే దానిని వెరిఫై చేయడం
				if (promoCode != null && !promoCode.trim().isEmpty()) {
					String cleanPromo = promoCode.trim().toUpperCase();

					// వన్-టైమ్ యూజ్ చెక్: ఈ కస్టమర్ ఈ కోడ్‌ని ఇదివరకే వాడేశారా లేదా?
					boolean alreadyUsed = orderRepository.existsByCustomerMobileAndPromoCode(customerMobile, cleanPromo);
					if (alreadyUsed) {
						return ResponseEntity.badRequest().body(Map.of("error", "ఈ కూపన్ కోడ్ మీరు ఇప్పటికే వాడేశారు! ఇది కేవలం ఒక్కసారే వర్తిస్తుంది."));
					}

					// మొదటి ఆర్డర్ కోడ్ అయితే, పాత ఆర్డర్లు ఉన్నాయో లేదో చెక్ చేయడం
					if (cleanPromo.equals("FIRSTORDERFREE") || cleanPromo.equals("FIRST50")) {
						if (previousOrdersCount > 0) {
							return ResponseEntity.badRequest().body(Map.of("error", "ఈ కూపన్ కేవలం కొత్త యూజర్ల మొదటి ఆర్డర్‌కు మాత్రమే వర్తిస్తుంది."));
						}
					}
				}

				// ఆర్డర్ జనరేట్ అయ్యేటప్పుడు 4-అంకెల డెలివరీ OTP ఆటోమేటిక్‌గా క్రియేట్ అవ్వడం (లేకపోతే)
				if (order.getDeliveryOtp() == null || order.getDeliveryOtp().trim().isEmpty()) {
					String randomOtp = String.format("%04d", (int)(Math.random() * 10000));
					order.setDeliveryOtp(randomOtp);
				}

				Order savedOrder = orderRepository.save(order);

				// 1. నిర్దిష్టమైన షాప్ ఓనర్‌కి పాపప్ నోటిఫికేషన్ పంపడం
				if (savedOrder.getShopId() != null) {
					messagingTemplate.convertAndSend("/topic/shop/" + savedOrder.getShopId(), savedOrder);
				}

				// 2. డెలివరీ పార్టనర్ అందరికీ రియల్ టైమ్ పాపప్ బ్రాడ్‌కాస్ట్ చేయడం (ఫ్రంట్‌ఎండ్ లిజనర్‌తో మ్యాచ్ అవ్వడానికి)
				messagingTemplate.convertAndSend("/topic/delivery-partners", savedOrder);
				
				// ✅ డెలివరీ యాప్ రియల్ టైమ్ అలర్ట్ కోసం అదనపు బ్రాడ్‌కాస్ట్ ఛానెల్
				messagingTemplate.convertAndSend("/topic/broadcast/delivery", savedOrder);
				
				// ఒకవేళ ఆర్డర్‌కి నిర్దిష్టంగా పార్ట్‌నర్ ఐడీ ఉంటే ఆ పర్టికులర్ పార్ట్‌నర్‌కి కూడా పంపడం
				if (savedOrder.getDeliveryPartnerId() != null) {
					// ఒకవేళ పర్టికులర్ పార్ట్‌నర్ అసైన్ అయితే కేవలం ఆడికే పంపాలి
					messagingTemplate.convertAndSend("/topic/delivery/orders/" + savedOrder.getDeliveryPartnerId(), savedOrder);
				} else {
					// లేకపోతే జనరల్ బ్రాడ్‌కాస్ట్
					messagingTemplate.convertAndSend("/topic/broadcast/delivery", savedOrder);
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
	
	// షాప్ ఐడీ ఆధారంగా ఆర్డర్స్ ఫెచ్ చేయడానికి (ShopOwnerApp కోసం)
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
	
	// ఆర్డర్ స్టేటస్ అప్‌డేట్ చేయడానికి మరియు కస్టమర్‌కి WebSocket ద్వారా బ్రాడ్‌కాస్ట్ చేయడానికి (PUT API)
	@PutMapping("/status/{orderId}")
	public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
		Optional<Order> orderOpt = orderRepository.findById(orderId);
		if (orderOpt.isPresent()) {
			Order order = orderOpt.get();
			order.setStatus(status); 
			orderRepository.save(order);

			messagingTemplate.convertAndSend("/topic/delivery-partners", order);
			messagingTemplate.convertAndSend("/topic/order/status/" + orderId, status);

			return ResponseEntity.ok(Map.of("status", "success", "message", "Order status updated to " + status));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
	}
}