package com.foodie.service;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppNotificationService {

    // 1. ఆర్డర్ కన్ఫర్మ్ అయినప్పుడు డీటెయిల్స్ పంపడం
    public void sendOrderConfirmationToWhatsApp(String customerMobile, Long orderId, double amount) {
        try {
            String message = "🎉 *Order Confirmed!* (Order #" + orderId + ")\n\n" +
                             "Thank you for ordering with Foodiee! Your payment of ₹" + amount + " was successful.\n" +
                             "Our shop is preparing your food now. 🍲";
            
            triggerWhatsAppMessage(customerMobile, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. డెలివరీ పార్టనర్ ఆర్డర్ యాక్సెప్ట్ చేశాక OTP పంపడం
    public void sendOtpToWhatsApp(String customerMobile, String otp, Long orderId) {
        try {
            String message = "🔐 *Delivery Verification OTP*\n\n" +
                             "Your Delivery Partner has accepted Order #" + orderId + ".\n" +
                             "Your Secret Delivery OTP is: *_" + otp + "_*\n" +
                             "Please share this OTP with the delivery partner only after receiving your food.";
            
            triggerWhatsAppMessage(customerMobile, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ఫ్రీ వాట్సాప్ లింక్ / ట్రిగ్గర్ లాజిక్
    private void triggerWhatsAppMessage(String mobile, String message) {
        // ఇక్కడ మీరు Free WhatsApp API (ఉదాహరణకు Meta Cloud API Free Tier లేదా Baileys Node.js local bridge) ఇంటిగ్రేట్ చేయవచ్చు.
        // ప్రస్తుతానికి కన్సోల్ లాగ్ మరియు లింక్ జనరేషన్:
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = "https://api.whatsapp.com/send?phone=91" + mobile + "&text=" + encodedMessage;
        
        System.out.println("🚀 [FREE WHATSAPP TRIGGERED]: " + whatsappUrl);
        // (మీ సర్వర్‌లో Node.js బాట్ లేదా WhatsApp Cloud API ఉంటే ఇక్కడ HTTP POST కాల్ చేయవచ్చు)
    }
}