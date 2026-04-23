package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.ForgotPasswordRequest;
import com.example.smartAttendence.dto.v1.ResetPasswordRequest;
import com.example.smartAttendence.dto.v1.ResetPasswordWithOTPRequest;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    private final UserV1Repository userV1Repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(
            UserV1Repository userV1Repository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userV1Repository = userV1Repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Handle forgot password request - works for ALL users
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        String emailOrPhone = request.emailOrPhone().trim();
        Optional<User> userOpt;

        // Find user by email or phone
        if (emailOrPhone.contains("@")) {
            userOpt = userV1Repository.findByEmailIgnoreCase(emailOrPhone);
        } else {
            // Search by mobile number for all user types (students, faculty, admin)
            userOpt = userV1Repository.findAll().stream()
                    .filter(u -> emailOrPhone.equals(u.getStudentMobile()))
                    .findFirst();
        }

        if (userOpt.isEmpty()) {
            // Don't reveal if user exists or not for security
            logger.warn("Password reset requested for non-existent email/phone: {}", emailOrPhone);
            return;
        }

        User user = userOpt.get();
        
        // Log which user type is being processed (for debugging)
        logger.info("Processing forgot password for: {} - {}", user.getRole(), user.getEmail());
        
        String otp = generateOTP();
        user.setOtpCode(otp);
        user.setOtpExpiry(Instant.now().plusSeconds(300)); // 5 minutes
        userV1Repository.save(user);

        // Send OTP via email or SMS - works for ALL user types
        try {
            if (request.method().equals("EMAIL") && user.getEmail() != null) {
                emailService.sendPasswordResetOTP(user.getEmail(), user.getName(), otp);
                logger.info("OTP sent via email to {} for user type: {}", user.getEmail(), user.getRole());
            } else if (request.method().equals("PHONE") && user.getStudentMobile() != null) {
                // TODO: Implement SMS service
                sendSMSOTP(user.getStudentMobile(), otp);
                logger.info("OTP sent via SMS to {} for user type: {}", user.getStudentMobile(), user.getRole());
            }
        } catch (Exception e) {
            logger.error("Failed to send OTP: {}", e.getMessage());
        }
    }

    /**
     * Reset password with OTP
     */
    public void resetPasswordWithOTP(ResetPasswordWithOTPRequest request) {
        String emailOrPhone = request.emailOrPhone().trim();
        Optional<User> userOpt;

        if (emailOrPhone.contains("@")) {
            userOpt = userV1Repository.findByEmailIgnoreCase(emailOrPhone);
        } else {
            userOpt = userV1Repository.findAll().stream()
                    .filter(u -> emailOrPhone.equals(u.getStudentMobile()))
                    .findFirst();
        }

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or OTP");
        }

        User user = userOpt.get();

        // Validate OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.otp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        // Update password and clear OTP
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setIsTemporaryPassword(false);

        userV1Repository.save(user);

        // Send confirmation
        emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getName());

        logger.info("Password reset completed for: {}", user.getEmail());
    }

    /**
     * Send OTP for password reset
     */
    public void sendPasswordResetOtp(ForgotPasswordRequest request) {
        String emailOrMobile = request.emailOrPhone();
        Optional<User> userOpt;

        // Try finding by email first
        userOpt = userV1Repository.findByEmailIgnoreCase(emailOrMobile);
        
        // If not found, try by mobile
        if (userOpt.isEmpty()) {
            userOpt = userV1Repository.findAll().stream()
                    .filter(u -> emailOrMobile.equals(u.getStudentMobile()) || emailOrMobile.equals(u.getParentMobile()))
                    .findFirst();
        }

        if (userOpt.isEmpty()) {
            // Don't reveal if user exists or not
            logger.info("Password reset requested for non-existent user: {}", emailOrMobile);
            return;
        }

        User user = userOpt.get();
        String otp = generateOTP();
        Instant otpExpiry = Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60);

        user.setOtpCode(otp);
        user.setOtpExpiry(otpExpiry);
        userV1Repository.save(user);

        // Send OTP via email or SMS
        if (emailOrMobile.contains("@")) {
            emailService.sendPasswordResetOtp(emailOrMobile, otp);
        } else {
            emailService.sendOTPSMS(emailOrMobile, otp);
        }

        logger.info("Password reset OTP sent for: {}", emailOrMobile);
    }

    /**
     * Reset password with OTP
     */
    public void resetPassword(ResetPasswordRequest request) {
        Optional<User> userOpt = userV1Repository.findByEmailIgnoreCase(request.email());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or OTP");
        }

        User user = userOpt.get();

        // Validate OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.otp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        // Update password and clear OTP
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setIsTemporaryPassword(false);

        userV1Repository.save(user);

        // Send confirmation
        emailService.sendPasswordChangeConfirmation(request.email(), user.getName());

        logger.info("Password reset completed for: {}", request.email());
    }

    // ========== HELPER METHODS ==========

    private String generateOTP() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private void sendSMSOTP(String phoneNumber, String otp) {
        // TODO: Implement SMS service integration
        logger.info("SMS OTP would be sent to {}: {}", phoneNumber, otp);
    }
}
