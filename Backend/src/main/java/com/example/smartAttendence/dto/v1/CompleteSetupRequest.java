package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;

public record CompleteSetupRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId,
    
    @NotBlank(message = "Biometric signature is required")
    String biometricSignature,
    
    String phoneNumber,
    String registrationNumber,
    String section,
    String department,
    String academicYear
) {
}
