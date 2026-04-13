package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record SubstituteClaimRequest(
    @NotNull(message = "Session ID is required")
    UUID sessionId,
    
    @NotNull(message = "Substitute faculty ID is required") 
    UUID substituteFacultyId,
    
    @NotBlank(message = "Reason for substitution is required")
    String substitutionReason,
    
    String originalFacultyNotes,
    
    // Emergency override - skip normal approval workflow
    Boolean emergencyOverride,
    
    // Time-bound substitution
    Instant substitutionStart,
    Instant substitutionEnd,
    
    // Location changes if any
    UUID newRoomId,
    
    Boolean notifyStudents,
    Boolean notifyDepartment
) {}
