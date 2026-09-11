package com.foodie.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(unique = true, nullable = false)
    private String mobile; // మొబైల్ నంబర్‌ని ప్రైమరీ కీగా వాడటం

    private String name;
    private String role; // ఉదాహరణకు: "Customer", "ShopOwner", "DeliveryPartner"
    
    @Column(columnDefinition = "LONGTEXT") // ప్రొఫైల్ ఫోటో బేస్64 లేదా URL సేవ్ చేయడానికి
    private String profilePhoto;
    
    private String otp;

    // ⭐ కస్టమర్ యాప్ కోసం కొత్తగా యాడ్ చేసిన ఫీల్డ్స్:
    
    private String deliveryAddress; // యూజర్ సేవ్ చేసుకున్న చివరి అడ్రస్
    private Double latitude;        // మ్యాప్ కోసం లైవ్ లాటిట్యూడ్
    private Double longitude;       // మ్యాప్ కోసం లైవ్ లాంగిట్యూడ్
    
    private Boolean isActive = true; // యూజర్ స్టేటస్ (Active/Blocked)

    // --- Getters and Setters ---
    
    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}