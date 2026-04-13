package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.LoginRequest;
import com.example.smartAttendence.dto.v1.*;
import com.example.smartAttendence.entity.RefreshToken;
import com.example.smartAttendence.security.AdvancedInputValidator;
import com.example.smartAttendence.security.JwtUtil;
import com.example.smartAttendence.service.RefreshTokenService;
import com.example.smartAttendence.service.v1.UnifiedAuthService;
import com.example.smartAttendence.service.v1.AuthenticationService;
import com.example.smartAttendence.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthV1Controller {

    private final UnifiedAuthService unifiedAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SecurityUtils securityUtils;
    private final AdvancedInputValidator advancedInputValidator;

    public AuthV1Controller(
            UnifiedAuthService unifiedAuthService,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            SecurityUtils securityUtils,
            AdvancedInputValidator advancedInputValidator) {
        this.unifiedAuthService = unifiedAuthService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.securityUtils = securityUtils;
        this.advancedInputValidator = advancedInputValidator;
    }

    /**
     * Enhanced Login with First Login Detection
     */
    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("🔐 Login attempt for email: {}", request.getEmail());
            
            // 🔐 ENHANCED INPUT VALIDATION
            // Validate email
            AdvancedInputValidator.ValidationResult emailValidation = advancedInputValidator.validateEmail(request.getEmail());
            if (!emailValidation.isValid()) {
                log.warn("❌ Email validation failed: {}", emailValidation.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid email format: " + emailValidation.getErrorMessage()));
            }
            
            // Basic password check (no security validation for login - let auth service handle it)
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.warn("❌ Password is null or empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }
            
            log.info("✅ Input validation passed, attempting login...");
            AuthenticationService.LoginResult result = unifiedAuthService.login(request.getEmail(), request.getPassword());
            
            log.info("✅ Login successful for: {}", result.user().getEmail());

            // 🔐 ADVANCED ZERO-TRUST TOKEN GENERATION
            String deviceFingerprint = generateDeviceFingerprint(httpRequest);
            String sessionId = java.util.UUID.randomUUID().toString();
            String clientIP = getClientIP(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String geoLocation = getGeoLocation(clientIP);
            
            log.debug("🔐 Generating JWT token for user: {}", result.user().getEmail());
            log.debug("🔐 Device fingerprint: {}", deviceFingerprint);
            log.debug("🔐 Session ID: {}", sessionId);
            log.debug("🔐 Client IP: {}", clientIP);
            log.debug("🔐 User Agent: {}", userAgent);
            log.debug("🔐 Geo Location: {}", geoLocation);
            
            String accessToken = jwtUtil.generateToken(
                result.user().getEmail(), 
                result.user().getRole().toString(), 
                deviceFingerprint, 
                sessionId,
                clientIP,
                userAgent,
                geoLocation
            );
            
            log.debug("🔐 JWT token generated successfully");
            
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(result.user());
            log.debug("🔐 Refresh token created successfully");

            if (result.status() == AuthenticationService.LoginStatus.REQUIRE_SETUP) {
                log.debug("🔐 Building response for first login setup");
                Map<String, Object> response = Map.of(
                    "message", "First login detected. Setup required.",
                    "requiresFirstLoginSetup", true,
                    "accessToken", accessToken,
                    "refreshToken", refreshToken.getToken(),
                    "tokenType", "Bearer",
                    "expiresIn", 15 * 60,
                    "user", Map.of(
                        "id", result.user().getId(),
                        "email", result.user().getEmail(),
                        "name", result.user().getName(),
                        "role", result.user().getRole()
                    )
                );
                log.debug("🔐 Response built successfully for first login");
                return ResponseEntity.status(202).body(response);
            }

            // 🔐 Resolve Department UUID for Dynamic UI Support
            String rawDept = result.user().getDepartment();
            UUID deptUuid = securityUtils.resolveDepartmentUuid(rawDept);
            
            log.debug("🔐 Building response for successful login");
            Map<String, Object> response = Map.of(
                "message", "Login successful",
                "requiresFirstLoginSetup", false,
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "tokenType", "Bearer",
                "expiresIn", 15 * 60, // 15 minutes in seconds
                "user", Map.of(
                    "id", result.user().getId(),
                    "email", result.user().getEmail(),
                    "name", result.user().getName(),
                    "role", result.user().getRole(),
                    "department", deptUuid != null ? deptUuid.toString() : "",
                    "sectionId", result.user().getSectionId() != null ? result.user().getSectionId().toString() : ""
                )
            );
            log.debug("🔐 Response built successfully for login");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("❌ IllegalArgumentException: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ General Exception: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    /**
     * Step 3: Complete First Login Setup - Secure Token Flow
     */
    @PostMapping("/complete-setup")
    public ResponseEntity<?> completeSetup(@Valid @RequestBody CompleteSetupRequest request, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Authentication required"));
            }

            // 🔐 ENHANCED INPUT VALIDATION
            AdvancedInputValidator validator = new AdvancedInputValidator();
            
            // Validate device ID
            if (request.deviceId() != null) {
                AdvancedInputValidator.ValidationResult deviceValidation = validator.validateDeviceId(request.deviceId());
                if (!deviceValidation.isValid()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid device ID: " + deviceValidation.getErrorMessage()));
                }
            }
            
            // Validate biometric signature
            if (request.biometricSignature() != null) {
                AdvancedInputValidator.ValidationResult bioValidation = validator.validateBiometricSignature(request.biometricSignature());
                if (!bioValidation.isValid()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid biometric signature: " + bioValidation.getErrorMessage()));
                }
            }
            
            // Validate phone number
            if (request.phoneNumber() != null) {
                AdvancedInputValidator.ValidationResult phoneValidation = validator.validatePhoneNumber(request.phoneNumber());
                if (!phoneValidation.isValid()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid phone number: " + phoneValidation.getErrorMessage()));
                }
            }

            String email = auth.getName();
            unifiedAuthService.completeFirstLoginSetup(email, request);

            return ResponseEntity.ok(Map.of(
                "message", "Biometric setup completed successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Setup completion failed: " + e.getMessage()));
        }
    }

    /**
     * Change Password (Authenticated)
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Authentication required"));
            }

            // 🔐 ENHANCED INPUT VALIDATION
            AdvancedInputValidator validator = new AdvancedInputValidator();
            
            // Validate current password (basic check)
            if (request.currentPassword() == null || request.currentPassword().length() < 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Current password is required"));
            }
            
            // Validate new password
            AdvancedInputValidator.ValidationResult passwordValidation = validator.validatePassword(request.newPassword());
            if (!passwordValidation.isValid()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid new password: " + passwordValidation.getErrorMessage()));
            }
            
            // Check if passwords are the same
            if (request.currentPassword().equals(request.newPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "New password must be different from current password"));
            }
            
            // Validate password confirmation
            if (!request.newPassword().equals(request.confirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "New password and confirmation do not match"));
            }

            String email = auth.getName();
            unifiedAuthService.changePassword(email, request);

            return ResponseEntity.ok(Map.of(
                    "message", "Password changed successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Password change failed: " + e.getMessage()));
        }
    }

    /**
     * Refresh Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String refreshTokenStr = request.get("refreshToken");
            if (refreshTokenStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
            }

            RefreshToken token = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> {
                        log.warn("🚨 REFRESH FAILED: Token not found in database: {}", refreshTokenStr.substring(0, Math.min(8, refreshTokenStr.length())) + "...");
                        return new RuntimeException("Refresh token not found or expired");
                    });

            refreshTokenService.verifyExpiration(token);
            User user = token.getUser();

        // 🔐 ADVANCED ZERO-TRUST TOKEN GENERATION
        String deviceFingerprint = generateDeviceFingerprint(httpRequest);
        String sessionId = java.util.UUID.randomUUID().toString();
        String clientIP = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String geoLocation = getGeoLocation(clientIP);
        
        log.info("🔐 REFRESH SUCCESS: Generating new access token for: {}", user.getEmail());
        
        String newToken = jwtUtil.generateToken(
            user.getEmail(), 
            user.getRole().toString(), 
            deviceFingerprint, 
            sessionId,
            clientIP,
            userAgent,
            geoLocation
        );

        return ResponseEntity.ok(Map.of(
                "token", newToken,
                "refreshToken", refreshTokenStr
        ));
        } catch (RuntimeException e) {
            log.error("🚨 REFRESH ERROR (401): {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }

    /**
     * Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        // 🚀 THE FIX: Catch the null token before it crashes the server!
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active session found or already logged out."));
        }

        // 1. Get the currently logged-in user's email from the active JWT
        String email = auth.getName();

        // 2. Find the user in the database using UnifiedAuthService
        User user = unifiedAuthService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Destroy their refresh token in the database!
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully. Tokens destroyed."));
    }

    // ========== PASSWORD MANAGEMENT ENDPOINTS ==========

    /**
     * Forgot password - send OTP
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            // Set default method if not provided
            if (request.method() == null || request.method().trim().isEmpty()) {
                // Auto-detect method based on emailOrPhone format
                String method = request.emailOrPhone().contains("@") ? "EMAIL" : "PHONE";
                // Create a new request with the method
                request = new ForgotPasswordRequest(request.emailOrPhone(), method);
            }
            
            unifiedAuthService.forgotPassword(request);
            return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your email",
                "emailOrPhone", request.emailOrPhone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    /**
     * Reset password with OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordWithOTPRequest request) {
        try {
            // Additional validation for password confirmation
            if (!request.newPassword().equals(request.confirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "New password and confirmation do not match"));
            }
            
            unifiedAuthService.resetPasswordWithOTP(request);
            return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Password reset failed: " + e.getMessage()));
        }
    }

    // 🔐 SECURITY HELPER METHODS
    private String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            String accept = request.getHeader("Accept");
            
            String fingerprintData = userAgent + "|" + acceptLanguage + "|" + acceptEncoding + "|" + accept;
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintData.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return "unknown-device";
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    private String getGeoLocation(String ip) {
        // 🔐 SIMPLIFIED GEOLOCATION
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "CAMPUS";
        }
        return "EXTERNAL";
    }
}
