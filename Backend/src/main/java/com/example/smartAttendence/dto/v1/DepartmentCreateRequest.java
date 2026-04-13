package com.example.smartAttendence.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new department")
public record DepartmentCreateRequest(
    
    @Schema(
        example = "Computer Science",
        description = "Full name of the department",
        required = true
    )
    String name,
    
    @Schema(
        example = "CS",
        description = "Unique code for the department",
        required = true
    )
    String code,

    @Schema(
        example = "Academic Department for Computer Science Engineering",
        description = "Optional description for the department",
        required = false
    )
    String description,
    
    @Schema(
        example = "true",
        description = "Whether the department is active",
        required = false
    )
    Boolean isActive
) {}
