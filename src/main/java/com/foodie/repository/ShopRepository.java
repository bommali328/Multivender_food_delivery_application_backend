package com.foodie.repository;

import com.foodie.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    
    // మొబైల్ నెంబర్ ద్వారా షాప్ వెతకడానికి
    Shop findByMobile(String mobile);
}