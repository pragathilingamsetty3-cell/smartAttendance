package com.example.smartAttendence.dto;

import com.example.smartAttendence.enums.Role;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String name;
    private String email;
    private String registrationNumber;
    private String phoneNumber;
    private Role role;
}