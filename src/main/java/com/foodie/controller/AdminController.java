package com.foodie.controller;

import com.foodie.model.User;
import com.foodie.model.Order;
import com.foodie.model.Shop;
import com.foodie.model.PromoCode;
import com.foodie.model.DeliveryPartner;
import com.foodie.repository.UserRepository;
import com.foodie.repository.OrderRepository;
import com.foodie.repository.ShopRepository;
import com.foodie.repository.PromoCodeRepository;
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
    private SimpMessagingTemplate messagingTemplate; 
    
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

    // --- PROMO CODE LOGIC ---

    @PostMapping("/admin/promos/save")
    public ResponseEntity<PromoCode> savePromoCode(@RequestBody PromoCode promoCode) {
        promoCode.setActive(true);
        PromoCode savedPromo = promoCodeRepository.save(promoCode);
        return ResponseEntity.ok(savedPromo);
    }

    // 🚀 కొత్తగా యాడ్ చేసిన ప్రొమో కోడ్ ఆన్/ఆఫ్ (Toggle) ఎండ్‌పాయింట్ (404 ఎర్రర్ రాకుండా ఉండటానికి)
    @PutMapping("/admin/promos/toggle/{id}")
    public ResponseEntity<?> togglePromoStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Optional<PromoCode> promoOpt = promoCodeRepository.findById(id);
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
    
    @PostMapping(value = "/admin/broadcast", consumes = {"multipart/form-data"})
    public ResponseEntity<?> sendBroadcast(
            @RequestParam("target") String target,
            @RequestParam("message") String message,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                imageUrl = "uploaded_images/" + image.getOriginalFilename();
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);
            payload.put("imageUrl", imageUrl);

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
    
    @PostMapping("/set")
    public ResponseEntity<?> setShopCommission(@RequestBody Map<String, Object> payload) {
        Long shopId = Long.valueOf(payload.get("shopId").toString());
        Double commission = Double.valueOf(payload.get("commissionRate").toString());
        
        shopCommissionMap.put(shopId, commission);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Commission updated to " + commission + "% for Shop ID: " + shopId));
    }

    // అన్ని షాప్‌ల కమిషన్ రేట్లు పొందడానికి
    @GetMapping("/all")
    public ResponseEntity<?> getAllCommissions() {
        return ResponseEntity.ok(shopCommissionMap);
    }
}