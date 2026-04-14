package com.example.smartAttendence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.time.LocalDate;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Password Reset OTP - Smart Attendance System");
            message.setText("Hello,\n\n" +
                           "You requested to reset your password for the Smart Attendance System.\n\n" +
                           "🔢 Your One-Time Password (OTP): " + otp + "\n\n" +
                           "⏰ Validity: This OTP will expire in 15 minutes\n\n" +
                           "🔒 Security Notice:\n" +
                           "• Never share this OTP with anyone\n" +
                           "• Our support team will never ask for your OTP\n" +
                           "• This OTP can only be used once\n\n" +
                           "If you didn't request this password reset, please ignore this email.\n\n" +
                           "Best regards,\n" +
                           "Smart Attendance Security Team");
            
            mailSender.send(message);
            logger.info("Password reset OTP sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset OTP", e);
        }
    }

    /**
     * Compatibility alias for legacy PasswordResetService
     */
    public void sendPasswordResetOTP(String email, String name, String otp) {
        sendPasswordResetOtp(email, otp);
    }
    
    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
        }
    }

    /**
     * Send password change confirmation
     */
    public void sendPasswordChangeConfirmation(String toEmail, String name) {
        String subject = "Security Alert: Password Changed";
        String body = String.format(
            "Dear %s,%n%n" +
            "The password for your Smart Attendance System account was successfully changed.%n%n" +
            "If you did not make this change, please contact the administrator immediately.%n%n" +
            "Best regards,%n" +
            "Smart Attendance Security Team",
            name
        );
        sendSimpleEmail(toEmail, subject, body);
    }

    /**
     * NO-OP Compatibility for SMS OTP
     */
    public void sendOTPSMS(String phoneNumber, String otp) {
        logger.info("[SMS_SIMULATION] OTP would be sent to {}: {}", phoneNumber, otp);
    }

    public void sendWeeklyReport(String toEmail, String facultyName, File reportFile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(toEmail);
            helper.setSubject("Weekly Attendance Report - " + facultyName);
            helper.setText("Dear " + facultyName + ",\n\n" +
                           "Please find attached your weekly attendance report.\n\n" +
                           "📊 Report Details:\n" +
                           "• Generated on: " + java.time.LocalDate.now() + "\n" +
                           "• Coverage: Past week (Monday - Sunday)\n" +
                           "• Format: Excel with multiple sheets\n\n" +
                           "📈 Report Contents:\n" +
                           "• Summary sheet: Overall attendance statistics\n" +
                           "• Detailed sheet: Individual student records\n" +
                           "• Analytics sheet: Trends and patterns\n\n" +
                           "🔍 Action Items:\n" +
                           "• Review students with <75% attendance\n" +
                           "• Identify patterns of absenteeism\n" +
                           "• Plan interventions for at-risk students\n\n" +
                           "For any questions about this report, please contact the system administrator.\n\n" +
                           "Best regards,\n" +
                           "Smart Attendance Reporting System\n" +
                           "🤖 AI-powered attendance analytics");
            
            FileSystemResource file = new FileSystemResource(reportFile);
            helper.addAttachment("Weekly_Attendance_Report_" + java.time.LocalDate.now() + ".xlsx", file);
            
            mailSender.send(message);
            logger.info("Weekly report sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send weekly report to: {}", toEmail, e);
            throw new RuntimeException("Failed to send weekly report", e);
        }
    }
    
    public void sendSessionStartNotification(String toEmail, String facultyName, String subject, java.time.Instant startTime, java.time.Instant endTime, String roomName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("🤖 Session Started Automatically: " + subject);
            message.setText("Dear " + facultyName + ",\n\n" +
                           "Good news! Your session has been automatically started by the AI Scheduler.\n\n" +
                           "📅 Session Details:\n" +
                           "• Subject: " + subject + "\n" +
                           "• Room: " + roomName + "\n" +
                           "• Start Time: " + startTime + "\n" +
                           "• End Time: " + endTime + "\n" +
                           "• Status: Active and ready for attendance\n\n" +
                           "🤖 AI Scheduler Features:\n" +
                           "• Automatic session creation based on timetable\n" +
                           "• PostGIS geofencing for accurate attendance\n" +
                           "• Real-time student location tracking\n" +
                           "• Automated report generation\n\n" +
                           "📱 What you can do:\n" +
                           "• Monitor attendance in real-time\n" +
                           "• View student locations on map\n" +
                           "• Generate instant reports\n" +
                           "• Manage exceptions (hall passes, etc.)\n\n" +
                           "The system will automatically end this session at the scheduled time.\n\n" +
                           "Best regards,\n" +
                           "AI Scheduler Team\n" +
                           "🚀 Smart Attendance Automation");
            
            mailSender.send(message);
            logger.info("Session start notification sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send session start notification to: {}", toEmail, e);
            // Don't throw exception here - this is a notification, not critical
        }
    }

    public void sendEmergencyChangeEmail(com.example.smartAttendence.domain.ClassroomSession session,
                                         com.example.smartAttendence.entity.EmergencySessionChange change) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(session.getFaculty().getEmail());
            message.setSubject("🚨 Emergency Session Change: " + session.getSubject());
            message.setText("Dear " + session.getFaculty().getName() + ",\n\n" +
                           "An emergency change has been made to your session.\n\n" +
                           "📅 Session Details:\n" +
                           "• Subject: " + session.getSubject() + "\n" +
                           "• Session ID: " + session.getId() + "\n" +
                           "• Change Type: " + change.getChangeType() + "\n" +
                           "• Reason: " + change.getReason() + "\n" +
                           "• Effective Time: " + change.getEffectiveTimestamp() + "\n" +
                           "• Changed By: " + change.getChangedBy().getName() + "\n\n" +
                           "🔄 Changes Made:\n" +
                           buildChangeDetails(change) + "\n\n" +
                           "This change was processed as an emergency override.\n\n" +
                           "Best regards,\n" +
                           "Smart Attendance Emergency Response Team");
            
            mailSender.send(message);
            logger.info("Emergency change notification sent to faculty: {}", session.getFaculty().getEmail());
        } catch (Exception e) {
            logger.error("Failed to send emergency change email to: {}", session.getFaculty().getEmail(), e);
        }
    }

    public void sendSubstituteNotification(com.example.smartAttendence.domain.ClassroomSession session,
                                          com.example.smartAttendence.entity.EmergencySessionChange change) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(session.getFaculty().getEmail());
            message.setSubject("👨‍🏫 Substitute Faculty Assignment: " + session.getSubject());
            message.setText("Dear " + session.getFaculty().getName() + ",\n\n" +
                           "You have been assigned as substitute faculty for the following session.\n\n" +
                           "📅 Session Details:\n" +
                           "• Subject: " + session.getSubject() + "\n" +
                           "• Session ID: " + session.getId() + "\n" +
                           "• Room: " + session.getRoom().getName() + "\n" +
                           "• Start Time: " + session.getStartTime() + "\n" +
                           "• End Time: " + session.getEndTime() + "\n" +
                           "• Original Faculty ID: " + change.getOriginalFacultyId() + "\n" +
                           "• Reason for Substitution: " + change.getReason() + "\n\n" +
                           "🔧 Your Responsibilities:\n" +
                           "• Monitor student attendance via the mobile app\n" +
                           "• Approve/deny hall pass requests as needed\n" +
                           "• Handle any classroom emergencies\n" +
                           "• Generate end-of-session reports\n\n" +
                           "The AI attendance system will handle automatic student tracking.\n\n" +
                           "Best regards,\n" +
                           "Smart Attendance Scheduling Team");
            
            mailSender.send(message);
            logger.info("Substitute notification sent to: {}", session.getFaculty().getEmail());
        } catch (Exception e) {
            logger.error("Failed to send substitute notification to: {}", session.getFaculty().getEmail(), e);
        }
    }

    /**
     * Send weekly attendance report with Excel attachment
     */
    public void sendWeeklyReport(String toEmail, String facultyName, byte[] excelReport, 
                              LocalDate weekStart, LocalDate weekEnd) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(toEmail);
            helper.setSubject("Weekly Attendance Report - " + weekStart + " to " + weekEnd);
            helper.setText("Dear " + facultyName + ",\n\n" +
                         "Please find attached the weekly attendance report for the period " + 
                         weekStart + " to " + weekEnd + ".\n\n" +
                         "This report includes:\n" +
                         "• Total attendance records\n" +
                         "• Student-wise attendance summary\n" +
                         "• Session details and timestamps\n\n" +
                         "For any queries, please contact the system administrator.\n\n" +
                         "Best regards,\n" +
                         "Smart Attendance System");
            
            // Add Excel attachment
            helper.addAttachment("weekly-attendance-report-" + weekStart + ".xlsx", 
                               new jakarta.mail.util.ByteArrayDataSource(excelReport, 
                               "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            
            mailSender.send(message);
            logger.info("Weekly report email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send weekly report email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendEmergencyChangeEmailToParents(com.example.smartAttendence.domain.ClassroomSession session,
                                                com.example.smartAttendence.entity.EmergencySessionChange change) {
        // TODO: Implement parent email notification
        // This would involve:
        // 1. Getting all parent emails for students in the session
        // 2. Sending bulk email with appropriate content
        logger.info("Emergency change notification to parents - TODO: Implement parent email retrieval");
    }

    private String buildChangeDetails(com.example.smartAttendence.entity.EmergencySessionChange change) {
        StringBuilder details = new StringBuilder();
        
        if (change.getOriginalFacultyId() != null && change.getNewFacultyId() != null) {
            details.append("• Faculty: ").append(change.getOriginalFacultyId())
                   .append(" → ").append(change.getNewFacultyId()).append("\n");
        }
        
        if (change.getOriginalRoomId() != null && change.getNewRoomId() != null) {
            details.append("• Room: ").append(change.getOriginalRoomId())
                   .append(" → ").append(change.getNewRoomId()).append("\n");
        }
        
        if (change.getOriginalStartTime() != null && change.getNewStartTime() != null) {
            details.append("• Start Time: ").append(change.getOriginalStartTime())
                   .append(" → ").append(change.getNewStartTime()).append("\n");
        }
        
        if (change.getOriginalEndTime() != null && change.getNewEndTime() != null) {
            details.append("• End Time: ").append(change.getOriginalEndTime())
                   .append(" → ").append(change.getNewEndTime()).append("\n");
        }
        
        if (details.length() == 0) {
            details.append("• Session cancelled or other emergency action taken");
        }
        
        return details.toString();
    }
}
