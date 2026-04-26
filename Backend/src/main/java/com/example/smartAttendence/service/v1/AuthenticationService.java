package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.CompleteSetupRequest;
import com.example.smartAttendence.entity.DeviceBinding;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.DeviceBindingRepository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserV1Repository userV1Repository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceBindingRepository deviceBindingRepository;
    private final SectionRepository sectionRepository;

    public AuthenticationService(
            UserV1Repository userV1Repository,
            PasswordEncoder passwordEncoder,
            DeviceBindingRepository deviceBindingRepository,
            SectionRepository sectionRepository) {
        this.userV1Repository = userV1Repository;
        this.passwordEncoder = passwordEncoder;
        this.deviceBindingRepository = deviceBindingRepository;
        this.sectionRepository = sectionRepository;
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
                    logger.warn("🚨 DEVICE LOCK VIOLATION: Device mismatch for student {}. Registered: {}, Incoming: {}", 
                        email, user.getDeviceId(), incomingDeviceId);
                    throw new IllegalArgumentException("Device not authorized. This account is locked to a different device. Expected [" + user.getDeviceId().substring(0, Math.min(8, user.getDeviceId().length())) + "...]");
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
        return userV1Repository.findByEmailIgnoreCase(email);
    }

    /**
     * Complete first login setup and bind device
     */
    @Transactional
    public User completeSetup(CompleteSetupRequest request, String email) {
        logger.info("🔑 START: completeSetup process for email: {}", email);
        
        User user = userV1Repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        logger.info("👤 Found user: {} (ID: {}) with current biometric state: {}", 
            email, user.getId(), (user.getBiometricSignature() != null ? "BOUND" : "UNBOUND"));

        // 🛡️ SECURITY LOCK: Block any biometric update if already registered. 
        if (user.getBiometricSignature() != null && !user.getBiometricSignature().isEmpty()) {
            logger.warn("🚨 SECURITY VIOLATION: User {} attempted to re-setup locked biometric", email);
            throw new IllegalArgumentException("Biometric already registered. Contact administrator to reset hardware binding.");
        }

        // Validate device ownership
        if (request.deviceId() != null) {
            logger.info("📱 Validating device ID ownership: {}", request.deviceId());
            Optional<DeviceBinding> existingBinding = deviceBindingRepository.findByDeviceId(request.deviceId());
            if (existingBinding.isPresent()) {
                DeviceBinding binding = existingBinding.get();
                if (binding.getUser() != null && !binding.getUser().getId().equals(user.getId())) {
                    logger.error("🚨 SETUP FAILED: Device {} already belongs to another user (ID: {})", 
                        request.deviceId(), binding.getUser().getId());
                    throw new IllegalArgumentException("This device is already registered to another student.");
                }
                logger.info("✅ Device already belongs to this user, updating existing binding.");
            }
        }

        // 4. Optionally Update Registration Info (Only if provided)
        if (request.registrationNumber() != null && !request.registrationNumber().isBlank()) {
            user.setRegistrationNumber(request.registrationNumber());
        }

        if (request.section() != null && !request.section().isBlank()) {
            Section section = sectionRepository.findByName(request.section())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid section: " + request.section()));
            user.setSection(section);
        }
        
        if (request.department() != null && !request.department().isBlank()) {
            user.setDepartment(request.department());
        }
        
        if (request.academicYear() != null && !request.academicYear().isBlank()) {
            user.setTotalAcademicYears(request.academicYear());
        }    

        // 📝 Update User Record
        try {
            logger.info("📝 Updating user biometric and device fields...");
            user.setDeviceId(request.deviceId());
            user.setBiometricSignature(request.biometricSignature());
            user.setIsTemporaryPassword(false);
            user.setFirstLogin(false);
            user.setDeviceRegisteredAt(Instant.now());
            
            // Generate Secret Key (Non-blocking)
            java.security.SecureRandom sr = new java.security.SecureRandom();
            byte[] secretKeyBytes = new byte[32];
            sr.nextBytes(secretKeyBytes);
            user.setSecretKey(java.util.Base64.getEncoder().encodeToString(secretKeyBytes));

            // Truncate phone number to prevent VARCHAR(20) overflow
            if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
                String mobile = request.phoneNumber().trim();
                user.setStudentMobile(mobile.length() > 20 ? mobile.substring(0, 20) : mobile);
                logger.info("📱 Mobile number saved (truncated to 20): {}", user.getStudentMobile());
            }

            user = userV1Repository.saveAndFlush(user);
            logger.info("✅ User saved successfully.");
        } catch (Exception e) {
            logger.error("🔥 CRITICAL ERROR during user save: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while saving profile: " + e.getMessage());
        }

        // 🛡️ Device Binding Management
        try {
            logger.info("⛓️ [DIAGNOSTIC] Managing device binding for user ID: {} with device: {}", user.getId(), request.deviceId());
            
            DeviceBinding deviceBinding = deviceBindingRepository.findByUser(user)
                    .orElseGet(() -> {
                        logger.info("🆕 [DIAGNOSTIC] Creating new device binding record");
                        return new DeviceBinding();
                    });
            
            deviceBinding.setUser(user);
            deviceBinding.setDeviceId(request.deviceId());
            deviceBinding.setDeviceFingerprint("BIND_" + UUID.randomUUID().toString().substring(0, 8));
            deviceBinding.setIsActive(true);
            deviceBinding.setRegisteredAt(Instant.now());
            deviceBinding.setLastUsedAt(Instant.now());
            
            deviceBindingRepository.save(deviceBinding);
            logger.info("✅ [DIAGNOSTIC] Device binding saved successfully");
        } catch (Exception e) {
            logger.error("🔥 [DIAGNOSTIC] CRITICAL ERROR during device binding save: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while binding device: " + e.getMessage());
        }
        
        logger.info("🎉 SUCCESS: completeSetup finished for user: {}", email);
        return user;
    }

    /**
     * Change password (authenticated)
     */
    public void changePassword(String email, com.example.smartAttendence.dto.v1.ChangePasswordRequest request) {
        Optional<User> userOpt = userV1Repository.findByEmailIgnoreCase(email);
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
        user.setFirstLogin(false); // ⚡ BREAK LOOP: Ensure they aren't asked for first-login setup again after manual change

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
