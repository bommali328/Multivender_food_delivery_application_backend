package com.foodie.repository;

import com.foodie.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByMobile(String mobile);
    
    // ఒకవేళ కంట్రోలర్‌లో findFirstByMobile వాడితే ఇది కూడా యాడ్ చేయండి లేదా findByMobile వాడండి
    Customer findFirstByMobile(String mobile);
}