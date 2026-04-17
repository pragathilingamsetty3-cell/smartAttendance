package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.CompleteSetupRequest;
import com.example.smartAttendence.entity.DeviceBinding;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.DeviceBindingRepository;
import com.example.smartAttendence.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserV1Repository userV1Repository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceBindingRepository deviceBindingRepository;

    public AuthenticationService(
            UserV1Repository userV1Repository,
            PasswordEncoder passwordEncoder,
            DeviceBindingRepository deviceBindingRepository) {
        this.userV1Repository = userV1Repository;
        this.passwordEncoder = passwordEncoder;
        this.deviceBindingRepository = deviceBindingRepository;
    }

    /**
     * Enhanced login that detects temporary password and enforces device locking
     * Device ID comes from request headers, extracted by controller
     */
    public LoginResult login(String email, String password, String incomingDeviceId) {
        logger.info("🔐 Login attempt for email: {}", email);
        
        Optional<User> userOpt = userV1Repository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            logger.warn("❌ User not found: {}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userOpt.get();
        logger.info("👤 User found: {} with password format: {}", email, 
            user.getPassword().startsWith("$2") ? "BCrypt" : "Plain text");
        
        // Handle both plain text and BCrypt passwords for testing
        boolean passwordValid = false;
        String storedPassword = user.getPassword();
        
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // BCrypt format
            passwordValid = passwordEncoder.matches(password, storedPassword);
            logger.info("🔑 BCrypt validation result: {}", passwordValid);
        } else {
            // Plain text format (for testing)
            passwordValid = password.equals(storedPassword);
            logger.info("🔑 Plain text validation result: {} | Input: '{}' | Stored: '{}'", 
                passwordValid, password, storedPassword);
        }
        
        if (!passwordValid) {
            logger.warn("❌ Invalid password for user: {}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }

        logger.info("✅ Password verified for: {}", email);

        // --- 🛡️ AI SECURITY: DEVICE LOCKING SYSTEM ---
        com.example.smartAttendence.enums.Role role = user.getRole();
        if (role == com.example.smartAttendence.enums.Role.STUDENT || 
            role == com.example.smartAttendence.enums.Role.CR || 
            role == com.example.smartAttendence.enums.Role.LR) {
            // DEVICE LOCK ENFORCEMENT: If student has registered device, must login from same device
            if (user.getDeviceId() != null) {
                logger.info("🛡️ DEVICE LOCK: Student has registered device: {}", user.getDeviceId());
                
                // Validate incoming device matches registered device
                if (incomingDeviceId == null || incomingDeviceId.isEmpty()) {
                    logger.warn("🚨 DEVICE LOCK VIOLATION: No device ID in login request for locked device student: {}", email);
                    throw new IllegalArgumentException("Device locked. Cannot login from unknown device. Contact admin.");
                }
                
                if (!user.getDeviceId().equals(incomingDeviceId)) {
                    logger.warn("🚨 DEVICE LOCK VIOLATION: Device mismatch for student {}. Expected: {}, Got: {}", 
                        email, user.getDeviceId(), incomingDeviceId);
                    throw new IllegalArgumentException("Device not authorized. This account is locked to a different device. Contact admin to reset device.");
                }
                
                // Also validate DeviceBinding is active
                Optional<DeviceBinding> deviceBindingOpt = deviceBindingRepository.findByUserIdAndDeviceId(user.getId(), user.getDeviceId());
                if (deviceBindingOpt.isPresent()) {
                    DeviceBinding binding = deviceBindingOpt.get();
                    if (!binding.getIsActive()) {
                        logger.warn("🚨 DEVICE LOCK VIOLATION: Device binding is inactive/revoked for student: {}", email);
                        throw new IllegalArgumentException("Device has been locked by admin. Contact admin to reset device.");
                    }
                    logger.info("✅ DEVICE LOCK: Device binding verified as active for student: {}", email);
                } else {
                    logger.warn("⚠️ Device binding not found for student {}, but device ID is set", email);
                }
            } else {
                logger.info("📱 DEVICE LOCK: Device not yet locked. Student can register device on first access.");
            }
        }

        // Check if using temporary password
        if (user.getIsTemporaryPassword()) {
            return new LoginResult(user, LoginStatus.REQUIRE_SETUP, null, null);
        }

        logger.info("✅ Login successful for: {}", email);
        return new LoginResult(user, LoginStatus.SUCCESS, null, null);
    }

    /**
     * Overload for backward compatibility
     */
    public LoginResult login(String email, String password) {
        return login(email, password, null);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userV1Repository.findByEmail(email);
    }

    /**
     * Complete first login setup and bind device
     */
    public User completeSetup(CompleteSetupRequest request) {
        // Extract user identity from JWT token
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required");
        }
        
        String email = auth.getName();
        Optional<User> userOpt = userV1Repository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsTemporaryPassword()) {
            throw new IllegalArgumentException("Setup already completed");
        }

        // Validate device doesn't already belong to another user
        if (deviceBindingRepository.existsByDeviceId(request.deviceId())) {
            Optional<DeviceBinding> existingBinding = deviceBindingRepository.findByDeviceId(request.deviceId());
            if (existingBinding.isPresent() && !existingBinding.get().getUser().getId().equals(user.getId())) {
                logger.warn("🚨 SETUP VIOLATION: Device already registered to another user: {}", request.deviceId());
                throw new IllegalArgumentException("Device is already registered to another account.");
            }
        }

        // Update biometric and device data
        user.setDeviceId(request.deviceId());
        user.setBiometricSignature(request.biometricSignature());
        user.setIsTemporaryPassword(false);
        user.setFirstLogin(false);

        user = userV1Repository.save(user);

        // Create DeviceBinding record for audit trail and admin operations
        DeviceBinding deviceBinding = new DeviceBinding();
        deviceBinding.setUser(user);
        deviceBinding.setDeviceId(request.deviceId());
        deviceBinding.setDeviceFingerprint(request.biometricSignature()); // Use biometric signature as fingerprint
        deviceBinding.setDeviceName("Mobile Device");
        deviceBinding.setDeviceType("MOBILE");
        deviceBinding.setRegisteredAt(Instant.now());
        deviceBinding.setIsActive(true);

        deviceBindingRepository.save(deviceBinding);

        logger.info("✅ DEVICE LOCK ACTIVATED: Biometric setup completed and device locked for user: {}", email);
        logger.info("📱 Device locked to: {} | Student can now only login from this device", request.deviceId());
        return user;
    }

    /**
     * Change password (authenticated)
     */
    public void changePassword(String email, com.example.smartAttendence.dto.v1.ChangePasswordRequest request) {
        Optional<User> userOpt = userV1Repository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();

        // Validate current password (handle both plain text and BCrypt)
        boolean currentPasswordValid = false;
        String storedPassword = user.getPassword();
        
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // BCrypt format
            currentPasswordValid = passwordEncoder.matches(request.currentPassword(), storedPassword);
        } else {
            // Plain text format (for testing)
            currentPasswordValid = request.currentPassword().equals(storedPassword);
        }
        
        if (!currentPasswordValid) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setIsTemporaryPassword(false);

        userV1Repository.save(user);

        // Send confirmation
        // emailService.sendPasswordChangeConfirmation(email, user.getName());

        logger.info("Password changed for: {}", email);
    }

    // ========== DATA RECORDS ==========

    public record LoginResult(User user, LoginStatus status, String message, String errorCode) {}
    public enum LoginStatus {
        SUCCESS,
        REQUIRE_SETUP,
        INVALID_CREDENTIALS
    }
}
