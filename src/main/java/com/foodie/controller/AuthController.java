package com.foodie.controller;

import com.foodie.model.DeliveryPartner;
import com.foodie.model.Shop;
import com.foodie.model.User;
import com.foodie.repository.DeliveryPartnerRepository;
import com.foodie.repository.ShopRepository;
import com.foodie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private DeliveryPartnerRepository partnerRepository;

	// ✅ 100% Free WhatsApp PIN storage for forgot password
	private final Map<String, String> whatsappPinStorage = new HashMap<>();

	// 1. రిజిస్ట్రేషన్ ఏపీఐ (Register Partner / User / Shop)[cite: 5]
	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody AuthRequest request) {
		try {
			String mobile = request.getMobile();
			String role = request.getRole() != null ? request.getRole().toLowerCase() : "customer";

			if (mobile == null || mobile.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
			}

			if (role.equalsIgnoreCase("partner")) {
				DeliveryPartner existing = partnerRepository.findByMobile(mobile).orElse(null);
				if (existing != null) {
					return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already registered!"));
				}

				DeliveryPartner newPartner = new DeliveryPartner();
				newPartner.setFullName(request.getFullName() != null ? request.getFullName() : "Ichapuram Rider");
				newPartner.setMobile(mobile);
				newPartner.setPassword(request.getPassword());
				newPartner.setVehicleType(request.getVehicleType() != null ? request.getVehicleType() : "Motorcycle");
				newPartner.setBikeNumber(request.getBikeNumber() != null ? request.getBikeNumber() : "AP30BIKE0000");
				newPartner.setKycStatus("Pending");
				newPartner.setLatitude(18.5793);
				newPartner.setLongitude(84.4452);
				newPartner.setOnline(false);
				newPartner.setRainSurgeActive(false);
				newPartner.setWalletBalance(0.0);
				newPartner.setCashInHand(0.0);

				DeliveryPartner saved = partnerRepository.save(newPartner);
				return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Partner registered successfully", "data", saved));
			} 
			else if (role.equalsIgnoreCase("customer")) {
				User existing = userRepository.findFirstByMobile(mobile);
				if (existing != null) {
					return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already registered!"));
				}

				User newUser = new User();
				newUser.setName(request.getFullName() != null ? request.getFullName() : "Foodie Customer");
				newUser.setMobile(mobile);
				newUser.setPassword(request.getPassword());
				newUser.setRole("Customer");
				newUser.setActive(true);

				User saved = userRepository.save(newUser);
				return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Customer registered successfully", "data", saved));
			}
			else if (role.equalsIgnoreCase("shop")) {
				Shop existing = shopRepository.findByMobile(mobile);
				if (existing != null) {
					return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already registered!"));
				}

				Shop newShop = new Shop();
				newShop.setShopName(request.getFullName() != null ? request.getFullName() : "Ichapuram Shop");
				newShop.setMobile(mobile);
				newShop.setPassword(request.getPassword());
				
				String selectedCategory = request.getCategory() != null ? request.getCategory() : 
				                          (request.getBikeNumber() != null ? request.getBikeNumber() : "FOOD");
				newShop.setCategory(selectedCategory);
				
				if(request.getVehicleType() != null && !request.getVehicleType().isEmpty()) {
					newShop.setAddress(request.getVehicleType());
				}

				Shop saved = shopRepository.save(newShop);
				return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Shop registered successfully", "data", saved));
			}

			return ResponseEntity.badRequest().body(Map.of("error", "Invalid Role for Registration"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
		}
	}

	// 2. పాస్‌వర్డ్ లాగిన్ ఏపీఐ[cite: 5]
	@PostMapping("/login")
	public ResponseEntity<?> loginWithPassword(@RequestBody AuthRequest request) {
		try {
			String mobile = request.getMobile();
			String password = request.getPassword();
			String role = request.getRole() != null ? request.getRole().toLowerCase() : "customer";

			if (mobile == null || mobile.isEmpty() || password == null || password.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number and password are required"));
			}

			if (role.equalsIgnoreCase("customer")) {
				User user = userRepository.findFirstByMobile(mobile);
				if (user == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Mobile number not registered! Please register first."));
				}
				if (user.getPassword() != null && user.getPassword().trim().equals(password.trim())) {
					return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Login successful", "data", user));
				} else {
					return ResponseEntity.status(401).body(Map.of("error", "Incorrect password!"));
				}
			} 
			else if (role.equalsIgnoreCase("shop")) {
				Shop shop = shopRepository.findByMobile(mobile);
				if (shop == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Shop not registered! Please register first."));
				}
				if (shop.getPassword() != null && shop.getPassword().trim().equals(password.trim())) {
					return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Login successful", "data", shop));
				} else {
					return ResponseEntity.status(401).body(Map.of("error", "Incorrect password!"));
				}
			} 
			else if (role.equalsIgnoreCase("partner")) {
				DeliveryPartner partner = partnerRepository.findByMobile(mobile).orElse(null);
				if (partner == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Partner not registered! Please register first."));
				}
				if (partner.getPassword() != null && partner.getPassword().trim().equals(password.trim())) {
					return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Login successful", "data", partner));
				} else {
					return ResponseEntity.status(401).body(Map.of("error", "Incorrect password!"));
				}
			}

			return ResponseEntity.status(400).body(Map.of("error", "Invalid role"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
		}
	}

	// 3. ఫర్గాట్ పాస్‌వర్డ్ - వాట్సాప్‌కి 4 డిజిట్ పిన్ పంపడానికి (Free of cost)
	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody AuthRequest request) {
		try {
			String mobile = request.getMobile();
			String role = request.getRole() != null ? request.getRole().toLowerCase() : "customer";

			if (mobile == null || mobile.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
			}

			boolean exists = false;
			if (role.equalsIgnoreCase("customer")) {
				exists = (userRepository.findFirstByMobile(mobile) != null);
			} else if (role.equalsIgnoreCase("shop")) {
				exists = (shopRepository.findByMobile(mobile) != null);
			} else if (role.equalsIgnoreCase("partner")) {
				exists = (partnerRepository.findByMobile(mobile).isPresent());
			}

			if (!exists) {
				return ResponseEntity.status(404).body(Map.of("error", "Mobile number not registered!"));
			}

			// 4 అంకెల పిన్ జనరేట్ చేయడం (0000 - 9999)
			String pin = String.format("%04d", new Random().nextInt(10000));
			whatsappPinStorage.put(mobile, pin);

			// ఉచిత వాట్సాప్ లింక్ (Click to chat)
			String whatsappUrl = "https://wa.me/91" + mobile + "?text=Your%20Foodiee%20Password%20Reset%20PIN%20is:%20" + pin;

			return ResponseEntity.ok(Map.of(
				"status", "SUCCESS",
				"message", "4-digit PIN generated for WhatsApp successfully!",
				"whatsappRedirectUrl", whatsappUrl,
				"debugPin", pin // డెవలప్‌మెంట్ సమయంలో టెస్ట్ చేయడానికి
			));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
		}
	}

	// 4. పిన్ వెరిఫై చేసి కొత్త పాస్‌వర్డ్ సెట్ చేయడానికి (Reset Password)
	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody AuthRequest request) {
		try {
			String mobile = request.getMobile();
			String pin = request.getOtp(); // 4-digit PIN
			String newPassword = request.getNewPassword();
			String role = request.getRole() != null ? request.getRole().toLowerCase() : "customer";

			if (mobile == null || mobile.isEmpty() || pin == null || pin.isEmpty() || newPassword == null || newPassword.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile, PIN and new password are required"));
			}

			if (!whatsappPinStorage.containsKey(mobile) || !whatsappPinStorage.get(mobile).equals(pin)) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired 4-digit PIN!"));
			}

			if (newPassword.trim().length() < 4) {
				return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 4 characters long."));
			}

			if (role.equalsIgnoreCase("customer")) {
				User user = userRepository.findFirstByMobile(mobile);
				if (user == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Mobile number not registered!"));
				}
				user.setPassword(newPassword.trim());
				userRepository.save(user);
			} 
			else if (role.equalsIgnoreCase("shop")) {
				Shop shop = shopRepository.findByMobile(mobile);
				if (shop == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Shop not registered!"));
				}
				shop.setPassword(newPassword.trim());
				shopRepository.save(shop);
			} 
			else if (role.equalsIgnoreCase("partner")) {
				DeliveryPartner partner = partnerRepository.findByMobile(mobile).orElse(null);
				if (partner == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Partner not registered!"));
				}
				partner.setPassword(newPassword.trim());
				partnerRepository.save(partner);
			}

			// సక్సెస్ అయిన తర్వాత స్టోరేజ్ నుండి పిన్ తొలగించడం
			whatsappPinStorage.remove(mobile);

			return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password updated successfully via WhatsApp PIN!"));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
		}
	}

	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody AuthRequest request) {
		String mobile = request.getMobile();
		String role = request.getRole();

		if (mobile == null || mobile.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
		}

		if (role == null || role.isEmpty()) {
			role = "customer";
		}

		String generatedOtp = String.format("%04d", new Random().nextInt(10000));

		if (role.equalsIgnoreCase("customer")) {
			User user = userRepository.findFirstByMobile(mobile);
			if (user == null) {
				return ResponseEntity.status(404).body(Map.of("status", "NOT_REGISTERED", "error", "User not registered. Please register first."));
			}
			user.setOtp(generatedOtp);
			userRepository.save(user);

		} else if (role.equalsIgnoreCase("shop")) {
			Shop shop = shopRepository.findByMobile(mobile);
			if (shop == null) {
				return ResponseEntity.status(404).body(Map.of("status", "NOT_REGISTERED", "error", "Shop not registered. Please register first."));
			}
			shop.setOtp(generatedOtp);
			shopRepository.save(shop);

		} else if (role.equalsIgnoreCase("partner")) {
			DeliveryPartner partner = partnerRepository.findByMobile(mobile).orElse(null);
			if (partner == null) {
				return ResponseEntity.status(404).body(Map.of("status", "NOT_REGISTERED", "error", "Partner not registered. Please register first."));
			}
			partner.setOtp(generatedOtp);
			partnerRepository.save(partner);
		}

		return ResponseEntity.ok(new OtpResponse("OTP sent successfully", generatedOtp));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody AuthRequest request) {
		try {
			String mobile = request.getMobile();
			String otp = request.getOtp();
			String role = request.getRole();

			if (mobile == null || mobile.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
			}

			if (otp == null || otp.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "OTP is required"));
			}

			if (role == null || role.isEmpty()) {
				role = "customer";
			}

			if (role.equalsIgnoreCase("partner")) {
				DeliveryPartner partner = partnerRepository.findByMobile(mobile).orElse(null);
				if (partner == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Partner not found. Please register first."));
				}
				
				if (partner.getOtp() == null || !partner.getOtp().equals(otp)) {
					return ResponseEntity.status(400).body(Map.of("error", "Invalid OTP!"));
				}

				partner.setOtp(null);
				DeliveryPartner savedPartner = partnerRepository.save(partner);
				return ResponseEntity.ok(savedPartner);
			}
			else if (role.equalsIgnoreCase("customer")) {
				User user = userRepository.findFirstByMobile(mobile);
				if (user == null) {
					return ResponseEntity.status(404).body(Map.of("error", "User not found. Please register first."));
				}

				if (user.getOtp() == null || !user.getOtp().equals(otp)) {
					return ResponseEntity.status(400).body(Map.of("error", "Invalid OTP!"));
				}

				user.setOtp(null);
				User savedUser = userRepository.save(user);
				return ResponseEntity.ok(savedUser);

			} else if (role.equalsIgnoreCase("shop")) {
				Shop shop = shopRepository.findByMobile(mobile);
				if (shop == null) {
					return ResponseEntity.status(404).body(Map.of("error", "Shop not found. Please register first."));
				}

				if (shop.getOtp() == null || !shop.getOtp().equals(otp)) {
					return ResponseEntity.status(400).body(Map.of("error", "Invalid OTP!"));
				}

				shop.setOtp(null);
				Shop savedShop = shopRepository.save(shop);
				return ResponseEntity.ok(savedShop);
			}

			return ResponseEntity.status(401).body(Map.of("error", "Invalid Role"));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Internal Error: " + e.getMessage()));
		}
	}
}

class AuthRequest {
	private String mobile;
	private String otp;
	private String role;
	private String fullName;
	private String vehicleType;
	private String bikeNumber;
	private String password;
	private String newPassword; 
	private String category; 

	public String getMobile() { return mobile; }
	public void setMobile(String mobile) { this.mobile = mobile; }

	public String getOtp() { return otp; }
	public void setOtp(String otp) { this.otp = otp; }

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }

	public String getVehicleType() { return vehicleType; }
	public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

	public String getBikeNumber() { return bikeNumber; }
	public void setBikeNumber(String bikeNumber) { this.bikeNumber = bikeNumber; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public String getNewPassword() { 
		return newPassword != null ? newPassword : password; 
	}
	public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }
}

class OtpResponse {
	public String message;
	public String otp;

	public OtpResponse(String message, String otp) {
		this.message = message;
		this.otp = otp;
	}
}