package com.example.smartAttendence.dto.v1;

import java.util.UUID;

/**
 * Response DTO for user onboarding operations
 * Contains the created user information and the temporary password
 */
public class OnboardingResponseDTO {
    
    private UUID userId;
    private String name;
    private String email;
    private String registrationNumber;
    private String role;
    private String temporaryPassword;
    
    public OnboardingResponseDTO() {}
    
    public OnboardingResponseDTO(UUID userId, String name, String email, 
                                String registrationNumber, String role, 
                                String temporaryPassword) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.registrationNumber = registrationNumber;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getTemporaryPassword() {
        return temporaryPassword;
    }
    
    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }
}
