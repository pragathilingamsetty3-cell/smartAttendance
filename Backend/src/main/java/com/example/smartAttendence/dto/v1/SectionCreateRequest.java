package com.example.smartAttendence.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Request to create a new section")
public record SectionCreateRequest(
    
    @Schema(
        example = "A",
        description = "Name/identifier of the section",
        required = true
    )
    String name,
    
    @Schema(
        example = "550e8400-e29b-41d4-a716-446655440000",
        description = "ID of the department this section belongs to",
        required = true
    )
    UUID departmentId,

    @Schema(
        example = "B.Tech",
        description = "Academic program for the section",
        required = true
    )
    String program,

    @Schema(
        example = "60",
        description = "Student capacity for the section",
        required = true
    )
    Integer capacity,

    @Schema(
        example = "2024",
        description = "Target batch / intake year for the section",
        required = false
    )
    Integer batchYear,

    @Schema(
        example = "4",
        description = "Total number of academic years allocated for the program",
        required = false
    )
    String totalAcademicYears,

    @Schema(
        example = "1",
        description = "Current active semester for the section",
        required = false
    )
    Integer currentSemester,

    @Schema(
        example = "Standard Section for B.Tech CSE",
        description = "Optional description for the section",
        required = false
    )
    String description,

    @Schema(
        example = "true",
        description = "Whether the section is currently active",
        required = false
    )
    Boolean isActive
) {}
