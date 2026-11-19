package com.foodie.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/promos")
@CrossOrigin(origins = "*")
public class PromoController {

    @PostMapping("/apply")
    public ResponseEntity<?> applyPromo(@RequestBody PromoApplyRequest request) {
        String code = request.getCode();
        String mobile = request.getMobile();

        // ఇక్కడ కూపన్ వాలిడేషన్ లాజిక్ రాయండి (ఉదాహరణకు FEST అయితే ₹25 డిస్కౌంట్)
        if ("FEST".equalsIgnoreCase(code) || "FIRST20".equalsIgnoreCase(code)) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "discount", 25.0,
                "message", "కూపన్ విజయవంతంగా అప్లై అయింది!"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "సరి కాని కూపన్ కోడ్ (Invalid Coupon Code)"
            ));
        }
    }
}

// DTO Class
class PromoApplyRequest {
    private String code;
    private String mobile;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}