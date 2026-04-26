package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Unified AuthService - Facade for authentication operations
 * Delegates to specialized services for better separation of concerns
 */
@Service
public class UnifiedAuthService {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    public UnifiedAuthService(
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
    }

    // ========== AUTHENTICATION DELEGATION ==========
    
    public AuthenticationService.LoginResult login(String email, String password) {
        return authenticationService.login(email, password);
    }

    public AuthenticationService.LoginResult login(String email, String password, String deviceId) {
        return authenticationService.login(email, password, deviceId);
    }

    public Optional<User> getUserByEmail(String email) {
        return authenticationService.getUserByEmail(email);
    }

    public User completeSetup(CompleteSetupRequest request, String email) {
        return authenticationService.completeSetup(request, email);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        authenticationService.changePassword(email, request);
    }

    // ========== PASSWORD RESET DELEGATION ==========

    public void forgotPassword(ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request);
    }

    public void resetPasswordWithOTP(ResetPasswordWithOTPRequest request) {
        passwordResetService.resetPasswordWithOTP(request);
    }

    public void sendPasswordResetOtp(ForgotPasswordRequest request) {
        passwordResetService.sendPasswordResetOtp(request);
    }

    public void resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    // ========== LEGACY METHODS FOR BACKWARD COMPATIBILITY ==========
    
    /**
     * Complete first login setup - delegated to AuthenticationService
     */
    public void completeFirstLoginSetup(String email, CompleteSetupRequest request) {
        authenticationService.completeSetup(request, email);
    }
}
