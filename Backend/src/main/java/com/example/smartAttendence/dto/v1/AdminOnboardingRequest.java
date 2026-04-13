package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin Onboarding Request DTO
 * Used for onboarding new admin users via AdminV1Controller
 */
public record AdminOnboardingRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String registrationNumber,

        @NotBlank(message = "Department is required")
        String department,

        @NotBlank(message = "Role is required")
        String role
) {}
