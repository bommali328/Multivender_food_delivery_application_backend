package com.foodie.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    @Column(unique = true)
    private String mobile;
    private String email;

    @Column(length = 500)
    private String fcmToken; // 👈 నోటిఫికేషన్స్ కోసం ఈ కొత్త ఫీల్డ్ యాడ్ చేయబడింది

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // 👇 fcmToken కోసమే కొత్తగా యాడ్ చేసిన Getters & Setters
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}