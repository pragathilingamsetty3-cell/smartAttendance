package com.example.smartAttendence.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            logger.error("❌ Firebase is enabled but FIREBASE_SERVICE_ACCOUNT_PATH is missing!");
            return null;
        }

        try {
            // Load the JSON key file from the classpath
            var resource = new org.springframework.core.io.ClassPathResource(serviceAccountPath);
            if (!resource.exists()) {
                throw new IOException("Firebase key file not found at: " + serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("✅ Firebase successfully initialized via JSON file: {}", serviceAccountPath);
            }

            return FirebaseMessaging.getInstance();

        } catch (Exception e) {
            logger.error("❌ Failed to initialize Firebase: {}", e.getMessage());
            throw new RuntimeException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }
}