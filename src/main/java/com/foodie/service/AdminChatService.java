package com.foodie.service;

import com.foodie.model.AdminChatMessage;
import com.foodie.repository.AdminChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminChatService {

	@Autowired
	private AdminChatMessageRepository chatRepository;

	// పార్ట్‌నర్ మొబైల్ ఆధారంగా హిస్టరీ తెచ్చుకోవడం
	// పార్ట్‌నర్ మొబైల్ లేదా షాప్ ఐడీ (identifier) ఆధారంగా హిస్టరీ తెచ్చుకోవడం
	public List<AdminChatMessage> getChatHistory(String identifier) {
		return chatRepository.findByIdentifierOrderByTimestampAsc(identifier);

	}

	// మెసేజ్ సేవ్ చేయడం
	public AdminChatMessage saveMessage(AdminChatMessage message) {
		if (message.getTimestamp() == null) {
			message.setTimestamp(LocalDateTime.now().toString());
		}
		return chatRepository.save(message);
	}
}