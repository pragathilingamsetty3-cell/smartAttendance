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
import com.example.smartAttendence.service.v1.PasswordResetService;
import com.example.smartAttendence.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthV1Controller {

    private final UnifiedAuthService unifiedAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SecurityUtils securityUtils;
    private final AdvancedInputValidator advancedInputValidator;
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    public AuthV1Controller(
            UnifiedAuthService unifiedAuthService,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            SecurityUtils securityUtils,
            AdvancedInputValidator advancedInputValidator,
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService) {
        this.unifiedAuthService = unifiedAuthService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.securityUtils = securityUtils;
        this.advancedInputValidator = advancedInputValidator;
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("[AUTH] Login attempt for email: {}", request.getEmail());
            AuthenticationService.LoginResult result = unifiedAuthService.login(request.getEmail(), request.getPassword(), extractDeviceId(httpRequest));
            
            String tokenFingerprint = generateDeviceFingerprint(httpRequest);
            String accessToken = jwtUtil.generateToken(result.user().getEmail(), result.user().getRole().toString(), tokenFingerprint, UUID.randomUUID().toString(), getClientIP(httpRequest), httpRequest.getHeader("User-Agent"), "EXTERNAL");
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(result.user());

            if (result.status() == AuthenticationService.LoginStatus.REQUIRE_SETUP) {
                Map<String, Object> response = new HashMap<>();
                response.put("requiresFirstLoginSetup", true);
                response.put("message", "First login detected. Setup required.");
                response.put("accessToken", accessToken);
                response.put("refreshToken", refreshToken.getToken());
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", result.user().getId());
                userData.put("email", result.user().getEmail());
                userData.put("role", result.user().getRole());
                userData.put("name", result.user().getName() != null ? result.user().getName() : "");
                userData.put("registrationNumber", result.user().getRegistrationNumber());
                userData.put("department", result.user().getDepartment());
                userData.put("firstLogin", result.user().isFirstLogin());
                userData.put("deviceId", result.user().getDeviceId());
                userData.put("biometricSignature", result.user().getBiometricSignature());
                userData.put("isTemporaryPassword", result.user().getIsTemporaryPassword());
                userData.put("secretKey", result.user().getSecretKey());
                
                response.put("user", userData);
                return ResponseEntity.status(202).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken.getToken());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", result.user().getId());
            userData.put("email", result.user().getEmail());
            userData.put("role", result.user().getRole());
            userData.put("name", result.user().getName() != null ? result.user().getName() : "");
            userData.put("registrationNumber", result.user().getRegistrationNumber());
            userData.put("department", result.user().getDepartment());
            userData.put("firstLogin", result.user().isFirstLogin());
            userData.put("deviceId", result.user().getDeviceId());
            userData.put("biometricSignature", result.user().getBiometricSignature());
            userData.put("isTemporaryPassword", result.user().getIsTemporaryPassword());
            userData.put("secretKey", result.user().getSecretKey());
            
            response.put("user", userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AUTH] Login Exception: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/complete-setup")
    public ResponseEntity<?> completeSetup(@Valid @RequestBody CompleteSetupRequest request, Authentication auth) {
        log.info("[AUTH] START: complete-setup request received for: {}", auth != null ? auth.getName() : "UNAUTHENTICATED");
        try {
            if (auth == null || !auth.isAuthenticated()) {
                log.warn("[AUTH] Setup attempt without authentication context");
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            
            log.info("[AUTH] Calling authenticationService.completeSetup for user: {}", auth.getName());
            User user = authenticationService.completeSetup(request, auth.getName());
            if (user == null) {
                log.error("[AUTH] Service returned null user for setup: {}", auth.getName());
                return ResponseEntity.status(500).body(Map.of("error", "Setup failed: Service returned invalid user state"));
            }

            log.info("[AUTH] Biometric data saved for {}. Generating fresh session...", auth.getName());

            // 🔐 CRITICAL: Generate new tokens after setup to ensure fresh session state
            String roleStr = user.getRole() != null ? user.getRole().toString() : "STUDENT";
            String accessToken = jwtUtil.generateToken(
                user.getEmail(), 
                roleStr,
                request.deviceId(),
                java.util.UUID.randomUUID().toString(),
                securityUtils.getClientIP(),
                securityUtils.getUserAgent(),
                "CAMPUS"
            );
            
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), request.deviceId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Biometric setup completed successfully");
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("role", roleStr);
            userData.put("firstLogin", false);
            userData.put("isTemporaryPassword", false);
            userData.put("deviceId", user.getDeviceId());
            userData.put("biometricSignature", user.getBiometricSignature());
            userData.put("needsSetup", false);
            
            response.put("user", userData);
            response.put("secretKey", user.getSecretKey() != null ? user.getSecretKey() : "NOT_GENERATED");
            
            log.info("[AUTH] SUCCESS: complete-setup finished for {}. Session updated.", auth.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[AUTH] Setup validation failed for {}: {}", auth.getName(), e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[AUTH] FATAL ERROR during setup for {}: ", auth != null ? auth.getName() : "UNKNOWN", e);
            return ResponseEntity.status(500).body(Map.of("error", "Setup completion failed: " + (e.getMessage() != null ? e.getMessage() : "Internal Error")));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        log.info("[AUTH] Change password request for: {}", auth != null ? auth.getName() : "UNAUTHENTICATED");
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            unifiedAuthService.changePassword(auth.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("[AUTH] Change password validation failed: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[AUTH] FATAL ERROR during password change: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("[AUTH] Forgot password request for: {}", request.emailOrPhone());
        try {
            passwordResetService.forgotPassword(request);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception e) {
            log.error("[AUTH] Forgot password error: {}", e.getMessage());
            Map<String, Object> errResponse = new HashMap<>();
            errResponse.put("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred");
            return ResponseEntity.status(500).body(errResponse);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordWithOTPRequest request) {
        log.info("[AUTH] Reset password request with OTP for: {}", request.emailOrPhone());
        try {
            passwordResetService.resetPasswordWithOTP(request);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            log.error("[AUTH] Reset password error: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    @Transactional
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String refreshTokenStr = request.get("refreshToken");
            RefreshToken token = refreshTokenService.findByToken(refreshTokenStr).orElseThrow(() -> new RuntimeException("Invalid refresh token"));
            refreshTokenService.verifyExpiration(token);
            String newToken = jwtUtil.generateToken(token.getUser().getEmail(), token.getUser().getRole().toString(), generateDeviceFingerprint(httpRequest), UUID.randomUUID().toString(), getClientIP(httpRequest), httpRequest.getHeader("User-Agent"), "EXTERNAL");
            return ResponseEntity.ok(Map.of("token", newToken, "refreshToken", refreshTokenStr));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            unifiedAuthService.getUserByEmail(auth.getName()).ifPresent(refreshTokenService::deleteByUser);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Fingerprint");
        return deviceId != null ? deviceId : "UNKNOWN";
    }

    private String generateDeviceFingerprint(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return "SECURE_" + (ua != null ? ua.hashCode() : 0);
    }

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}
