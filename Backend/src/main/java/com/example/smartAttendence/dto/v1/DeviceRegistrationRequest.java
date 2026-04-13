package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRegistrationRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId,
    
    @NotBlank(message = "Device fingerprint is required")
    String deviceFingerprint,
    
    String biometricPublicKey,
    
    String biometricType, // FACE, FINGERPRINT
    
    String appVersion,
    
    String osVersion
) {}
