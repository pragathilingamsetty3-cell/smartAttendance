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

    @Value("${FIREBASE_KEY_BASE64:}")
    private String firebaseKeyBase64;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        // 📡 HIGHEST PRIORITY: Base64 String (Cloud Production)
        if (firebaseKeyBase64 != null && !firebaseKeyBase64.trim().isEmpty()) {
            try {
                // 🛡️ HARDENING: Aggressively remove anything that is NOT a valid Base64 character (including '\', quotes, spaces, etc.)
                String sanitizedKey = firebaseKeyBase64.replaceAll("[^a-zA-Z0-9+/=]", "");
                byte[] decodedKey = Base64.getDecoder().decode(sanitizedKey);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey)))
                        .build();
                
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    logger.info("🚀 Firebase initialized via FIREBASE_KEY_BASE64 string!");
                }
                return FirebaseMessaging.getInstance();
            } catch (Exception e) {
                logger.error("❌ Failed to initialize Firebase from Base64: {}", e.getMessage());
                // Fall through to file loading if Base64 fails
            }
        }

        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            logger.error("❌ Firebase is enabled but neither FIREBASE_KEY_BASE64 nor path is provided!");
            return null;
        }

        try {
            // 📡 SMART LOADING: Check FileSystem (Render) then ClassPath (Local)
            java.io.File file = new java.io.File(serviceAccountPath);
            java.io.InputStream inputStream;

            if (file.exists()) {
                inputStream = new java.io.FileInputStream(file);
                logger.info("✅ Firebase initializing via FileSystem: {}", serviceAccountPath);
            } else {
                // Fallback to ClassPath (inside the JAR)
                var resource = new org.springframework.core.io.ClassPathResource(serviceAccountPath);
                if (!resource.exists()) {
                    // Try to see if it's in a subdirectory in classpath
                    logger.warn("⚠️ File not found at root, checking for variants...");
                    throw new IOException("Firebase key file not found at: " + serviceAccountPath);
                }
                inputStream = resource.getInputStream();
                logger.info("✅ Firebase initializing via ClassPath: {}", serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("🚀 FirebaseApp initialized successfully!");
            }

            return FirebaseMessaging.getInstance();

        } catch (Exception e) {
            logger.error("⚠️ Firebase initialization failed: {}. Continuing without Firebase features.", e.getMessage());
            return null;
        }
    }
}