package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BulkPromotionRequest(
    @NotEmpty(message = "Student IDs list cannot be empty")
    List<UUID> studentIds,
    
    @NotNull(message = "Target section ID is required")
    UUID targetSectionId,
    
    Boolean autoIncrementSemester
) {}
