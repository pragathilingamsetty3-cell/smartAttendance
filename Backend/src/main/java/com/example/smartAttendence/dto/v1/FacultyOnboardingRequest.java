package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Faculty Onboarding Request DTO
 * Used for onboarding new faculty via AdminV1Controller
 */
public record FacultyOnboardingRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String registrationNumber,

        // Department field - no validation for dynamic handling
        String department,

        String facultyMobile
) {}
