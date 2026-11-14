package com.foodie.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity; 
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory; // 👈 టైమ్‌అవుట్ కోసం
import org.springframework.scheduling.annotation.Async; // 👈 Async కోసం ఇది అవసరం
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppNotificationService {

    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    // ⏱️ 2 సెకన్ల టైమ్‌అవుట్‌తో RestTemplate క్రియేట్ చేయడం
    private final RestTemplate restTemplate;

    public WhatsAppNotificationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 2 seconds
        factory.setReadTimeout(2000);    // 2 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    // 1. ఆర్డర్ కన్ఫర్మ్ అయినప్పుడు పూర్తి వివరాలతో మొదటి మెసేజ్ పంపడం
    public void sendOrderConfirmationToWhatsApp(String customerMobile, Long orderId, String shopName, String items, String deliveryAddress, double amount, String transactionId) {
        try {
            String message = "🎉 *Order Placed Successfully!*\n\n" +
                             "🆔 Order ID: #" + orderId + "\n" +
                             "🏪 Shop Name: " + (shopName != null ? shopName : "Partner Shop") + "\n" +
                             "🍔 Food Item: " + items + "\n" +
                             "📍 Delivery Address: " + (deliveryAddress != null ? deliveryAddress : "Ichapuram") + "\n" +
                             "⏱️ Estimated Time: ~20 Mins\n" +
                             "💳 Transaction Id: " + (transactionId != null ? transactionId : "N/A") + "\n" +
                             "💵 Total Amount: ₹" + amount + "\n\n" +
                             "Thank You! 🙏\n" +
                             "- Team Foodiee";
            
            triggerWhatsAppMessage(customerMobile, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. డెలివరీ OTP కోసం ప్రత్యేకంగా రెండవ మెసేజ్ పంపడం
    public void sendOtpToWhatsApp(String customerMobile, String otp, Long orderId) {
        try {
            String message = "🔑 *Secure Delivery OTP: " + otp + "*\n\n" +
                             "ఈ OTP ని ఫుడ్ డెలివరీ అయిన తర్వాత డెలివరీ బాయ్‌కి చెప్పండి. Thank you for ordering with Foodiee!";
            
            triggerWhatsAppMessage(customerMobile, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 3. అడ్మిన్ లాగిన్ కోసం WhatsApp OTP పంపే మెథడ్
    public void sendAdminOtpToWhatsApp(String adminMobile, String otp) {
        try {
            String message = "🔐 *Foodiee Secure Admin Portal*\n\n" +
                             "మీ అడ్మిన్ లాగిన్ వన్-టైమ్ పిన్ (OTP): *" + otp + "*\n\n" +
                             "ఈ OTP ని ఎవరితోనూ షేర్ చేయకండి. ఇది కేవలం మీ లాగిన్ కొరకే.";
            
            triggerWhatsAppMessage(adminMobile, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async // 🚀 ఇది బ్యాక్‌గ్రౌండ్‌లో రన్ అవుతుంది, ఆర్డర్ ప్రాసెస్‌ను ఆపదు
    private void triggerWhatsAppMessage(String mobile, String message) {
        try {
            if (mobile == null || mobile.trim().isEmpty()) {
                System.err.println("❌ WhatsApp Error: Mobile number is null or empty");
                return;
            }
            
            String cleanPhone = mobile.replaceAll("[^0-9]", "");
            String formattedPhone = cleanPhone.length() == 10 ? "91" + cleanPhone : cleanPhone;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("phone", formattedPhone);
            requestBody.put("message", message);

            HttpEntity < ?> requestEntity = new HttpEntity<>(requestBody, headers);

            System.out.println("🚀 [WHATSAPP API CALL] Sending to URL: " + whatsappApiUrl + " with Phone: " + formattedPhone);
            
            String response = restTemplate.postForObject(whatsappApiUrl, requestEntity, String.class);
            System.out.println("🚀 [WHATSAPP SENT SUCCESSFULLY] Response: " + response);
            
        } catch (Exception e) {
            System.err.println("❌ Error connecting to WhatsApp bot: " + e.getMessage());
        }
    }
}