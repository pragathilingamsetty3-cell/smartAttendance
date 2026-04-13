package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record QRRoomChangeRequest(
    @NotNull(message = "QR Room ID is required")
    UUID qrRoomId,
    
    // User who scanned QR (CR/LR/Faculty)
    @NotNull(message = "Scanner user ID is required")
    UUID scannerUserId,
    
    // Auto-detected by AI for CR/LR, selected by faculty
    UUID sectionId,
    
    // Change context
    String reason,
    
    // Emergency flag
    Boolean isEmergency,
    
    // Notification preferences
    Boolean notifyStudents,
    Boolean notifyFaculty,
    Boolean notifyParents
) {
    public QRRoomChangeRequest {
        if (isEmergency == null) isEmergency = false;
        if (notifyStudents == null) notifyStudents = true;
        if (notifyFaculty == null) notifyFaculty = true;
        if (notifyParents == null) notifyParents = false;
    }
}
