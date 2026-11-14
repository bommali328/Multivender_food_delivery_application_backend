package com.foodie.model;

import jakarta.persistence.*;

@Entity
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String discount;
    private String minOrder;
    private boolean isActive = true;
    
    // ✅ కొత్తగా యాడ్ చేయవలసిన ఫీల్డ్ (కూపన్ వాడేశారా లేదా అని చెక్ చేయడానికి)
    private boolean isUsed = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscount() { return discount; }
    public void setDiscount(String discount) { this.discount = discount; }

    public String getMinOrder() { return minOrder; }
    public void setMinOrder(String minOrder) { this.minOrder = minOrder; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // ✅ isUsed కోసం Getters & Setters
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }
}