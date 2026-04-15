package com.example.smartAttendence.dto.v1;

public record DepartmentCreateRequest(
    String name,
    String code,
    String description,
    Boolean isActive
) {}
