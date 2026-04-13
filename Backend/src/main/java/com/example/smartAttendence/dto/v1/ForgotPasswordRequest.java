package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequest(
    
    @NotBlank(message = "Email or phone number is required")
    String emailOrPhone,
    
    String method // EMAIL, PHONE - made optional to prevent validation errors
) {}
