package com.foodie.controller;

import com.foodie.model.MenuItem;
import com.foodie.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    private MenuRepository menuRepository;

    // ⭐ కస్టమర్ యాప్ కోసం అన్ని మెనూ ఐటమ్స్ తెచ్చే ఎండ్‌పాయింట్
    @GetMapping("/all")
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        List<MenuItem> items = menuRepository.findAll();
        return ResponseEntity.ok(items);
    }

    // ⭐ నిర్దిష్టమైన షాప్‌కి కొత్త ఐటమ్ యాడ్ చేసే ఎండ్‌పాయింట్
    @PostMapping("/add/{shopId}")
    public ResponseEntity<?> addMenuItem(@PathVariable Long shopId, @RequestBody MenuItem item) {
        try {
            item.setShopId(shopId);
            if (item.getItemName() != null && item.getName() == null) {
                item.setName(item.getItemName());
            } else if (item.getName() != null && item.getItemName() == null) {
                item.setItemName(item.getName());
            }
            MenuItem saved = menuRepository.save(item);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ⭐ షాప్ ఐడీ ఆధారంగా మెనూ తెచ్చే ఎండ్‌పాయింట్
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<MenuItem>> getMenuByShop(@PathVariable Long shopId) {
        List<MenuItem> items = menuRepository.findByShopId(shopId);
        return ResponseEntity.ok(items);
    }

    // ⭐ మెనూ ఐటమ్ ధర, పేరు లేదా అవైలబిలిటీని ఎడిట్/అప్‌డేట్ చేసే ఎండ్‌పాయింట్
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem updatedItem) {
        Optional<MenuItem> itemOpt = menuRepository.findById(id);
        if (itemOpt.isPresent()) {
            MenuItem item = itemOpt.get();
            
            if (updatedItem.getName() != null) item.setName(updatedItem.getName());
            if (updatedItem.getItemName() != null) item.setItemName(updatedItem.getItemName());
            if (updatedItem.getPrice() != null) item.setPrice(updatedItem.getPrice());
            if (updatedItem.getCategory() != null) item.setCategory(updatedItem.getCategory());
            if (updatedItem.getDescription() != null) item.setDescription(updatedItem.getDescription());
            if (updatedItem.getImageUrl() != null) item.setImageUrl(updatedItem.getImageUrl());
            item.setAvailable(updatedItem.isAvailable());
            
            MenuItem saved = menuRepository.save(item);
            return ResponseEntity.ok(saved);
        }
        return ResponseEntity.status(404).body("Menu item not found");
    }

    // ⭐ [NEW - Admin Panel Support] అడ్మిన్ ప్యానెల్ నుండి మెనూ ఐటమ్ డిలీట్ చేయడానికి
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMenuItem(@PathVariable Long id) {
        if (menuRepository.existsById(id)) {
            menuRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Menu item deleted successfully"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Menu item not found"));
    }
}