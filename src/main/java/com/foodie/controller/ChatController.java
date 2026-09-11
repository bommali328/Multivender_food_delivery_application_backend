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
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 1. REST Endpoint for frontend fetch calls (POST /api/chat/send)
    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessageRest(@RequestBody ChatMessage chatMessage) {
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);
        
        // Broadcast to specific order chat topic
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getOrderId(), savedMessage);
        
        // Broadcast to admin panel for unread counter / blinking dot
        messagingTemplate.convertAndSend("/topic/admin/chats", savedMessage);
        
        return ResponseEntity.ok(savedMessage);
    }

    // 2. WebSocket STOMP Mapping
    @MessageMapping("/send/{orderId}")
    public void receiveMessage(@DestinationVariable String orderId, ChatMessage chatMessage) {
        chatMessage.setOrderId(orderId);
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);
        
        messagingTemplate.convertAndSend("/topic/chat/" + orderId, savedMessage);
        messagingTemplate.convertAndSend("/topic/admin/chats", savedMessage);
    }

    // 3. Get Chat History by Order ID
    @GetMapping("/history/{orderId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String orderId) {
        List<ChatMessage> history = chatService.getChatHistoryByOrderId(orderId);
        return ResponseEntity.ok(history);
    }

    // 4. Get All Conversations for Admin Dashboard
    @GetMapping("/admin/conversations")
    public ResponseEntity<List<ChatMessage>> getAllChatsForAdmin() {
        List<ChatMessage> allMessages = chatService.getAllChats();
        return ResponseEntity.ok(allMessages);
    }
}