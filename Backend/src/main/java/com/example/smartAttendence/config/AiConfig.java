package com.example.smartAttendence.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${spring.ai.google.genai.api-key:}")
    private String apiKey;

    /**
     * Overrides the default Google GenAI Client to prevent crashes when the API key is missing.
     */
    @Bean
    @Primary
    public Client googleGenAiClient() {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("${")) {
            log.warn("⚠️ AI Configuration: No Gemini API Key found. AI features will be DISABLED.");
            return null;
        }
        
        try {
            return Client.builder()
                    .apiKey(apiKey)
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to initialize Google GenAI Client: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public ChatClient chatClient(org.springframework.beans.factory.ObjectProvider<ChatClient.Builder> builderProvider) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("⚠️ ChatClient.Builder not available (AI is disabled).");
            return null;
        }
        return builder.build();
    }
}
