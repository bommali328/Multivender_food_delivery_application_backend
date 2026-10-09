package com.foodie.service;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppNotificationService {

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

    // ఫ్రీ వాట్సాప్ లింక్ / ట్రిగ్గర్ లాజిక్
    private void triggerWhatsAppMessage(String mobile, String message) {
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = "https://api.whatsapp.com/send?phone=91" + mobile + "&text=" + encodedMessage;
        
        System.out.println("🚀 [FREE WHATSAPP TRIGGERED]: " + whatsappUrl);
        // (మీ సర్వర్‌లో Node.js బాట్ లేదా WhatsApp Cloud API ఉంటే ఇక్కడ HTTP POST కాల్ చేయవచ్చు)
    }
}