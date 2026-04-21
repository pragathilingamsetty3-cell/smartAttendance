package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record EnhancedHeartbeatPing(
    @NotNull(message = "Student ID is required")
    UUID studentId,
    
    @NotNull(message = "Session ID is required")
    UUID sessionId,
    
    @NotNull(message = "Latitude is required")
    Double latitude,
    
    @NotNull(message = "Longitude is required")
    Double longitude,
    
    @NotNull(message = "Step count is required")
    Integer stepCount,
    
    @NotNull(message = "Acceleration X is required")
    Double accelerationX,
    
    @NotNull(message = "Acceleration Y is required")
    Double accelerationY,
    
    @NotNull(message = "Acceleration Z is required")
    Double accelerationZ,
    
    @NotNull(message = "Device moving status is required")
    Boolean isDeviceMoving,
    
    Instant timestamp,
    
    String deviceFingerprint,
    String biometricSignature, // 🔐 BIOMETRIC SECURITY
    
    // 🔋 BATTERY OPTIMIZATION FIELDS
    Integer batteryLevel,        // 0-100%
    Boolean isCharging,         // true/false
    Boolean isScreenOn,          // true/false
    String deviceState,          // "STATIONARY", "MOVING", "WALKING"
    Double gpsAccuracy,          // 🛰️ GPS Precision Data
    Long nextHeartbeatInterval,   // Dynamic interval in seconds
    String requestSignature,     // 🔐 HMAC-SHA256 Signature
    Long sequenceId              // 📈 Reliability: Packet Sequence ID
) {}
