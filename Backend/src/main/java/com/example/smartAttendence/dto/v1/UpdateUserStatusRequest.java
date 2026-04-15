package com.example.smartAttendence.dto.v1;

import com.example.smartAttendence.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
    UserStatus status,
    
    @NotBlank(message = "Reason is required")
    String reason
) {}
