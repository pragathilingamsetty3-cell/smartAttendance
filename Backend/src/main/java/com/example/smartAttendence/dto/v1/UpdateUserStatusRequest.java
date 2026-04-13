package com.example.smartAttendence.dto.v1;

import com.example.smartAttendence.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update user status with reason")
public record UpdateUserStatusRequest(
    
    @Schema(
        description = "New status for the user",
        example = "DROPPED_OUT",
        allowableValues = {"ACTIVE", "INACTIVE", "DROPPED_OUT", "SUSPENDED", "GRADUATED", "TRANSFERRED", "RESIGNED"}
    )
    UserStatus status,
    
    @Schema(
        description = "Reason for status change",
        example = "Student dropped out due to personal reasons"
    )
    @NotBlank(message = "Reason is required")
    String reason
) {}
