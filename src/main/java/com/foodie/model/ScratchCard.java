package com.foodie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scratch_cards")
public class ScratchCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerMobile; // కస్టమర్ మొబైల్ నెంబర్
    private String orderId;        // ఏ ఆర్డర్ పై ఈ స్క్రాచ్ కార్డ్ వచ్చింది (Unique)
    private double amount;         // స్క్రాచ్ చేసినప్పుడు వచ్చే అమౌంట్ (ఉదా: ₹10, ₹22)
    private boolean isClaimed = false; // అమౌంట్ యాడ్ అయిందా లేదా
    private String unlockedAt;     // అన్‌లాక్ అయిన సమయం

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { isClaimed = claimed; }

    public String getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(String unlockedAt) { this.unlockedAt = unlockedAt; }
}