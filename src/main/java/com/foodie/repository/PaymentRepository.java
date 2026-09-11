package com.foodie.repository;

import com.foodie.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByShopId(Long shopId);
    List<Payment> findByCustomerMobile(String customerMobile);
}