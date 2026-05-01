package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record OfflineSyncRequest(
    @NotEmpty(message = "Attendance records cannot be empty")
    List<OfflineAttendanceRecord> records
) {
    public record OfflineAttendanceRecord(
        @NotNull(message = "Session ID is required")
        String sessionId,

        @NotNull(message = "Student ID is required")
        String studentId,
        
        @NotNull(message = "Client timestamp is required")
        Instant clientTimestamp,
        
        Double locationLat,
        
        Double locationLng,
        
        String biometricSignature,
        
        @NotNull(message = "Device fingerprint is required")
        String deviceFingerprint
    ) {}
}
