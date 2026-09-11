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

	@PostMapping("/register")
	public ResponseEntity<?> registerPartner(@RequestBody DeliveryPartner partnerRequest) {
		try {
			if (partnerRequest.getMobile() == null || partnerRequest.getMobile().trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
			}

			// Check if mobile already exists
			DeliveryPartner existing = partnerRepository.findByMobile(partnerRequest.getMobile()).orElse(null);
			if (existing != null) {
				return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already registered!"));
			}

			DeliveryPartner newPartner = new DeliveryPartner();
			newPartner.setFullName(partnerRequest.getFullName() != null ? partnerRequest.getFullName() : "Ichapuram Rider");
			newPartner.setMobile(partnerRequest.getMobile());
			newPartner.setVehicleType(partnerRequest.getVehicleType() != null ? partnerRequest.getVehicleType() : "Motorcycle");
			newPartner.setBikeNumber(partnerRequest.getBikeNumber() != null ? partnerRequest.getBikeNumber() : "AP30BIKE0000");
			newPartner.setKycStatus("Pending");
			newPartner.setLatitude(18.5793);
			newPartner.setLongitude(84.4452);
			newPartner.setOnline(false);
			newPartner.setRainSurgeActive(false);
			newPartner.setWalletBalance(0.0);
			newPartner.setCashInHand(0.0);

			DeliveryPartner saved = partnerRepository.save(newPartner);
			return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Partner registered successfully", "data", saved));
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
				
				// 🔍 ఓటీపీ సరిపోలిందో లేదో చెక్ చేయడం
				if (partner.getOtp() == null || !partner.getOtp().equals(otp)) {
					return ResponseEntity.status(400).body(Map.of("error", "Invalid OTP!"));
				}

				partner.setOtp(null); // సక్సెస్ అయిన తర్వాత ఓటీపీ క్లియర్ చేయడం
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
}

class OtpResponse {
	public String message;
	public String otp;

	public OtpResponse(String message, String otp) {
		this.message = message;
		this.otp = otp;
	}
}