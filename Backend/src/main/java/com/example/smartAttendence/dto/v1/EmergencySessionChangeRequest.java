package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record EmergencySessionChangeRequest(
    @NotNull(message = "Session ID is required")
    UUID sessionId,
    
    @NotBlank(message = "Change reason is required")
    String reason,
    
    EmergencyChangeType changeType,
    
    // For faculty change
    UUID newFacultyId,
    
    // For room change
    UUID newRoomId,
    
    // For time change
    Instant newStartTime,
    Instant newEndTime,
    
    // Additional context
    String adminNotes,
    
    Boolean notifyStudents,
    Boolean notifyParents
) {
    public enum EmergencyChangeType {
        FACULTY_SUBSTITUTION,
        ROOM_CHANGE,
        TIME_CHANGE,
        CANCELLATION,
        MERGE_SESSIONS
    }
}
