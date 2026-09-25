package com.foodie.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "shops")
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shopName;
    private String ownerName;
    
    @Column(unique = true, nullable = false)
    private String mobile;
    
    private String password;
    private String address;
    private String category;
    private String latitude;
    private String longitude;
    
    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl; // మెయిన్ / ప్రైమరీ కవర్ ఇమేజ్
    
    @ElementCollection(fetch = FetchType.EAGER) // 👈 మళ్లీ EAGER పెట్టాలి
    @CollectionTable(name = "shop_additional_images", joinColumns = @JoinColumn(name = "shop_id"))
    @Column(columnDefinition = "LONGTEXT")
    private List<String> additionalImages;
    
    private String upiId;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    
    private String fssaiLicense;
    private String otp;
    
    private boolean isOpen = true;
    
    // అడ్మిన్ ప్యానెల్ కోసం యాక్టివ్ / డియాక్టివ్ స్టేటస్ ఫీల్డ్
    private boolean active = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getAdditionalImages() { return additionalImages; }
    public void setAdditionalImages(List<String> additionalImages) { this.additionalImages = additionalImages; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getFssaiLicense() { return fssaiLicense; }
    public void setFssaiLicense(String fssaiLicense) { this.fssaiLicense = fssaiLicense; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}