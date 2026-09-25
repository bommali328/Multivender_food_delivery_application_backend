package com.foodie.repository;

import com.foodie.model.AdminChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminChatMessageRepository extends JpaRepository<AdminChatMessage, Long> {
    List<AdminChatMessage> findByIdentifierOrderByTimestampAsc(String identifier);
    List<AdminChatMessage> findByPartnerMobileOrderByTimestampAsc(String partnerMobile);

}