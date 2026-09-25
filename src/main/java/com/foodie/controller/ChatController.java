package com.foodie.controller;

import com.foodie.model.ChatMessage;
import com.foodie.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 1. REST Endpoint for frontend fetch calls (POST /api/chat/send)
    @PostMapping("/api/chat/send")
    public ResponseEntity<ChatMessage> sendMessageRest(@RequestBody ChatMessage chatMessage) {
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);
        
        // Target key: orderId or senderMobile
        String targetKey = chatMessage.getOrderId() != null ? chatMessage.getOrderId() : chatMessage.getSenderMobile();
        
        if (targetKey != null) {
            // Order base topic leda mobile topic ki broadcast cheyadam
            messagingTemplate.convertAndSend("/topic/chat/" + targetKey, savedMessage);
        }
        
        // Admin dashboard kosam broadcast
        messagingTemplate.convertAndSend("/topic/admin/chats", savedMessage);
        
        return ResponseEntity.ok(savedMessage);
    }

    // 2. WebSocket STOMP Mapping
    @MessageMapping("/send/{targetId}")
    public void receiveMessage(@DestinationVariable String targetId, ChatMessage chatMessage) {
        chatMessage.setOrderId(targetId);
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);
        
        messagingTemplate.convertAndSend("/topic/chat/" + targetId, savedMessage);
        messagingTemplate.convertAndSend("/topic/admin/chats", savedMessage);
    }

 // 3. Get Chat History by Order ID or Mobile (Only for customer/order chats)
    @GetMapping("/api/chat/history/{id}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String id) {
        List<ChatMessage> history = chatService.getChatHistoryByOrderId(id);
        if (history == null || history.isEmpty()) {
            history = chatService.getChatHistoryByMobile(id);
        }
        return ResponseEntity.ok(history);
    }
    
    // 4. Shop owner or Delivery partner ki order wise chat history kosam special endpoint
    @GetMapping("/api/chat/order/{orderId}")
    public ResponseEntity<List<ChatMessage>> getOrderChatHistory(@PathVariable String orderId) {
        List<ChatMessage> history = chatService.getChatHistoryByOrderId(orderId);
        return ResponseEntity.ok(history);
    }

    // 5. Get All Conversations for Admin Dashboard
    @GetMapping({"/api/chat/admin/conversations", "/api/admin-chat/conversations"})
    public ResponseEntity<List<ChatMessage>> getAllChatsForAdmin() {
        List<ChatMessage> allMessages = chatService.getAllChats();
        return ResponseEntity.ok(allMessages);
    }
}