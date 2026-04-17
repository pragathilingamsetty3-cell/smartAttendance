package com.example.smartAttendence.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
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
    private static volatile boolean firebaseInitialized = false;

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Value("${FIREBASE_KEY_BASE64:}")
    private String firebaseKeyBase64;

    /**
     * Initialize Firebase with multiple fallback strategies
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        synchronized (FirebaseConfig.class) {
            if (firebaseInitialized) {
                logger.info("📡 Firebase already initialized, returning existing instance");
                try {
                    return FirebaseMessaging.getInstance();
                } catch (Exception e) {
                    logger.warn("⚠️ Firebase initialization was attempted but unavailable: {}", e.getMessage());
                    return null;
                }
            }

            // 📡 STRATEGY 1: Base64 String (Cloud Production - Highest Priority)
            if (firebaseKeyBase64 != null && !firebaseKeyBase64.trim().isEmpty()) {
                if (initializeFromBase64(firebaseKeyBase64)) {
                    return FirebaseMessaging.getInstance();
                }
            }

            // 📡 STRATEGY 2: File System (Render/Production Deployment)
            if (serviceAccountPath != null && !serviceAccountPath.trim().isEmpty()) {
                if (initializeFromFile(serviceAccountPath)) {
                    return FirebaseMessaging.getInstance();
                }
            }

            // ⚠️ GRACEFUL DEGRADATION: Firebase disabled if no credentials available
            logger.warn("⚠️ Firebase disabled: Neither FIREBASE_KEY_BASE64 nor FIREBASE_SERVICE_ACCOUNT_PATH provided valid credentials");
            firebaseInitialized = true; // Mark as attempted
            return null;
        }
    }

    /**
     * Initialize Firebase from Base64-encoded service account JSON
     */
    private boolean initializeFromBase64(String base64Key) {
        try {
            logger.info("📡 Attempting Firebase initialization from FIREBASE_KEY_BASE64...");
            
            String sanitizedKey = base64Key.trim().replaceAll("[^a-zA-Z0-9+/=]", "");
            
            if (sanitizedKey.isEmpty()) {
                logger.error("❌ FIREBASE_KEY_BASE64 is invalid (empty after sanitization)");
                return false;
            }

            byte[] decodedKey = Base64.getDecoder().decode(sanitizedKey);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey)))
                    .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                logger.info("✅ Firebase successfully initialized from FIREBASE_KEY_BASE64!");
                return true;
            }
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("❌ Base64 decoding failed: Invalid Base64 format - {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Firebase initialization from Base64 failed: {} [{}]", e.getMessage(), e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Initialize Firebase from file path (filesystem or classpath)
     */
    private boolean initializeFromFile(String path) {
        try {
            logger.info("📡 Attempting Firebase initialization from file: {}", path);
            
            java.io.InputStream inputStream;
            java.io.File file = new java.io.File(path);

            if (file.exists()) {
                inputStream = new java.io.FileInputStream(file);
                logger.info("✅ Found Firebase key at filesystem: {}", path);
            } else {
                var resource = new org.springframework.core.io.ClassPathResource(path);
                if (resource.exists()) {
                    inputStream = resource.getInputStream();
                    logger.info("✅ Found Firebase key in classpath: {}", path);
                } else {
                    logger.error("❌ Firebase key file not found at: {} (filesystem or classpath)", path);
                    return false;
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                logger.info("✅ Firebase successfully initialized from file!");
                return true;
            }
            return true;
        } catch (Exception e) {
            logger.error("❌ Firebase initialization from file failed: {} [{}]", e.getMessage(), e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Firestore bean - gracefully handles missing Firebase initialization
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public Firestore firestore() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.warn("⚠️ Firestore not initialized: FirebaseApp not available. Threat detection will use local caching only.");
                return null;
            }
            Firestore fs = FirestoreClient.getFirestore();
            logger.info("✅ Firestore bean created successfully");
            return fs;
        } catch (Exception e) {
            logger.error("❌ Failed to create Firestore bean: {}", e.getMessage());
            return null;
        }
    }
}