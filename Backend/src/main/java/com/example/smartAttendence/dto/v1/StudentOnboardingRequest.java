package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

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

        // Section ID - no validation for dynamic handling
        String sectionId,

        // Department field - no validation for dynamic handling
        String department,

        @Email(message = "Invalid parent email format")
        String parentEmail,

        String parentMobile,

        String studentMobile,

        @NotBlank(message = "Academic year is required")
        @Schema(description = "Total academic years for the course (e.g., 4 for 4-year course, 2 for 2-year course)", example = "4")
        String totalAcademicYears,
        
        @Schema(
            description = "The semester for the student (defaults to 1 for new students)",
            example = "1"
        )
        Integer semester
) {}
