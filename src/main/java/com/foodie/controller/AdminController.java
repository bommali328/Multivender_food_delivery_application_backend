package com.foodie.controller;

import com.foodie.model.User;
import com.foodie.model.Order;
import com.foodie.model.Shop;
import com.foodie.model.PromoCode;
import com.foodie.model.AdminChatMessage;
import com.foodie.model.DeliveryPartner;
import com.foodie.repository.UserRepository;
import com.foodie.repository.OrderRepository;
import com.foodie.repository.ShopRepository;
import com.foodie.repository.PromoCodeRepository;
import com.foodie.repository.AdminChatMessageRepository;
import com.foodie.repository.DeliveryPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdminController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private PromoCodeRepository promoCodeRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private DeliveryPartnerRepository deliveryPartnerRepository;

	@Autowired
	private AdminChatMessageRepository adminChatMessageRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired(required = false)
	private com.google.firebase.messaging.FirebaseMessaging firebaseMessaging;
	
	// షాప్ వారీగా వేర్వేరు కమిషన్ రేట్లు స్టోర్ చేయడానికి
	private static final Map<Long, Double> shopCommissionMap = new HashMap<>();

	// 1. అడ్మిన్ ప్యానెల్ కోసం అన్ని కస్టమర్ల జాబితాను పంపడానికి
	@GetMapping("/admin/customers/all")
	public ResponseEntity<List<User>> getAllCustomers() {
		List<User> customers = userRepository.findAll();
		return ResponseEntity.ok(customers);
	}

	// 2. కస్టమర్‌ని డిలీట్ చేయడానికి
	@DeleteMapping("/admin/customer/{mobile}")
	public ResponseEntity<?> deleteCustomer(@PathVariable String mobile) {
		if (userRepository.existsById(mobile)) {
			userRepository.deleteById(mobile);
			return ResponseEntity.ok("Customer deleted successfully");
		}
		return ResponseEntity.status(404).body("Customer not found");
	}

	// 3. షాప్‌ని డిలీట్ చేయడానికి
	@DeleteMapping("/admin/shop/{id}")
	public ResponseEntity<?> deleteShop(@PathVariable Long id) {
		if (shopRepository.existsById(id)) {
			shopRepository.deleteById(id);
			return ResponseEntity.ok("Shop deleted successfully");
		}
		return ResponseEntity.status(404).body("Shop not found");
	}

	@PutMapping("/partner/status/update/{partnerId}")
	public ResponseEntity<?> updatePartnerStatus(@PathVariable Long partnerId, @RequestParam boolean isOnline) {
		DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId).orElse(null);
		if (partner != null) {
			partner.setOnline(isOnline);
			deliveryPartnerRepository.save(partner);
			return ResponseEntity.ok(Map.of("status", "success", "online", partner.isOnline()));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}

	// 🌟 అడ్మిన్ ప్యానెల్ నుండి నేరుగా కస్టమర్ వాలెట్ బ్యాలెన్స్ అప్‌డేట్ చేయడానికి
	@PutMapping("/admin/customer/wallet/{mobile}")
	public ResponseEntity<?> updateCustomerWallet(@PathVariable String mobile,
			@RequestBody Map<String, Double> payload) {
		Optional<User> userOpt = userRepository.findById(mobile);
		if (userOpt.isPresent()) {
			User user = userOpt.get();
			Double amountToAdd = payload.get("amount");
			if (amountToAdd != null) {
				double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
				user.setWalletBalance(currentBalance + amountToAdd);
				userRepository.save(user);
				return ResponseEntity.ok(Map.of("status", "success", "newBalance", user.getWalletBalance()));
			}
			return ResponseEntity.badRequest().body(Map.of("error", "Amount not specified"));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
	}

	// --- PROMO CODE LOGIC ---

	@PostMapping("/admin/promos/save")
	public ResponseEntity<PromoCode> savePromoCode(@RequestBody PromoCode promoCode) {
		promoCode.setActive(true);
		PromoCode savedPromo = promoCodeRepository.save(promoCode);
		return ResponseEntity.ok(savedPromo);
	}

	// కస్టమర్ యాప్ కోసం యాక్టివ్ ప్రొమో కోడ్స్ అన్నీ పంపడానికి
	@GetMapping("/promos/active")
	public ResponseEntity<List<PromoCode>> getActivePromos() {
		List<PromoCode> activePromos = promoCodeRepository.findByIsActive(true);
		return ResponseEntity.ok(activePromos);
	}

	// 6. అన్ని ఆర్డర్‌లను పొందడానికి
	@GetMapping("/admin/orders/all")
	public ResponseEntity<List<Order>> getAllOrdersForAdmin() {
		List<Order> orders = orderRepository.findAll();
		return ResponseEntity.ok(orders);
	}

	// 7. అన్ని డెలివరీ పార్టర్లను పొందడానికి
	@GetMapping("/admin/partners/all")
	public ResponseEntity<List<DeliveryPartner>> getAllPartnersForAdmin() {
		List<DeliveryPartner> partners = deliveryPartnerRepository.findAll();
		return ResponseEntity.ok(partners);
	}

	// 🚀 షాప్స్ అన్నీ అడ్మిన్ కోసం ఫెచ్ చేయడానికి
	@GetMapping("/admin/shop/all")
	public ResponseEntity<List<Shop>> getAllShopsForAdmin() {
		List<Shop> shops = shopRepository.findAll();
		return ResponseEntity.ok(shops);
	}

	// 🚀 అడ్మిన్ చాట్ కోసం అన్ని కేటగిరీల యూజర్లను ఫెచ్ చేసే ఎండ్‌పాయింట్
	@GetMapping("/admin/chat/users")
	public ResponseEntity<?> getUsersForAdminChat(@RequestParam("role") String role) {
		try {
			if ("customer".equalsIgnoreCase(role)) {
				return ResponseEntity.ok(userRepository.findAll());
			} else if ("partner".equalsIgnoreCase(role)) {
				return ResponseEntity.ok(deliveryPartnerRepository.findAll());
			} else if ("shop".equalsIgnoreCase(role)) {
				return ResponseEntity.ok(shopRepository.findAll());
			}
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping(value = "/admin/broadcast", consumes = { "multipart/form-data" })
	public ResponseEntity<?> sendBroadcast(@RequestParam("target") String target,
			@RequestParam("message") String message,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		try {
			String imageUrl = null;
			if (image != null && !image.isEmpty()) {
				imageUrl = "https://api.foodiee.shop/uploaded_images/" + image.getOriginalFilename();
			}

			Map<String, Object> payload = new HashMap<>();
			payload.put("message", message);
			payload.put("imageUrl", imageUrl);

			// 1. రియల్ టైమ్ వెబ్‌సాకెట్ అలర్ట్స్ కోసం
			if ("All Users".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/all", payload);
				messagingTemplate.convertAndSend("/topic/broadcast/shops", payload);
				messagingTemplate.convertAndSend("/topic/broadcast/partners", payload);
			} else if ("All Delivery Partners".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/partners", payload);
			} else if ("All Shop Owners".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/shops", payload);
			} else if ("All Customers".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/customers", payload);
				
				// 🌟 2. డేటాబేస్ నుండి కస్టమర్ల FCM Tokens సేకరించి మొబైల్‌కి Push Notification పంపడం
				List<User> customers = userRepository.findAll();
				for (User customer : customers) {
					if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty() && firebaseMessaging != null) {
						com.google.firebase.messaging.Message.Builder msgBuilder = com.google.firebase.messaging.Message.builder()
								.setToken(customer.getFcmToken())
								.setNotification(com.google.firebase.messaging.Notification.builder()
										.setTitle("📢 Foodiee Announcement")
										.setBody(message)
										.setImage(imageUrl)
										.build());
						
						try {
							firebaseMessaging.send(msgBuilder.build());
						} catch (Exception e) {
							System.err.println("FCM Push Error for " + customer.getMobile() + ": " + e.getMessage());
						}
					}
				}
			} else {
				messagingTemplate.convertAndSend("/topic/broadcast/all", payload);
			}

			Map<String, String> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Broadcast sent to " + target);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	// 🚀 ప్రొమో కోడ్ ఆన్/ఆఫ్ (Toggle) ఎండ్‌పాయింట్
	@PutMapping("/admin/promos/toggle/{idOrCode}")
	public ResponseEntity<?> togglePromoStatus(@PathVariable String idOrCode,
			@RequestBody Map<String, Boolean> request) {
		Optional<PromoCode> promoOpt = Optional.empty();

		try {
			Long id = Long.valueOf(idOrCode);
			promoOpt = promoCodeRepository.findById(id);
		} catch (NumberFormatException e) {
			promoOpt = promoCodeRepository.findByCode(idOrCode);
		}

		if (promoOpt.isPresent()) {
			PromoCode promo = promoOpt.get();
			Boolean isActive = request.get("isActive");
			if (isActive != null) {
				promo.setActive(isActive);
				promoCodeRepository.save(promo);
				return ResponseEntity.ok(Map.of("status", "success", "isActive", promo.isActive()));
			}
		}
		return ResponseEntity.status(404).body(Map.of("error", "Promo code not found"));
	}

	@PostMapping("/set")
	public ResponseEntity<?> setShopCommission(@RequestBody Map<String, Object> payload) {
		Long shopId = Long.valueOf(payload.get("shopId").toString());
		Double commission = Double.valueOf(payload.get("commissionRate").toString());

		shopCommissionMap.put(shopId, commission);
		return ResponseEntity.ok(Map.of("status", "success", "message",
				"Commission updated to " + commission + "% for Shop ID: " + shopId));
	}

	// ✅ WebSocket ద్వారా అడ్మిన్ లేదా షాప్/పార్ట్‌నర్ మెసేజ్ పంపే STOMP Mapping
	@org.springframework.messaging.handler.annotation.MessageMapping("/admin-partner/send")
	public void receiveAdminPartnerMessage(AdminChatMessage chatMessage) {
		try {
			String mob = chatMessage.getPartnerMobile() != null ? chatMessage.getPartnerMobile()
					: chatMessage.getIdentifier();
			if (mob != null) {
				String cleanMob = mob.replaceAll("^(\\+91|91)", "").trim();
				chatMessage.setPartnerMobile(cleanMob);
				chatMessage.setIdentifier(cleanMob);
			}

			AdminChatMessage savedMsg = adminChatMessageRepository.save(chatMessage);

			messagingTemplate.convertAndSend("/topic/chat/admin-partner/" + savedMsg.getIdentifier(), savedMsg);
			messagingTemplate.convertAndSend("/topic/admin/chats", savedMsg);

		} catch (Exception e) {
			System.err.println("WebSocket Admin Chat Error: " + e.getMessage());
		}
	}

	// ✅ డెలివరీ పార్టనర్ KYC వెరిఫై లేదా అప్డేట్ చేయడానికి ఎండ్‌పాయింట్
	@PutMapping("/admin/partner/kyc/verify/{partnerId}")
	public ResponseEntity<?> verifyPartnerKyc(@PathVariable Long partnerId, @RequestBody Map<String, String> payload) {
		Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(partnerId);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();
			String kycStatus = payload.get("kycStatus");
			if (kycStatus != null) {
				partner.setKycStatus(kycStatus);
				deliveryPartnerRepository.save(partner);
				return ResponseEntity.ok(Map.of("status", "success", "message", "Partner KYC updated to " + kycStatus,
						"kycStatus", partner.getKycStatus()));
			}
			return ResponseEntity.badRequest().body(Map.of("error", "KycStatus not specified in payload"));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Delivery partner not found"));
	}
	
	@PutMapping("/order/status/{orderId}")
	public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> payload) {
		Order order = orderRepository.findById(orderId).orElse(null);
		if (order != null) {
			String newStatus = payload.get("status");
			order.setStatus(newStatus);
			orderRepository.save(order);

			String customerMobile = order.getCustomerMobile();
			Optional<User> userOpt = userRepository.findById(customerMobile);
			
			if (userOpt.isPresent() && userOpt.get().getFcmToken() != null && firebaseMessaging != null) {
				String fcmToken = userOpt.get().getFcmToken();
				
				String title = "📦 Order Update: " + newStatus;
				String body = "Your order #" + orderId + " is now " + newStatus.toLowerCase() + ".";

				try {
					com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
							.setToken(fcmToken)
							.setNotification(com.google.firebase.messaging.Notification.builder()
									.setTitle(title)
									.setBody(body)
									.build())
							.putData("orderId", String.valueOf(orderId))
							.putData("status", newStatus)
							.build();

					firebaseMessaging.send(message);
				} catch (Exception e) {
					System.err.println("Order Push Notification Error: " + e.getMessage());
				}
			}

			return ResponseEntity.ok(Map.of("status", "success", "message", "Order status updated and notification sent"));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
	}

	// అన్ని షాప్‌ల కమిషన్ రేట్లు పొందడానికి
	@GetMapping("/all")
	public ResponseEntity<?> getAllCommissions() {
		return ResponseEntity.ok(shopCommissionMap);
	}

}