package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CRLRAssignmentRequest(
    @NotNull(message = "Student ID is required")
    UUID studentId,
    
    @NotNull(message = "Section ID is required")
    UUID sectionId,
    
    @NotNull(message = "Role type is required")
    CRLRAssignmentRequest.RoleType roleType,
    
    String academicYear,
    
    String semester,
    
    String notes,
    
    // Assignment duration
    Integer validForMonths,
    
    // Auto-renewal option
    Boolean autoRenew
) {
    public enum RoleType {
        CR,    // Class Representative
        LR     // Lab Representative
    }
    
    public CRLRAssignmentRequest {
        if (validForMonths == null) validForMonths = 6; // Default 6 months
        if (autoRenew == null) autoRenew = false;
    }
}
