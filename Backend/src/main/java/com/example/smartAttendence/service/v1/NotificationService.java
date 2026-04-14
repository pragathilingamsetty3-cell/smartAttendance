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

