package com.foodie.repository;

import com.foodie.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // కస్టమర్ మొబైల్ నంబర్ ఆధారంగా ఆర్డర్స్ తెచ్చుకోవడానికి
    List<Order> findByCustomerMobile(String customerMobile);
    
    // కస్టమర్ మొబైల్ నంబర్ ఆధారంగా లేటెస్ట్ ఆర్డర్స్ టాప్‌లో వచ్చేలా
    List<Order> findByCustomerMobileOrderByIdDesc(String customerMobile);

    // 🏪 షాప్ ఐడీ ఆధారంగా ఆర్డర్స్ తెచ్చుకోవడానికి
    List<Order> findByShopIdOrderByIdDesc(Long shopId);

    // 🛵 డెలివరీ పార్టనర్ ఐడీ ఆధారంగా ఆర్డర్ హిస్టరీ తెచ్చుకోవడానికి (మోడల్ క్లాస్‌లోని ఫీల్డ్ పేరు ప్రకారం)
    List<Order> findByDeliveryPartnerId(Long deliveryPartnerId);
    
    // ==========================================
    // ✅ NEW METHODS FOR PROMO CODE & FIRST ORDER
    // ==========================================

    // 1. కస్టమర్ మొబైల్ నంబర్ ఆధారంగా టోటల్ ఆర్డర్స్ కౌంట్ చేయడానికి (First Order Free చెక్ కోసం)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerMobile = :mobile")
    long countByCustomerMobile(@Param("mobile") String mobile);

    // 2. ఒక కస్టమర్ నిర్దిష్టమైన ప్రొమో కోడ్‌ని ఇదివరకే వాడేశారా లేదా అని చెక్ చేయడానికి (One-time use validation)
    boolean existsByCustomerMobileAndPromoCode(String customerMobile, String promoCode);
    
}