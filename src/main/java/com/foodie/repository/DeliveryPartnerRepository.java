package com.foodie.repository;

import com.foodie.model.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    
    Optional<DeliveryPartner> findByMobile(String mobile);
    
    List<DeliveryPartner> findByIsOnline(boolean isOnline);

    // --- Smart Assignment కోసం కొత్త క్వెరీస్ ---

    // 1. ఆన్‌లైన్‌లో ఉండి, ప్రస్తుతం ఎలాంటి ఆర్డర్‌లోనూ బిజీగా లేని (Not Busy) పార్ట్‌నర్ల జాబితా కోసం
    List<DeliveryPartner> findByIsOnlineTrueAndIsBusyFalse();

    // 2. ఒక నిర్దిష్ట దుకాణం (Shop) చుట్టూ ఉన్న రేడియస్ లో ఆన్‌లైన్ & ఫ్రీగా ఉన్న పార్ట్‌నర్లను GPS కోఆర్డినేట్ల ద్వారా ఫిల్టర్ చేయడానికి (Haversine Formula)
    @Query("SELECT p FROM DeliveryPartner p WHERE p.isOnline = true AND p.isBusy = false AND " +
           "(6371 * acos(cos(radians(:shopLat)) * cos(radians(p.latitude)) * " +
           "cos(radians(p.longitude) - radians(:shopLng)) + " +
           "sin(radians(:shopLat)) * sin(radians(p.latitude)))) <= :radiusKm")
    List<DeliveryPartner> findAvailablePartnersNearby(@Param("shopLat") Double shopLat, 
                                                      @Param("shopLng") Double shopLng, 
                                                      @Param("radiusKm") double radiusKm);
}