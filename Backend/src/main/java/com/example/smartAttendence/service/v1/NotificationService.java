package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.event.WalkOutEvent;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {
    
    private final UserV1Repository userV1Repository;
    private final EmailService emailService;

    public NotificationService(UserV1Repository userV1Repository, EmailService emailService) {
        this.userV1Repository = userV1Repository;
        this.emailService = emailService;
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
            log.info("🚨 [SMS GATEWAY TRIGGERED] 🚨");
            log.info("To: Parent of {}", user.getName());
            log.info(
                    "Message: URGENT - Student {} has left the classroom without authorization. Attendance revoked.",
                    user.getName()
            );
        });
    }

    /**
     * Send automated attendance alert to student and parents
     */
    public void sendAttendanceAlert(com.example.smartAttendence.domain.User student, com.example.smartAttendence.entity.Timetable session, String alertType) {
        String studentName = student.getName();
        String subject = session.getSubject();
        String parentMobile = student.getParentMobile();
        String studentMobile = student.getStudentMobile();

        log.info("🤖 AI MONITOR [{}]: Initiating stakeholder notifications for {} ({})", 
                alertType, studentName, student.getRegistrationNumber());

        boolean isWalkout = "UNAUTHORIZED_WALKOUT".equalsIgnoreCase(alertType);
        String parentMessage = isWalkout 
            ? String.format("URGENT: Your ward %s has been marked ABSENT because they left the %s session without authorization and did not return.", studentName, subject)
            : String.format("Alert! Your ward %s is marked ABSENT for the %s session starting at %s.", studentName, subject, session.getStartTime());

        if (parentMobile != null && !parentMobile.isEmpty()) {
            log.info("📩 SMS SENT TO PARENT [{}]: {}", parentMobile, parentMessage);
        }

        if (studentMobile != null && !studentMobile.isEmpty()) {
            String studentMessage = isWalkout
                ? String.format("AI SECURITY ALERT: You have been marked ABSENT for leaving class %s unauthorized. This incident has been reported to parents.", subject)
                : String.format("Attendance Alert: You have been marked ABSENT for %s. Please check in immediately if this is an error.", subject);
            log.info("📩 SMS SENT TO STUDENT [{}]: {}", studentMobile, studentMessage);
        }

        if (student.getParentEmail() != null) {
            String emailSubject = isWalkout ? "SECURITY ALERT: Unauthorized Class Departure" : "Official Attendance Notification - " + subject;
            log.info("📧 EMAIL SENT TO PARENT [{}]: {} - STATUS: ABSENT", 
                    student.getParentEmail(), emailSubject);
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

        // SMS Notification
        if (student.getStudentMobile() != null) {
            log.info("📩 SMS PROMPT SENT [{}]: 🤖 AI ALERT: Your class for {} has started in Room {}. Please mark your presence via biometrics NOW.", 
                    student.getStudentMobile(), subject, roomName);
        }

        // Push Notification Simulation
        log.info("🔔 PUSH NOTIFICATION SENT to {}: Subject: Class Started | Body: [AI] {} is live in {}. Tap to check-in.", 
                student.getEmail(), subject, roomName);
    }
}

