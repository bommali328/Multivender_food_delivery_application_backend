package com.foodie.repository;

import com.foodie.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByOrderIdOrderByTimestampAsc(String orderId);
    List<ChatMessage> findBySenderMobileOrderByTimestampAsc(String senderMobile);
}