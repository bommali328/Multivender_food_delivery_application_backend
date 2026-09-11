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

		// 2. డెలివరీ పార్టనర్ అందరికీ రియల్ టైమ్ పాపప్ బ్రాడ్‌కాస్ట్ చేయడం
		messagingTemplate.convertAndSend("/topic/delivery-partners", savedOrder);

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

	// డెలివరీ కంప్లీట్ చేయడానికి OTP వెరిఫై చేసే API (POST API)
	@PostMapping("/verify-delivery/{id}")
	public ResponseEntity<?> verifyAndCompleteDelivery(@PathVariable Long id, @RequestBody Map<String, String> payload) {
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) {
			return ResponseEntity.status(404).body("Order not found");
		}

		String enteredOtp = payload.get("otp");
		String partnerIdStr = payload.get("partnerId"); // 👈 పార్టనర్ ఐడీని రిసీవ్ చేసుకోవడం

		if (enteredOtp != null && enteredOtp.equals(order.getDeliveryOtp())) {
			order.setStatus("Delivered");
			
			// ఒకవేళ పార్టనర్ ఐడీ ఉంటే ఆర్డర్‌కి సేవ్ చేయడం
			if (partnerIdStr != null && !partnerIdStr.isEmpty()) {
				order.setDeliveryPartnerId(Long.valueOf(partnerIdStr));
			}
			
			orderRepository.save(order);
			
			// కస్టమర్‌కి లైవ్ స్టేటస్ పంపడం
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

	        // 🚀 కరెక్ట్ వేరియబుల్ 'order' ఇక్కడ వాడాలి
	        messagingTemplate.convertAndSend("/topic/delivery-partners", order);
	        messagingTemplate.convertAndSend("/topic/order/status/" + orderId, status);

	        return ResponseEntity.ok(Map.of("status", "success", "message", "Order status updated to " + status));
	    }
	    return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
	}
}