package com.example.smartAttendence.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Firebase Service for Push Notifications and Real-time Messaging
 * 
 * Features:
 * - Push notifications for attendance alerts
 * - Real-time messaging capabilities
 * - Firebase Cloud Messaging (FCM) integration
 * - Mobile app notification support
 */
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseService(@Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Test Firebase connection
     */
    public boolean testConnection() {
        try {
            // Check if Firebase is initialized
            if (FirebaseApp.getApps().isEmpty()) {
                logger.warn("Firebase is not initialized");
                return false;
            }

            // Test basic Firebase functionality
            FirebaseApp.getInstance().getOptions();
            logger.info("Firebase connection test successful for project: {}", projectId);
            return true;

        } catch (Exception e) {
            logger.error("Firebase connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Firebase project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Check if Firebase is enabled (Messaging bean is present)
     */
    public boolean isEnabled() {
        return firebaseMessaging != null;
    }

    /**
     * Check if Firebase is initialized
     */
    public boolean isInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    /**
     * Send test notification
     */
    public String sendTestNotification() {
        try {
            // Create a test notification
            Notification notification = Notification.builder()
                    .setTitle("Smart Attendance Test")
                    .setBody("Firebase is working correctly!")
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .putData("type", "test")
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .setTopic("test")
                    .build();

            // Send the message
            String messageId = firebaseMessaging.send(message);
            
            logger.info("✅ Test notification sent successfully. Message ID: {}", messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("❌ Failed to send test notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send test notification", e);
        }
    }

    /**
     * Send attendance notification
     */
    public String sendAttendanceNotification(String studentName, String status, String timestamp) {
        try {
            Notification notification = Notification.builder()
                    .setTitle("Attendance Update")
                    .setBody(studentName + " marked as " + status)
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .putData("type", "attendance")
                    .putData("studentName", studentName)
                    .putData("status", status)
                    .putData("timestamp", timestamp)
                    .setTopic("attendance")
                    .build();

            String messageId = firebaseMessaging.send(message);
            
            logger.info("✅ Attendance notification sent. Message ID: {}", messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("❌ Failed to send attendance notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send attendance notification", e);
        }
    }

    /**
     * Send emergency notification
     */
    public String sendEmergencyNotification(String message, String location) {
        try {
            Notification notification = Notification.builder()
                    .setTitle("🚨 Emergency Alert")
                    .setBody(message)
                    .build();

            Message fcmMessage = Message.builder()
                    .setNotification(notification)
                    .putData("type", "emergency")
                    .putData("message", message)
                    .putData("location", location)
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .setTopic("emergency")
                    .build();

            String messageId = firebaseMessaging.send(fcmMessage);
            
            logger.info("✅ Emergency notification sent. Message ID: {}", messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("❌ Failed to send emergency notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send emergency notification", e);
        }
    }

    /**
     * Send custom notification
     */
    public String sendCustomNotification(String title, String body, Map<String, String> data, String topic) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notification)
                    .setTopic(topic);

            // Add custom data
            if (data != null) {
                data.forEach(messageBuilder::putData);
            }

            Message message = messageBuilder.build();
            String messageId = firebaseMessaging.send(message);
            
            logger.info("✅ Custom notification sent. Message ID: {}", messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("❌ Failed to send custom notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send custom notification", e);
        }
    }

    @PostConstruct
    public void initialize() {
        if (isInitialized()) {
            logger.info("🔥 Firebase Service is ENABLED and READY");
            logger.info("📱 Push notifications active");
        } else {
            logger.warn("⚠️ Firebase Service is currently in standby (Waiting for connection)");
        }
    }
}
