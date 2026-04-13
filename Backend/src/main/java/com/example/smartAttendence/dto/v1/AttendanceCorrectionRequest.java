package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 🤖 AI FEEDBACK LEARNING DTO
 * Used when Faculty corrects a manual attendance entry
 */
public record AttendanceCorrectionRequest(
    @NotNull(message = "Record ID is required")
    UUID recordId,
    
    @NotBlank(message = "New status is required")
    String newStatus,
    
    String reason,
    
    @NotNull(message = "Corrector ID is required")
    UUID correctorId
) {}
