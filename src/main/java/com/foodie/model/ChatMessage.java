package com.foodie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_chats")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;          // ఏ ఆర్డర్ కోసం ఈ చాట్ (ఉదా: #ORD-11)
    private String senderMobile;     // మెసేజ్ పంపిన వారి మొబైల్ నంబర్
    private String senderName;       // పంపిన వారి పేరు (Customer / Shop / Partner)
    
    @Column(columnDefinition = "LONGTEXT") // ✅ ఇమేజ్ డేటా కోసం ఇది చాలా ముఖ్యం
    private String message;        // చాట్ మెసేజ్
    
    private String senderType;       // "customer", "shop", లేదా "partner"
    private String recipientRole;    // ఎవరికి వెళ్ళాలి: "shop", "partner", లేదా "customer"
    
    private boolean isRead = false; 
    
    
    // అన్-రీడ్ నోటిఫికేషన్ కౌంట్ కోసం
    private LocalDateTime timestamp = LocalDateTime.now();

    // --- Getters and Setters ---
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSenderMobile() { return senderMobile; }
    public void setSenderMobile(String senderMobile) { this.senderMobile = senderMobile; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}