package com.foodie.controller;

import com.foodie.model.AdminChatMessage;
import com.foodie.repository.AdminChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin-chat")
@CrossOrigin(origins = "*")
public class AdminChatController {

    @Autowired
    private AdminChatMessageRepository adminChatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/history/{identifier}")
    public ResponseEntity<List<AdminChatMessage>> getChatHistory(@PathVariable String identifier) {
        try {
            String cleanId = identifier != null ? identifier.replaceAll("^(\\+91|91)", "").trim() : "";
            List<AdminChatMessage> messages = null;
            if (!cleanId.isEmpty()) {
                messages = adminChatMessageRepository.findByIdentifierOrderByTimestampAsc(cleanId);
                if (messages == null || messages.isEmpty()) {
                    messages = adminChatMessageRepository.findByPartnerMobileOrderByTimestampAsc(cleanId);
                }
            }
            if ((messages == null || messages.isEmpty()) && identifier != null) {
                messages = adminChatMessageRepository.findByIdentifierOrderByTimestampAsc(identifier);
            }
            return ResponseEntity.ok(messages != null ? messages : List.of());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody AdminChatMessage chatMessage) {
        try {
            String mob = chatMessage.getPartnerMobile() != null ? chatMessage.getPartnerMobile() : chatMessage.getIdentifier();
            if (mob != null) {
                String cleanMob = mob.replaceAll("^(\\+91|91)", "").trim();
                chatMessage.setPartnerMobile(cleanMob);
                chatMessage.setIdentifier(cleanMob);
            }

            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(java.time.Instant.now().toString());
            }

            // Database lo message save avvadam
            AdminChatMessage savedMsg = adminChatMessageRepository.save(chatMessage);

            // Live broadcast to both parties
            messagingTemplate.convertAndSend("/topic/chat/admin-partner/" + savedMsg.getIdentifier(), savedMsg);
            messagingTemplate.convertAndSend("/topic/admin/chats", savedMsg);

            return ResponseEntity.ok(savedMsg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving chat: " + e.getMessage());
        }
    }
}