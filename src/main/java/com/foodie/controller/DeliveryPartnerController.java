package com.foodie.controller;

import com.foodie.model.DeliveryPartner;
import com.foodie.repository.DeliveryPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DeliveryPartnerController {

	@Autowired
	private DeliveryPartnerRepository deliveryPartnerRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	@Autowired
	private com.foodie.service.DeliveryPartnerService deliveryPartnerService;

	private final String UPLOAD_DIR = "uploads/kyc/";

	// 7. అడ్మిన్ ప్యానెల్ కోసం: పార్టనర్ KYC ని వెరిఫై లేదా రిజెక్ట్ చేయడానికి
	@PutMapping("/admin/partners/verify-kyc/{id}")
	public ResponseEntity<?> adminVerifyKyc(@PathVariable Long id, @RequestParam String status) {
		Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(id);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();
			partner.setKycStatus(status);
			deliveryPartnerRepository.save(partner);
			return ResponseEntity.ok(Map.of("status", "success", "message", "KYC status updated to: " + status));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}

	// ==========================================
	// 🛵 PARTNER APP CORE ENDPOINTS
	// ==========================================

	// 1. డెలివరీ పార్టనర్ లొకేషన్ అప్‌డేట్ చేయడానికి మరియు అడ్మిన్ ఫ్లీట్ మ్యాప్‌కి
	// / కస్టమర్‌కి బ్రాడ్‌కాస్ట్ చేయడానికి
	@PostMapping("/partner/location/update")
	public ResponseEntity<?> updateLocation(@RequestBody Map<String, Object> locationData) {
		try {
			Object partnerIdObj = locationData.get("partnerId");
			Object orderIdObj = locationData.get("orderId");
			Object latObj = locationData.get("lat");
			Object lngObj = locationData.get("lng");

			Long partnerId = partnerIdObj != null ? Long.valueOf(partnerIdObj.toString()) : null;
			Long orderId = orderIdObj != null ? Long.valueOf(orderIdObj.toString()) : null;
			Double lat = latObj != null ? Double.valueOf(latObj.toString()) : null;
			Double lng = lngObj != null ? Double.valueOf(lngObj.toString()) : null;

			if (partnerId != null) {
				Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(partnerId);
				if (partnerOpt.isPresent()) {
					DeliveryPartner partner = partnerOpt.get();
					if (lat != null)
						partner.setLatitude(lat);
					if (lng != null)
						partner.setLongitude(lng);
					deliveryPartnerRepository.save(partner);
				}
			}

			// ఒకవేళ ఆర్డర్ ఐడీ ఉంటే కస్టమర్‌కి లొకేషన్ వెళుతుంది
			if (orderId != null && lat != null && lng != null) {
				messagingTemplate.convertAndSend("/topic/location/" + orderId, (Object) locationData);
			}

			// 🚀 అడ్మిన్ ఫ్లీట్ ట్రాకింగ్ మ్యాప్ కోసం లైవ్ లొకేషన్ బ్రాడ్‌కాస్ట్ చేయడం
			if (lat != null && lng != null) {
				messagingTemplate.convertAndSend("/topic/fleet/tracking", (Object) locationData);
			}

			return ResponseEntity.ok(Map.of("status", "success", "message", "Location updated successfully"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	// 2. పార్టనర్ ప్రొఫైల్ మరియు డాక్యుమెంట్స్ (KYC) ఫైల్స్ తో సహా అప్‌డేట్
	// చేయడానికి
	@PutMapping("/partner/update-with-docs/{id}")
	public ResponseEntity<?> updatePartnerWithDocs(@PathVariable Long id,
			@RequestParam(value = "fullName", required = false) String fullName,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "bikeNumber", required = false) String bikeNumber,
			@RequestParam(value = "aadhaarNo", required = false) String aadhaarNo,
			@RequestParam(value = "panNo", required = false) String panNo,
			@RequestParam(value = "licenseNo", required = false) String licenseNo,
			@RequestParam(value = "bankAccount", required = false) String bankAccount,
			@RequestParam(value = "ifscCode", required = false) String ifscCode,
			@RequestParam(value = "upiId", required = false) String upiId,
			@RequestParam(value = "aadhaarFile", required = false) MultipartFile aadhaarFile,
			@RequestParam(value = "panFile", required = false) MultipartFile panFile,
			@RequestParam(value = "licenseFile", required = false) MultipartFile licenseFile,
			@RequestParam(value = "bikeFile", required = false) MultipartFile bikeFile,
			@RequestParam(value = "driverPhotoFile", required = false) MultipartFile driverPhotoFile) {
		try {
			Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(id);
			if (partnerOpt.isEmpty()) {
				return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
			}

			DeliveryPartner partner = partnerOpt.get();

			if (fullName != null)
				partner.setFullName(fullName);
			if (email != null)
				partner.setEmail(email);
			if (bikeNumber != null)
				partner.setBikeNumber(bikeNumber);
			if (aadhaarNo != null)
				partner.setAadhaarNo(aadhaarNo);
			if (panNo != null)
				partner.setPanNo(panNo);
			if (licenseNo != null)
				partner.setLicenseNo(licenseNo);
			if (bankAccount != null)
				partner.setBankAccount(bankAccount);
			if (ifscCode != null)
				partner.setIfscCode(ifscCode);
			if (upiId != null)
				partner.setUpiId(upiId);

			File uploadDir = new File(UPLOAD_DIR);
			if (!uploadDir.exists()) {
				uploadDir.mkdirs();
			}

			if (aadhaarFile != null && !aadhaarFile.isEmpty()) {
				partner.setAadhaarUrl(saveFile(aadhaarFile));
			}
			if (panFile != null && !panFile.isEmpty()) {
				partner.setPanUrl(saveFile(panFile));
			}
			if (licenseFile != null && !licenseFile.isEmpty()) {
				partner.setLicenseUrl(saveFile(licenseFile));
			}
			if (bikeFile != null && !bikeFile.isEmpty()) {
				partner.setBikeUrl(saveFile(bikeFile));
			}
			if (driverPhotoFile != null && !driverPhotoFile.isEmpty()) {
				partner.setDriverPhotoUrl(saveFile(driverPhotoFile));
			}

			partner.setKycStatus("Under Review");
			DeliveryPartner saved = deliveryPartnerRepository.save(partner);
			return ResponseEntity.ok(saved);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	private String saveFile(MultipartFile file) {
		try {
			String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
			String fileName = System.currentTimeMillis() + "_" + originalName.replaceAll("\\s+", "_");
			Path path = Paths.get(UPLOAD_DIR + fileName);

			if (!Files.exists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}

			Files.write(path, file.getBytes());
			return "/uploads/kyc/" + fileName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@GetMapping("/partner/files/{fileName}")
	public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
		try {
			Path path = Paths.get(UPLOAD_DIR + fileName);
			Resource resource = new UrlResource(path.toUri());

			if (resource.exists() || resource.isReadable()) {
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
						.body(resource);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@PutMapping("/partner/shift/{id}")
	public ResponseEntity<?> toggleShiftStatus(@PathVariable Long id, @RequestParam boolean online) {
		Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(id);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();
			partner.setOnline(online);
			deliveryPartnerRepository.save(partner);

			return ResponseEntity
					.ok(Map.of("isOnline", partner.isOnline(), "message", online ? "Shift started!" : "Shift paused."));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}

	@PutMapping("/partner/surge/{id}")
	public ResponseEntity<?> toggleRainSurge(@PathVariable Long id, @RequestParam boolean surgeActive) {
		Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(id);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();
			return ResponseEntity.ok(Map.of("rainSurgeActive", surgeActive, "message", "Surge updated"));
		}
		return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}

	@PostMapping("/partner/withdraw/{id}")
	public ResponseEntity<?> withdrawEarnings(@PathVariable Long id, @RequestParam double amount) {
		Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(id);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();

			Double walletObj = partner.getWalletBalance();
			double currentWallet = walletObj != null ? walletObj : 0.0;

			if (currentWallet >= amount) {
				partner.setWalletBalance(currentWallet - amount);
				deliveryPartnerRepository.save(partner);
				return ResponseEntity.ok(Map.of("status", "SUCCESS", "remainingBalance", partner.getWalletBalance(),
						"message", "Amount transferred successfully!"));
			} else {
				return ResponseEntity.badRequest().body(Map.of("error", "Insufficient wallet balance"));
			}
		}
		return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}
	
	@PutMapping("/status/update/{partnerId}")
	public ResponseEntity<?> updatePartnerOnlineStatus(@PathVariable Long partnerId, @RequestParam boolean isOnline) {
	    Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findById(partnerId);
	    if (partnerOpt.isPresent()) {
	        DeliveryPartner partner = partnerOpt.get();
	        partner.setOnline(isOnline); // లేదా setIsOnline(isOnline)
	        deliveryPartnerRepository.save(partner);
	        return ResponseEntity.ok(Map.of("status", "success", "isOnline", isOnline));
	    }
	    return ResponseEntity.status(404).body(Map.of("error", "Partner not found"));
	}
	
	@GetMapping("/partner/find-best")
	public ResponseEntity<?> findBestPartner(@RequestParam Double shopLat, @RequestParam Double shopLng) {
		try {
			DeliveryPartner bestPartner = deliveryPartnerService.findBestPartnerForOrder(shopLat, shopLng);
			if (bestPartner != null) {
				return ResponseEntity.ok(Map.of(
					"status", "success", 
					"partnerId", bestPartner.getId(), 
					"fullName", bestPartner.getFullName(),
					"mobile", bestPartner.getMobile()
				));
			} else {
				return ResponseEntity.status(404).body(Map.of("status", "error", "message", "No available delivery partners nearby"));
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	@PostMapping("/broadcast")
	public ResponseEntity<?> sendBroadcast(@RequestBody Map<String, String> broadcastPayload) {
		try {
			String target = broadcastPayload.get("target");
			String message = broadcastPayload.get("message");

			if ("All Users".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/all", message);
			} else if ("All Delivery Partners".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/partners", message);
			} else if ("All Customers".equalsIgnoreCase(target)) {
				messagingTemplate.convertAndSend("/topic/broadcast/customers", message);
			} else {
				messagingTemplate.convertAndSend("/topic/broadcast/all", message);
			}

			return ResponseEntity.ok(Map.of("status", "success", "message", "Broadcast sent to " + target));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}
}