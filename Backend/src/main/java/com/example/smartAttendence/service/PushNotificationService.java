package com.example.smartAttendence.service;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private final FirebaseMessaging firebaseMessaging;

    public PushNotificationService(@Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendSessionStartNotification(String deviceToken, String sessionTitle, String roomName) {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging is not available, skipping session start notification");
            return;
        }
        
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .putData("type", "session_start")
                    .putData("sessionId", sessionTitle)
                    .setNotification(Notification.builder()
                            .setTitle("Session Started")
                            .setBody(sessionTitle + " has started in " + roomName)
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent session start notification: {}", response);

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send session start notification", e);
        }
    }

    public void sendBulkNotification(List<String> deviceTokens, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging is not available, skipping bulk notification");
            return;
        }
        
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .addAllTokens(deviceTokens)
                    .putAllData(data)
                    .build();

            BatchResponse response = firebaseMessaging.sendMulticast(message);
            logger.info("Successfully sent {} notifications, {} failed", 
                    response.getSuccessCount(), response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send bulk notifications", e);
        }
    }

    public void sendAttendanceConfirmation(String deviceToken, String status, String sessionTitle) {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging is not available, skipping attendance confirmation");
            return;
        }
        
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .putData("type", "attendance_confirmation")
                    .putData("status", status)
                    .setNotification(Notification.builder()
                            .setTitle("Attendance Marked")
                            .setBody("Your attendance for " + sessionTitle + " has been marked as " + status)
                            .build())
                    .build();

            firebaseMessaging.send(message);
            logger.info("Attendance confirmation sent successfully");

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send attendance confirmation", e);
        }
    }

    /**
     * Sends push notification when class automatically starts (autonomous system)
     */
    public void sendClassStartNotification(UUID studentId, String subject, String roomName, LocalDateTime startTime) {
        String title = "Class Started - " + subject;
        String body = String.format("Your %s class has started in %s. Please open the app and mark your attendance.", 
                                   subject, roomName);
        
        Map<String, String> data = Map.of(
            "type", "CLASS_START",
            "subject", subject,
            "room", roomName,
            "startTime", startTime.toString(),
            "studentId", studentId.toString()
        );

        sendNotificationToUser(studentId, title, body, data);
    }

    /**
     * Sends reminder notification 5 minutes before class (autonomous system)
     */
    public void sendClassReminderNotification(UUID studentId, String subject, String roomName, LocalTime startTime) {
        String title = "Upcoming Class - " + subject;
        String body = String.format("Your %s class starts at %s in %s. Be ready to mark attendance.", 
                                   subject, startTime, roomName);
        
        Map<String, String> data = Map.of(
            "type", "CLASS_REMINDER",
            "subject", subject,
            "room", roomName,
            "startTime", startTime.toString(),
            "studentId", studentId.toString()
        );

        sendNotificationToUser(studentId, title, body, data);
    }

    /**
     * Sends notification for hall pass approval/rejection
     */
    public void sendHallPassNotification(UUID studentId, boolean approved, int durationMinutes) {
        String title = approved ? "Hall Pass Approved" : "Hall Pass Denied";
        String body = approved ? 
            String.format("Your hall pass for %d minutes has been approved.", durationMinutes) :
            "Your hall pass request has been denied by faculty.";
        
        Map<String, String> data = Map.of(
            "type", "HALL_PASS",
            "approved", String.valueOf(approved),
            "duration", String.valueOf(durationMinutes),
            "studentId", studentId.toString()
        );

        sendNotificationToUser(studentId, title, body, data);
    }

    /**
     * Sends notification when student is marked as walked out
     */
    public void sendWalkOutNotification(UUID studentId, String subject, LocalDateTime walkOutTime) {
        String title = "Attendance Alert - Walk Out Detected";
        String body = String.format("You were marked as having walked out of %s at %s. This has been reported to faculty.", 
                                   subject, walkOutTime.toLocalTime());
        
        Map<String, String> data = Map.of(
            "type", "WALK_OUT",
            "subject", subject,
            "walkOutTime", walkOutTime.toString(),
            "studentId", studentId.toString()
        );

        sendNotificationToUser(studentId, title, body, data);
    }

    /**
     * Sends notification to faculty for walk-out events
     */
    public void sendWalkOutAlertToFaculty(UUID facultyId, UUID studentId, String subject, LocalDateTime walkOutTime) {
        String title = "Student Walk Out Alert";
        String body = String.format("Student %s walked out of %s class at %s.", 
                                   studentId.toString().substring(0, 8), subject, walkOutTime.toLocalTime());
        
        Map<String, String> data = Map.of(
            "type", "WALK_OUT_ALERT",
            "studentId", studentId.toString(),
            "subject", subject,
            "walkOutTime", walkOutTime.toString(),
            "facultyId", facultyId.toString()
        );

        sendNotificationToUser(facultyId, title, body, data);
    }

    /**
     * Sends exam day barcode scanning notification to faculty
     */
    public void sendExamDayNotification(UUID facultyId, String subject, String roomName, LocalDateTime examTime) {
        String title = "Exam Day - Ready for Barcode Scanning";
        String body = String.format("%s exam is starting in %s. Please open the app to scan student ID barcodes.", 
                                   subject, roomName);
        
        Map<String, String> data = Map.of(
            "type", "EXAM_DAY",
            "subject", subject,
            "room", roomName,
            "examTime", examTime.toString(),
            "facultyId", facultyId.toString()
        );

        sendNotificationToUser(facultyId, title, body, data);
    }

    /**
     * Generic method to send push notification to a user (autonomous system)
     */
    private void sendNotificationToUser(UUID userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging is not available, skipping notification to user {}", userId);
            return;
        }
        
        try {
            String fcmToken = getFcmTokenForUser(userId);
            
            if (fcmToken == null) {
                logger.warn("No FCM token found for user {}", userId);
                return;
            }

            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setColor("#4285F4")
                        .setIcon("ic_notification")
                        .build())
                    .build())
                .setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                    .build())
                .build();

            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent notification to user {}: {}", userId, response);

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Sends notification for emergency session changes
     */
    public void sendEmergencyChangeNotification(com.example.smartAttendence.domain.ClassroomSession session, 
                                              com.example.smartAttendence.entity.EmergencySessionChange change) {
        String title = "Emergency Schedule Change";
        String body = buildEmergencyChangeMessage(session, change);
        
        Map<String, String> data = Map.of(
            "type", "EMERGENCY_CHANGE",
            "sessionId", session.getId().toString(),
            "changeType", change.getChangeType().toString(),
            "reason", change.getReason(),
            "effectiveTimestamp", change.getEffectiveTimestamp().toString()
        );

        // Send to all enrolled students
        sendBulkNotificationToSessionStudents(session, title, body, data);
    }

    /**
     * Sends notification for substitute faculty
     */
    public void sendSubstituteNotification(com.example.smartAttendence.domain.ClassroomSession session,
                                         com.example.smartAttendence.entity.EmergencySessionChange change) {
        String title = "Faculty Change Notification";
        String body = String.format("Your %s class has a substitute faculty due to: %s", 
                                   session.getSubject(), change.getReason());
        
        Map<String, String> data = Map.of(
            "type", "FACULTY_SUBSTITUTION",
            "sessionId", session.getId().toString(),
            "originalFacultyId", change.getOriginalFacultyId().toString(),
            "newFacultyId", change.getNewFacultyId().toString(),
            "reason", change.getReason()
        );

        sendBulkNotificationToSessionStudents(session, title, body, data);
    }

    /**
     * Sends emergency room change notification
     */
    public void sendEmergencyRoomChangeNotification(com.example.smartAttendence.domain.ClassroomSession session,
                                                   UUID oldRoomId, UUID newRoomId, String reason) {
        String title = "Room Change Alert";
        String body = String.format("Your %s class has been moved to a new room. Reason: %s", 
                                   session.getSubject(), reason);
        
        Map<String, String> data = Map.of(
            "type", "EMERGENCY_ROOM_CHANGE",
            "sessionId", session.getId().toString(),
            "oldRoomId", oldRoomId.toString(),
            "newRoomId", newRoomId.toString(),
            "reason", reason
        );

        sendBulkNotificationToSessionStudents(session, title, body, data);
    }

    /**
     * Build emergency change message based on change type
     */
    private String buildEmergencyChangeMessage(com.example.smartAttendence.domain.ClassroomSession session,
                                             com.example.smartAttendence.entity.EmergencySessionChange change) {
        return switch (change.getChangeType()) {
            case FACULTY_SUBSTITUTION -> 
                String.format("Your %s class has a substitute faculty. Reason: %s", session.getSubject(), change.getReason());
            case ROOM_CHANGE -> 
                String.format("Your %s class has been moved to a new room. Reason: %s", session.getSubject(), change.getReason());
            case TIME_CHANGE -> 
                String.format("Your %s class schedule has changed. Reason: %s", session.getSubject(), change.getReason());
            case CANCELLATION -> 
                String.format("Your %s class has been cancelled. Reason: %s", session.getSubject(), change.getReason());
            default -> 
                String.format("Emergency change for your %s class. Reason: %s", session.getSubject(), change.getReason());
        };
    }

    /**
     * Send bulk notification to all students in a session
     */
    private void sendBulkNotificationToSessionStudents(com.example.smartAttendence.domain.ClassroomSession session,
                                                       String title, String body, Map<String, String> data) {
        // TODO: Implement logic to get all enrolled students' device tokens
        // This would typically involve:
        // 1. Querying the section/students for the session
        // 2. Getting their FCM tokens from user_tokens table
        // 3. Sending bulk notification
        
        List<String> studentTokens = List.of(); // Placeholder
        sendBulkNotification(studentTokens, title, body, data);
    }

    /**
     * Retrieve FCM token for a user from database
     * TODO: Implement token retrieval based on your user entity structure
     */
    private String getFcmTokenForUser(UUID userId) {
        // TODO: Implement token retrieval from database
        // This would typically involve querying a user_tokens table or similar
        // For now, return null to indicate no token available
        return null;
    }
}
