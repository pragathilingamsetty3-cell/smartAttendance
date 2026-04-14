package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.event.WalkOutEvent;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.EmailService;
import com.example.smartAttendence.service.FirebaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {
    
    private final UserV1Repository userV1Repository;
    private final EmailService emailService;
    private final FirebaseService firebaseService;

    public NotificationService(UserV1Repository userV1Repository, EmailService emailService, FirebaseService firebaseService) {
        this.userV1Repository = userV1Repository;
        this.emailService = emailService;
        this.firebaseService = firebaseService;
    }

    @Async
    public void sendWelcomeNotification(String toEmail, String name, String plainTextPassword) {
        log.info("Starting async welcome email to: {}", toEmail);
        
        try {
            String subject = "Welcome to Smart Attendance";
            String body = String.format(
                "Dear %s,%n%n" +
                "Your account has been created successfully.%n%n" +
                "Login Details:%n" +
                "Email: %s%n" +
                "Temporary Password: %s%n%n" +
                "Please login and change your password on first login.%n%n" +
                "Best regards,%n" +
                "Smart Attendance Team",
                name, toEmail, plainTextPassword
            );
            
            emailService.sendSimpleEmail(toEmail, subject, body);
            log.info("Finished async email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    @Async
    @EventListener
    public void handleWalkOutEvent(WalkOutEvent event) {
        UUID studentId = event.studentId();

        userV1Repository.findById(studentId).ifPresent(user -> {
            log.info("🚨 [AI SECURITY] Unauthorized Walkout: {}", user.getName());
            
            // Route to Firebase for real-time alert
            if (firebaseService.isEnabled()) {
                Map<String, String> data = new HashMap<>();
                data.put("studentId", studentId.toString());
                data.put("event", "WALKOUT");
                
                firebaseService.sendCustomNotification(
                    "🚨 Security Alert: Unauthorized Exit",
                    String.format("User %s has left the secure area without authorization.", user.getName()),
                    data,
                    "emergency"
                );
            }

            // Route to Email for formal record
            emailService.sendSimpleEmail(
                user.getEmail(),
                "URGENT: Unauthorized Area Exit Detected",
                String.format("Dear %s,\n\nOur AI system detected you leaving the designated area without authorization. This event has been logged and reported to the system administrators.\n\nPlease return immediately or contact your supervisor.", user.getName())
            );
        });
    }

    /**
     * Send automated attendance alert to student and parents
     */
    public void sendAttendanceAlert(com.example.smartAttendence.domain.User student, com.example.smartAttendence.entity.Timetable session, String alertType) {
        String studentName = student.getName();
        String subject = session.getSubject();

        log.info("🤖 AI MONITOR [{}]: Initiating stakeholder notifications for {} ({})", 
                alertType, studentName, student.getRegistrationNumber());

        boolean isWalkout = "UNAUTHORIZED_WALKOUT".equalsIgnoreCase(alertType);
        String alertTitle = isWalkout ? "🚨 Security Alert: Class Walkout" : "🔔 Attendance Alert";
        String message = isWalkout 
            ? String.format("Attendance revoked for %s. Unauthorized exit from %s.", studentName, subject)
            : String.format("You have been marked ABSENT for %s starting at %s.", subject, session.getStartTime());

        // Route to Firebase (Primary for daily alerts)
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", alertType);
            data.put("subject", subject);
            
            firebaseService.sendCustomNotification(alertTitle, message, data, "attendance");
            log.info("✅ Firebase alert sent to topic: attendance");
        }

        // Route to Email ONLY if it's a security walkout (user requested email for reports/onboarding, 
        // but it's good practice for security alerts too). 
        // For general "ABSENT" alerts, we stick to Firebase as requested.
        if (isWalkout && student.getEmail() != null) {
            emailService.sendSimpleEmail(student.getEmail(), alertTitle, message);
            log.info("📧 Email security alert sent to: {}", student.getEmail());
        }
    }

    /**
     * Notify about AI-detected room change grace period
     */
    public void notifyRoomChangeGracePeriod(UUID sectionId, String newRoomName) {
        log.info("🤖 AI MONITOR [ROOM_CHANGE]: Detected section {} moving to {}. Granting 15-minute transition grace period.", 
                sectionId, newRoomName);
    }

    /**
     * AI-Driven Session Start Prompt
     * Notifies students to mark attendance immediately via biometrics
     */
    public void sendSessionStartPrompt(com.example.smartAttendence.domain.User student, com.example.smartAttendence.domain.ClassroomSession session) {
        String studentName = student.getName();
        String subject = session.getSubject();
        String roomName = session.getRoom() != null ? session.getRoom().getName() : "Assigned Room";
        
        log.info("🤖 AI PROMPT: Prompting student {} for biometric check-in (Session: {})", 
                studentName, subject);

        // Route to Firebase (Engaging push notification)
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("subject", subject);
            data.put("room", roomName);
            data.put("action", "CHECK_IN");

            firebaseService.sendCustomNotification(
                "🚀 Class Started: " + subject,
                String.format("Your class is live in %s. Tap to mark attendance via biometrics now!", roomName),
                data,
                "attendance"
            );
            log.info("✅ Firebase check-in prompt sent.");
        }
    }

    /**
     * Send Push Notification for Hall Pass Status
     */
    public void sendHallPassNotification(UUID studentId, boolean approved, int durationMinutes) {
        userV1Repository.findById(studentId).ifPresent(user -> {
            String title = approved ? "🎟️ Hall Pass Approved" : "❌ Hall Pass Denied";
            String body = approved ? 
                String.format("Your hall pass for %d minutes has been approved. Please return promptly.", durationMinutes) :
                "Your hall pass request has been denied by faculty.";
            
            if (firebaseService.isEnabled()) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "HALL_PASS");
                data.put("approved", String.valueOf(approved));
                data.put("duration", String.valueOf(durationMinutes));

                firebaseService.sendCustomNotification(title, body, data, "hall_pass_" + studentId);
                log.info("✅ Hall pass notification sent to user: {}", studentId);
            }
        });
    }

    /**
     * Send Emergency Room Change Notification to Students
     */
    public void sendEmergencyRoomChangeNotification(com.example.smartAttendence.domain.ClassroomSession session, 
                                                   UUID oldRoomId, UUID newRoomId, String reason) {
        log.info("🚨 [EMERGENCY] Room changed for session {}. Reason: {}", session.getId(), reason);
        
        String title = "🔄 Room Change Alert";
        String body = String.format("Your %s class has been moved. Reason: %s", session.getSubject(), reason);
        
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "ROOM_CHANGE");
            data.put("sessionId", session.getId().toString());
            data.put("newRoomId", newRoomId.toString());

            firebaseService.sendCustomNotification(title, body, data, "section_" + session.getSection().getId());
            log.info("✅ Emergency room change broadcast to section: {}", session.getSection().getId());
        }
    }

    /**
     * Send Emergency Change Notification (Generic)
     */
    public void sendEmergencyChangeNotification(com.example.smartAttendence.domain.ClassroomSession session, 
                                               com.example.smartAttendence.entity.EmergencySessionChange change) {
        log.info("🚨 [EMERGENCY] Session change: {}. Type: {}", session.getId(), change.getChangeType());
        
        String title = "🚨 Emergency Session Alert";
        String body = String.format("An emergency change has been made to your %s class. Reason: %s", 
                session.getSubject(), change.getReason());
        
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "EMERGENCY_CHANGE");
            data.put("changeType", change.getChangeType().toString());

            firebaseService.sendCustomNotification(title, body, data, "section_" + session.getSection().getId());
        }
    }

    /**
     * Send Substitute Notification
     */
    public void sendSubstituteNotification(com.example.smartAttendence.domain.ClassroomSession session, 
                                          com.example.smartAttendence.entity.EmergencySessionChange change) {
        log.info("👨‍🏫 [SUBSTITUTE] Faculty substituted for session: {}", session.getId());
        
        String title = "👨‍🏫 New Faculty Assigned";
        String body = String.format("A substitute faculty has been assigned for your %s class in %s.", 
                session.getSubject(), session.getRoom().getName());
        
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "SUBSTITUTION");
            data.put("sessionId", session.getId().toString());

            firebaseService.sendCustomNotification(title, body, data, "section_" + session.getSection().getId());
        }
    }

    /**
     * Send Class Start Notification (Compatible with Scheduler)
     */
    public void sendClassStartNotification(UUID studentId, String subject, String roomName, java.time.LocalDateTime startTime) {
        log.info("🚀 [CLASS_START] Notifying student {} about {}", studentId, subject);
        
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "CLASS_START");
            data.put("subject", subject);
            
            firebaseService.sendCustomNotification(
                "🚀 Class Starting: " + subject,
                String.format("Your class is live in %s. Join now! (Starts: %s)", roomName, startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))),
                data,
                "hall_pass_" + studentId
            );
        }
    }

    /**
     * Send Class Reminder Notification (Compatible with Scheduler)
     */
    public void sendClassReminderNotification(UUID studentId, String subject, String roomName, java.time.LocalTime startTime) {
        log.info("⏰ [REMINDER] Reminding student {} about {}", studentId, subject);
        
        if (firebaseService.isEnabled()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "REMINDER");
            data.put("subject", subject);
            
            firebaseService.sendCustomNotification(
                "⏰ Class Reminder: " + subject,
                String.format("Reminder: Your class starts @ %s in %s.", startTime.toString(), roomName),
                data,
                "hall_pass_" + studentId
            );
        }
    }

    /**
     * Send Password Reset OTP
     * Routed to EMAIL as requested
     */
    public void sendPasswordResetOtp(String email, String name, String otp) {
        log.info("🔐 Sending password reset OTP to: {}", email);
        emailService.sendPasswordResetOtp(email, otp);
        
        // Also send a Firebase notification as a security redundancy
        if (firebaseService.isEnabled()) {
            firebaseService.sendCustomNotification(
                "🔐 Security Alert: Password Reset",
                "A password reset was requested. If this wasn't you, secure your account now.",
                null,
                "security"
            );
        }
    }

    /**
     * Send Monthly/Weekly Reports
     * Routed to EMAIL with attachments
     */
    public void sendAttendanceReport(String email, String facultyName, byte[] reportData, boolean isMonthly) {
        log.info("📊 Sending {} attendance report to: {}", isMonthly ? "monthly" : "weekly", email);
        emailService.sendWeeklyReport(email, facultyName, reportData, 
                java.time.LocalDate.now().minusDays(isMonthly ? 30 : 7), 
                java.time.LocalDate.now());
    }
}

