package com.foodie.service;

import com.foodie.model.Payment;
import com.foodie.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByShop(Long shopId) {
        return paymentRepository.findByShopId(shopId);
    }

    public List<Payment> getPaymentsByCustomer(String mobile) {
        return paymentRepository.findByCustomerMobile(mobile);
    }
}