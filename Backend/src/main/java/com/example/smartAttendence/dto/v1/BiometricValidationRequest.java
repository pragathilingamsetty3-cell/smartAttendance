package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;

public record BiometricValidationRequest(
    @NotBlank(message = "Biometric data is required")
    String biometricData,
    
    @NotBlank(message = "Biometric type is required")
    String biometricType // FACE, FINGERPRINT
) {}
