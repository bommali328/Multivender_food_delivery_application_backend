package com.foodie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic (broadcasting ki), /queue (user-specific messages ki) enable chestunnam
        config.enableSimpleBroker("/topic", "/queue");
        
        // App destination prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix (Swiggy-style direct partner assignment kosam)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-foodiee")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}