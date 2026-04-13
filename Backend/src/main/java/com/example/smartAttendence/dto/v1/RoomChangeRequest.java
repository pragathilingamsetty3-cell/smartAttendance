package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RoomChangeRequest(
    @NotNull(message = "Room ID is required")
    UUID roomId,
    
    RoomChangeType changeType,
    
    // For section-specific changes
    UUID sectionId,
    
    // For faculty-initiated changes
    UUID facultyId,
    
    // For pre-planned changes
    Instant scheduledTime,
    
    // For weekly swaps
    UUID swapWithRoomId,
    UUID swapWithSectionId,
    
    // Additional context
    String reason,
    
    // Notification preferences
    Boolean notifyStudents,
    Boolean notifyFaculty,
    Boolean notifyParents
) {
    public enum RoomChangeType {
        SUDDEN_CHANGE,        // Real-time QR scan
        PRE_PLANNED,          // Faculty schedules in advance
        WEEKLY_SWAP,          // Regular weekly room swapping
        EMERGENCY_MOVE        // Emergency relocation
    }
    
    // Default constructor values
    public RoomChangeRequest {
        if (notifyStudents == null) notifyStudents = true;
        if (notifyFaculty == null) notifyFaculty = true;
        if (notifyParents == null) notifyParents = false;
    }
}
