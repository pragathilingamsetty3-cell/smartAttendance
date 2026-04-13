package com.example.smartAttendence.config;

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
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. RAW ENDPOINT: This allows Postman to connect directly
        registry.addEndpoint("/ws-attendance")
                .setAllowedOriginPatterns("*"); 

        // 2. SOCKJS ENDPOINT: This is for your Frontend/Mobile App
        registry.addEndpoint("/ws-attendance")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}