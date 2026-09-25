package com.foodie.service;

import com.foodie.model.Payment;
import com.foodie.model.User;
import com.foodie.repository.PaymentRepository;
import com.foodie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    public Payment savePayment(Payment payment) {
        // 1. పేమెంట్ ట్రాన్సాక్షన్ డేటాబేస్‌లో సేవ్ చేయడం
        Payment savedPayment = paymentRepository.save(payment);

        // 2. 🌟 కస్టమర్ వాలెట్ బ్యాలెన్స్ డేటాబేస్‌లో అప్‌డేట్ చేయడం (అడ్మిన్ యాప్ సింక్ కోసం)
        if (payment.getCustomerMobile() != null && payment.getTotalAmount() > 0) {
            // మీ UserRepository లో ఉన్న మెథడ్ పేరు ఇక్కడ వాడాలి
            User customer = userRepository.findFirstByMobile(payment.getCustomerMobile());
            
            if (customer != null) {
                double currentBalance = customer.getWalletBalance() != null ? customer.getWalletBalance() : 0.0;
                customer.setWalletBalance(currentBalance + payment.getTotalAmount());
                userRepository.save(customer); // డేటాబేస్‌లో అప్‌డేట్ అవుతుంది
            }
        }

        return savedPayment;
    }

    public List<Payment> getPaymentsByShop(Long shopId) {
        return paymentRepository.findByShopId(shopId);
    }

    public List<Payment> getPaymentsByCustomer(String mobile) {
        return paymentRepository.findByCustomerMobile(mobile);
    }
}