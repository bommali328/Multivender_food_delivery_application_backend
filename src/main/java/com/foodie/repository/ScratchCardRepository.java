package com.foodie.repository;

import com.foodie.model.ScratchCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScratchCardRepository extends JpaRepository<ScratchCard, Long> {
    
    // ఒక నిర్దిష్ట కస్టమర్ కి సంబంధించిన అన్ని స్క్రాచ్ కార్డ్స్ తెచ్చుకోవడానికి
    List<ScratchCard> findByCustomerMobile(String customerMobile);

    // ఒక ఆర్డర్‌కి ఆల్రెడీ స్క్రాచ్ కార్డ్ వచ్చిందో లేదో చెక్ చేయడానికి
    boolean existsByOrderId(String orderId);
}