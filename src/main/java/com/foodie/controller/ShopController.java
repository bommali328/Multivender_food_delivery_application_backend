package com.foodie.controller;

import com.foodie.model.Shop;
import com.foodie.repository.ShopRepository;
import com.foodie.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "*")
public class ShopController {

    @Autowired
    private ShopRepository shopRepository;
    
    @Autowired
    private ShopService shopService;

    private static final String UPLOAD_DIR = "uploads/";

    // 1. కొత్త షాప్ రిజిస్ట్రేషన్ (GPS కోఆర్డినేట్స్ & అడ్రస్‌తో సహా)
 // 1. కొత్త షాప్ రిజిస్ట్రేషన్ (GPS కోఆర్డినేట్స్ & అడ్రస్‌తో సహా)
    @PostMapping("/register")
    public ResponseEntity<?> registerShop(@RequestBody Shop shop, @RequestParam(required = false) Long ownerId) {
        try {
            if (shop.getMobile() == null || shop.getMobile().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Mobile number is required");
            }

            // మొబైల్ నంబర్ ఇప్పటికే రిజిస్టర్ అయిందా లేదా చెక్ చేయడం
            Shop existingShop = shopRepository.findByMobile(shop.getMobile());
            if (existingShop != null) {
                return ResponseEntity.badRequest().body("ఈ మొబైల్ నంబర్తో ఇప్పటికే షాప్ రిజిస్టర్ చేయబడింది.");
            }
            
            // ఒకవేళ category నల్ గా వస్తే బై డిఫాల్ట్ 'FOOD' సెట్ చేయడం
            if (shop.getCategory() == null || shop.getCategory().trim().isEmpty()) {
                shop.setCategory("FOOD");
            }

            Shop savedShop = shopService.registerShop(shop);
            return ResponseEntity.ok(savedShop);
        } catch (Exception e) {
            e.printStackTrace(); // ఇంటెలిజెడబ్ల్యూ కన్సోల్‌లో అసలు ఎర్రర్ ఏంటో ప్రింట్ అవుతుంది
            return ResponseEntity.status(500).body("Registration Error: " + e.getMessage());
        }
    }

    // 2. అన్ని షాప్స్‌ను కస్టమర్ యాప్‌కి పంపడానికి
    @GetMapping("/all")
    public ResponseEntity<List<Shop>> getAllShops() {
        List<Shop> shops = shopRepository.findAll();
        return ResponseEntity.ok(shops);
    }

    // 3. షాప్ ఐడీ ద్వారా ప్రొఫైల్ వివరాలు పొందడానికి
    @GetMapping("/{id}")
    public ResponseEntity<?> getShopProfile(@PathVariable Long id) {
        Optional<Shop> shop = shopService.getShopById(id);
        if (shop.isPresent()) {
            return ResponseEntity.ok(shop.get());
        }
        return ResponseEntity.status(404).body("Shop not found");
    }

    // 4. బ్యాంక్ & UPI వివరాలు అప్‌డేట్ చేయడానికి
    @PutMapping("/bank-details/{id}")
    public ResponseEntity<?> updateBankDetails(@PathVariable Long id, @RequestBody Shop bankDetails) {
        Shop updated = shopService.updateBankDetails(id, bankDetails);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.status(404).body("Shop not found");
    }

    // 5. షాప్ ప్రొఫైల్ ఎడిట్ / అప్‌డేట్ చేయడానికి (Name, Mobile, Address, FSSAI మొదలైనవి)
    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateShopProfile(@PathVariable Long id, @RequestBody Shop updatedDetails) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isPresent()) {
            Shop shop = shopOpt.get();
            
            if (updatedDetails.getShopName() != null) shop.setShopName(updatedDetails.getShopName());
            if (updatedDetails.getOwnerName() != null) shop.setOwnerName(updatedDetails.getOwnerName());
            if (updatedDetails.getMobile() != null) shop.setMobile(updatedDetails.getMobile());
            if (updatedDetails.getCategory() != null) shop.setCategory(updatedDetails.getCategory());
            if (updatedDetails.getAddress() != null) shop.setAddress(updatedDetails.getAddress());
            if (updatedDetails.getFssaiLicense() != null) shop.setFssaiLicense(updatedDetails.getFssaiLicense());
            if (updatedDetails.getImageUrl() != null) shop.setImageUrl(updatedDetails.getImageUrl());
            
            Shop savedShop = shopRepository.save(shop);
            return ResponseEntity.ok(savedShop);
        }
        return ResponseEntity.status(404).body("Shop not found");
    }

 // 6. షాప్ మెయిన్ కవర్ ఫోటో మరియు మల్టిపుల్ గ్యాలరీ ఫోటోలను అప్‌లోడ్ / అప్‌డేట్ చేయడానికి
    @PostMapping("/update-images/{id}")
    public ResponseEntity<?> updateShopImages(
            @PathVariable Long id,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "galleryImages", required = false) List<MultipartFile> galleryImages) {
        
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (!shopOpt.isPresent()) {
            return ResponseEntity.status(404).body("Shop not found");
        }

        Shop shop = shopOpt.get();

        try {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // మెయిన్ కవర్ ఇమేజ్ సేవ్ చేయడం
            if (mainImage != null && !mainImage.isEmpty()) {
                String mainFileName = System.currentTimeMillis() + "_" + mainImage.getOriginalFilename();
                Path mainPath = Paths.get(UPLOAD_DIR + mainFileName);
                Files.write(mainPath, mainImage.getBytes());
                // 🛠️ పోర్ట్ 8080 బదులుగా 5080 కి మార్చబడింది
                shop.setImageUrl("http://localhost:5080/uploads/" + mainFileName);
            }

            // మల్టిపుల్ గ్యాలరీ ఫోటోలను సేవ్ చేయడం
            if (galleryImages != null && !galleryImages.isEmpty()) {
                List<String> imageUrls = shop.getAdditionalImages();
                if (imageUrls == null) {
                    imageUrls = new ArrayList<>();
                }

                for (MultipartFile file : galleryImages) {
                    if (!file.isEmpty()) {
                        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        Path path = Paths.get(UPLOAD_DIR + fileName);
                        Files.write(path, file.getBytes());
                        // 🛠️ పోర్ట్ 8080 బదులుగా 5080 కి మార్చబడింది
                        imageUrls.add("http://localhost:5080/uploads/" + fileName);
                    }
                }
                shop.setAdditionalImages(imageUrls);
            }

            Shop savedShop = shopRepository.save(shop);
            return ResponseEntity.ok(savedShop);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error uploading images: " + e.getMessage());
        }
    }
    
    // ==========================================
    // 🛡️ ADMIN PANEL ENDPOINTS
    // ==========================================

    // 7. అడ్మిన్ ప్యానెల్ కోసం: షాప్ యాక్టివ్ / డియాక్టివ్ స్టేటస్ మార్చడానికి
    @PutMapping("/admin/status/{id}")
    public ResponseEntity<?> adminToggleShopStatus(@PathVariable Long id, @RequestParam boolean active) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isPresent()) {
            Shop shop = shopOpt.get();
            shop.setActive(active);
            shopRepository.save(shop);
            return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Shop active status updated to: " + active));
        }
        return ResponseEntity.status(404).body("Shop not found");
    }
}