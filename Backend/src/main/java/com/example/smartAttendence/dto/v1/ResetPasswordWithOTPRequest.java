package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordWithOTPRequest(
    
    @NotBlank(message = "Email or phone number is required")
    String emailOrPhone,
    
    @NotBlank(message = "OTP is required")
    String otp,
    
    @NotBlank(message = "New password is required")
    String newPassword,
    
    @NotBlank(message = "Confirm password is required")
    String confirmPassword
) {}
