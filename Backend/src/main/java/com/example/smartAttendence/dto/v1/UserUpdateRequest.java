package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UserUpdateRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Role is required")
    String role,
    
    String department,
    
    UUID sectionId,
    
    String status,
    
    String studentMobile,
    
    String parentMobile,
    
    Integer semester,
    
    String totalAcademicYears
) {}
