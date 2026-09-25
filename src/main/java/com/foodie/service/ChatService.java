package com.foodie.service;

import com.foodie.model.ChatMessage;
import com.foodie.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    // 1. కొత్త మెసేజ్‌ను లేదా ఇమేజ్‌ను డేటాబేస్‌లో సేవ్ చేయడానికి
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now()); // ✅ LocalDateTime ఆబ్జెక్ట్‌గా సెట్ అవుతుంది
        }
        return chatRepository.save(chatMessage);
    }

    // 2. ఆర్డర్ ఐడీ ఆధారంగా చాట్ హిస్టరీని తెప్పించడానికి
    public List<ChatMessage> getChatHistoryByOrderId(String orderId) {
        try {
            return chatRepository.findByOrderIdOrderByTimestampAsc(orderId);
        } catch (Exception e) {
            return List.of();
        }
    }

    // 3. మొబైల్ నంబర్ ఆధారంగా చాట్ హిస్టరీని తెప్పించడానికి
    public List<ChatMessage> getChatHistoryByMobile(String mobile) {
        try {
            return chatRepository.findBySenderMobileOrderByTimestampAsc(mobile);
        } catch (Exception e) {
            return List.of();
        }
    }

    // 4. అడ్మిన్ ప్యానెల్ కోసం అన్ని చాట్ మెసేజ్‌లను పొందడానికి
    public List<ChatMessage> getAllChats() {
        return chatRepository.findAll();
    }
}