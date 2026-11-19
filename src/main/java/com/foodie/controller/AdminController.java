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
	
	private static final Map<Long, Double> shopCommissionMap = new HashMap<>();
	
	private String adminOtpCode = "";
	private java.time.Instant adminOtpExpiryTime = null;
	private final String SECURE_ADMIN_MOBILE = "7893521159";

	@GetMapping("/admin/customers/all")
	public ResponseEntity<List<User>> getAllCustomers() {
		List<User> customers = userRepository.findAll();
		return ResponseEntity.ok(customers);
	}

	@DeleteMapping("/admin/customer/{mobile}")
	public ResponseEntity<?> deleteCustomer(@PathVariable String mobile) {
		if (userRepository.existsById(mobile)) {
			userRepository.deleteById(mobile);
			return ResponseEntity.ok("Customer deleted successfully");
		}
		return ResponseEntity.status(404).body("Customer not found");
	}

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

	@PostMapping("/admin/promos/save")
	public ResponseEntity<PromoCode> savePromoCode(@RequestBody PromoCode promoCode) {
		promoCode.setActive(true);
		PromoCode savedPromo = promoCodeRepository.save(promoCode);
		return ResponseEntity.ok(savedPromo);
	}

	@GetMapping("/promos/active")
	public ResponseEntity<List<PromoCode>> getActivePromos() {
		List<PromoCode> activePromos = promoCodeRepository.findByIsActive(true);
		return ResponseEntity.ok(activePromos);
	}

	@GetMapping("/admin/orders/all")
	public ResponseEntity<List<Order>> getAllOrdersForAdmin() {
		List<Order> orders = orderRepository.findAll();
		return ResponseEntity.ok(orders);
	}

	@GetMapping("/admin/partners/all")
	public ResponseEntity<List<DeliveryPartner>> getAllPartnersForAdmin() {
		List<DeliveryPartner> partners = deliveryPartnerRepository.findAll();
		return ResponseEntity.ok(partners);
	}

	@GetMapping("/admin/shop/all")
	public ResponseEntity<List<Shop>> getAllShopsForAdmin() {
		List<Shop> shops = shopRepository.findAll();
		return ResponseEntity.ok(shops);
	}

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

	@PostMapping("/admin/broadcast")
	public ResponseEntity<?> sendBroadcast(
			@RequestParam("target") String target,
			@RequestParam(value = "title", defaultValue = "📢 Foodiee Announcement") String title,
			@RequestParam("message") String message,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		try {
			String imageUrl = null;
			if (image != null && !image.isEmpty()) {
				imageUrl = "https://api.foodiee.shop/uploaded_images/" + image.getOriginalFilename();
			}

			Map<String, Object> payload = new HashMap<>();
			payload.put("title", title);
			payload.put("message", message);
			payload.put("imageUrl", imageUrl);

			boolean isAllUsers = "All Users".equalsIgnoreCase(target);
			boolean sendToCustomers = "All Customers".equalsIgnoreCase(target) || isAllUsers;
			boolean sendToShops = "All Shop Owners".equalsIgnoreCase(target) || "All Shops".equalsIgnoreCase(target) || isAllUsers;
			boolean sendToPartners = "All Delivery Partners".equalsIgnoreCase(target) || isAllUsers;

			if (firebaseMessaging == null) {
				System.err.println("❌ FirebaseMessaging bean is NULL! Check serviceAccountKey.json initialization.");
			}

			// 1. Customers కి WebSocket + FCM Push
			if (sendToCustomers) {
				messagingTemplate.convertAndSend("/topic/broadcast/customers", payload);
				messagingTemplate.convertAndSend("/topic/broadcast/all", payload);

				if (firebaseMessaging != null) {
					for (User customer : userRepository.findAll()) {
						sendFcmToToken(customer.getFcmToken(), "Customer (" + customer.getMobile() + ")", title, message, imageUrl);
					}
				}
			}

			// 2. Shops కి WebSocket + FCM Push
			if (sendToShops) {
				messagingTemplate.convertAndSend("/topic/broadcast/shops", payload);
				if (isAllUsers) {
					messagingTemplate.convertAndSend("/topic/broadcast/all", payload);
				}

				if (firebaseMessaging != null) {
					for (Shop shop : shopRepository.findAll()) {
						sendFcmToToken(shop.getFcmToken(), "Shop (" + shop.getId() + " - " + shop.getShopName() + ")", title, message, imageUrl);
					}
				}
			}

			// 3. Delivery Partners కి WebSocket
			if (sendToPartners) {
				messagingTemplate.convertAndSend("/topic/broadcast/partners", payload);
			}

			if (!sendToCustomers && !sendToShops && !sendToPartners) {
				messagingTemplate.convertAndSend("/topic/broadcast/all", payload);
			}

			return ResponseEntity.ok(Map.of("status", "success", "message", "Broadcast sent successfully"));

		} catch (Exception e) {
			System.err.println("Broadcast Error: " + e.getMessage());
			return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	private void sendFcmToToken(String token, String label, String title, String message, String imageUrl) {
		if (token == null || token.trim().isEmpty() || firebaseMessaging == null) {
			return;
		}
		try {
			com.google.firebase.messaging.Notification.Builder notifBuilder =
					com.google.firebase.messaging.Notification.builder()
							.setTitle(title)
							.setBody(message);
			if (imageUrl != null && !imageUrl.isBlank()) {
				notifBuilder.setImage(imageUrl);
			}

			com.google.firebase.messaging.Message fcmMsg =
					com.google.firebase.messaging.Message.builder()
							.setToken(token.trim())
							.setNotification(notifBuilder.build())
							.setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
									.setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
									.setNotification(com.google.firebase.messaging.AndroidNotification.builder()
											.setColor("#FC8019")
											.setChannelId("foodie_orders")
											.build())
									.build())
							.putData("image", imageUrl != null ? imageUrl : "")
							.putData("title", title)
							.putData("message", message)
							.build();

			String responseId = firebaseMessaging.send(fcmMsg);
			System.out.println("✅ FCM sent to " + label + " -> " + responseId);
		} catch (Exception e) {
			System.err.println("❌ FCM Push Error for " + label + ": " + e.getMessage());
		}
	}

	@PostMapping("/set")
	public ResponseEntity<?> setShopCommission(@RequestBody Map<String, Object> payload) {
		Long shopId = Long.valueOf(payload.get("shopId").toString());
		Double commission = Double.valueOf(payload.get("commissionRate").toString());

		shopCommissionMap.put(shopId, commission);
		return ResponseEntity.ok(Map.of("status", "success", "message",
				"Commission updated to " + commission + "% for Shop ID: " + shopId));
	}

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
	
	// ✅ కేవలం పాస్‌వర్డ్ ద్వారా మాత్రమే డైరెక్ట్ అడ్మిన్ లాగిన్ (WhatsApp / OTP అవసరం లేదు)
		@PostMapping("/admin/auth/login")
		public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> payload) {
		    String password = payload.get("password");
		    
		    // మీ అడ్మిన్ పాస్‌వర్డ్ ఇక్కడ సెట్ చేయబడింది
		    String SECURE_ADMIN_PASSWORD = "Foodiee@Admin2026";

		    if (password != null && password.trim().equals(SECURE_ADMIN_PASSWORD)) {
		        return ResponseEntity.ok(Map.of(
		            "status", "SUCCESS",
		            "message", "Admin login successful"
		        ));
		    }
		    
		    return ResponseEntity.status(401).body(Map.of(
		        "status", "FAILED",
		        "error", "తప్పు పాస్‌వర్డ్! దయచేసి సరైన పాస్‌వర్డ్ ఎంటర్ చేయండి."
		    ));
		}

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
			String newStatus = payload.get("status"); // 👈 షాప్ లేదా పార్ట్నర్ అప్‌డేట్ చేసిన లైవ్ స్టేటస్
			order.setStatus(newStatus);
			orderRepository.save(order);

			String customerMobile = order.getCustomerMobile();
			Optional<User> userOpt = userRepository.findById(customerMobile);
			
			if (userOpt.isPresent() && userOpt.get().getFcmToken() != null && firebaseMessaging != null) {
				String fcmToken = userOpt.get().getFcmToken();
				
				// 🌟 డైనమిక్ టైటిల్ మరియు లైవ్ స్టేటస్ మెసేజ్
				String title = "🔥 𝙁𝙤𝙤𝙙𝙞𝙚𝙚 • Order #" + orderId;
				String body = "📦 Your order status is now: " + newStatus + "! Tap to track 🛵";

				// స్టేటస్‌కి తగినట్లుగా ప్రత్యేకమైన ఎమోజీలు మరియు మెసేజ్‌లు
				if ("Preparing".equalsIgnoreCase(newStatus)) {
					body = "👨‍🍳 Your food is being freshly prepared by the kitchen!";
				} else if ("Picked Up".equalsIgnoreCase(newStatus)) {
					body = "🛵 Delivery partner has picked up your order and is on the way!";
				} else if ("Delivered".equalsIgnoreCase(newStatus)) {
					body = "🎉 Your order has been delivered successfully. Enjoy your meal! 🍔";
				} else if ("Out for Delivery".equalsIgnoreCase(newStatus)) {
					body = "🚀 Your order is out for delivery and arriving shortly!";
				}

				try {
					com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
							.setToken(fcmToken)
							.setNotification(com.google.firebase.messaging.Notification.builder()
									.setTitle(title)
									.setBody(body)
									.build())
							.setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
									.setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
									.setNotification(com.google.firebase.messaging.AndroidNotification.builder()
											.setColor("#FC8019") // 👈 నోటిఫికేషన్ యాసెంట్ కలర్ (ఆరెంజ్)
											.build())
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
	
	@GetMapping("/all")
	public ResponseEntity<?> getAllCommissions() {
		return ResponseEntity.ok(shopCommissionMap);
	}

}