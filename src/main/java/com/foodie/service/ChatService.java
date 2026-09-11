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

    // 1. కొత్త మెసేజ్‌ను డేటాబేస్‌లో సేవ్ చేయడానికి
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        return chatRepository.save(chatMessage);
    }

    // 2. నిర్దిష్ట ఆర్డర్ ఐడీ ఆధారంగా చాట్ హిస్టరీని (టైమ్‌స్టాంప్ ప్రకారం క్రమపద్ధతిలో) తెప్పించడానికి
    public List<ChatMessage> getChatHistoryByOrderId(String orderId) {
        return chatRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    // 3. అడ్మిన్ ప్యానెల్ కోసం అన్ని చాట్ మెసేజ్‌లను పొందడానికి
    public List<ChatMessage> getAllChats() {
        return chatRepository.findAll();
    }
}