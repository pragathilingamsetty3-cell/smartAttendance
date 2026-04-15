package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record SectionCreateRequest(
    String name,
    UUID departmentId,
    String program,
    Integer capacity,
    Integer batchYear,
    String totalAcademicYears,
    Integer currentSemester,
    String description,
    Boolean isActive
) {}
