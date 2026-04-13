package com.example.smartAttendence.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request for bulk promotion of students to new section and academic year")
public record BulkPromotionRequest(
    
    @Schema(
        description = "List of student IDs to promote",
        example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]"
    )
    @NotEmpty(message = "Student IDs list cannot be empty")
    List<UUID> studentIds,
    
    @Schema(
        description = "The target section ID for promotion",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @NotNull(message = "Target section ID is required")
    UUID targetSectionId,
    
    @Schema(
        description = "Whether to automatically increment semester by 1",
        example = "true"
    )
    Boolean autoIncrementSemester
) {}
