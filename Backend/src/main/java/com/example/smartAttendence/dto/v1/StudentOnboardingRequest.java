package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Student Onboarding Request DTO
 * Used for onboarding new students via AdminV1Controller
 */
public record StudentOnboardingRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Registration number is required")
        String registrationNumber,

        String sectionId,
        String department,

        @Email(message = "Invalid parent email format")
        String parentEmail,

        String parentMobile,
        String studentMobile,

        @NotBlank(message = "Academic year is required")
        String totalAcademicYears,
        
        Integer semester
) {}
