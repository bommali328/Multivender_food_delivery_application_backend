package com.foodie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_partners")
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    @Column(unique = true)
    private String mobile;
    private String email;
    private String password; // ⭐ పాస్‌వర్డ్ లాగిన్ కోసం కొత్తగా యాడ్ చేసిన ఫీల్డ్
    private String vehicleType;
    private String bikeNumber;
    private String aadhaarNo;
    private String licenseNo;
    private String panNo;
    private String bankAccount;
    private String ifscCode;
    private String upiId;
    private String kycStatus;

    private String otp;
    
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    
    private boolean isOnline = false;        
    private boolean isBusy = false; 
    private boolean rainSurgeActive = false;   
    private double walletBalance = 0.0;      
    private double cashInHand = 0.0;

    // --- Smart Assignment Fields ---
    private LocalDateTime lastOrderAcceptedTime; 
    private LocalDateTime onlineStartTime;     
    private double performanceRating = 5.0;    
    private double completionRate = 100.0;     

    // --- Document URLs ---
    private String aadhaarUrl;
    private String panUrl;
    private String licenseUrl;
    private String bikeUrl;
    private String driverPhotoUrl;

    // --- Password Getters and Setters ---
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // --- Getters and Setters for New Fields ---

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public LocalDateTime getLastOrderAcceptedTime() {
        return lastOrderAcceptedTime;
    }

    public void setLastOrderAcceptedTime(LocalDateTime lastOrderAcceptedTime) {
        this.lastOrderAcceptedTime = lastOrderAcceptedTime;
    }

    public LocalDateTime getOnlineStartTime() {
        return onlineStartTime;
    }

    public void setOnlineStartTime(LocalDateTime onlineStartTime) {
        this.onlineStartTime = onlineStartTime;
    }

    public double getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(double performanceRating) {
        this.performanceRating = performanceRating;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    // --- Existing Getters and Setters ---
    
    public String getAadhaarUrl() { return aadhaarUrl; }
    public void setAadhaarUrl(String aadhaarUrl) { this.aadhaarUrl = aadhaarUrl; }

    public String getPanUrl() { return panUrl; }
    public void setPanUrl(String panUrl) { this.panUrl = panUrl; }

    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }

    public String getBikeUrl() { return bikeUrl; }
    public void setBikeUrl(String bikeUrl) { this.bikeUrl = bikeUrl; }

    public String getDriverPhotoUrl() { return driverPhotoUrl; }
    public void setDriverPhotoUrl(String driverPhotoUrl) { this.driverPhotoUrl = driverPhotoUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getBikeNumber() { return bikeNumber; }
    public void setBikeNumber(String bikeNumber) { this.bikeNumber = bikeNumber; }

    public String getAadhaarNo() { return aadhaarNo; }
    public void setAadhaarNo(String aadhaarNo) { this.aadhaarNo = aadhaarNo; }

    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { this.isOnline = online; }

    public boolean isRainSurgeActive() { return rainSurgeActive; }
    public void setRainSurgeActive(boolean rainSurgeActive) { this.rainSurgeActive = rainSurgeActive; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public double getCashInHand() { return cashInHand; }
    public void setCashInHand(double cashInHand) { this.cashInHand = cashInHand; }
}