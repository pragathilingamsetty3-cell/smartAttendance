package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SectionV1Service {

    private final SharedUtilityService sharedUtilityService;
    private final UserV1Repository userV1Repository;

    public SectionV1Service(SharedUtilityService sharedUtilityService, UserV1Repository userV1Repository) {
        this.sharedUtilityService = sharedUtilityService;
        this.userV1Repository = userV1Repository;
    }

    public void assignStudentToSection(UUID sectionId, UUID studentId) {
        // Use shared utility for validation
        var section = sharedUtilityService.validateAndGetSection(sectionId);
        var student = sharedUtilityService.validateAndGetUser(studentId);

        // Validate student role using enum comparison
        if (!sharedUtilityService.validateStudentRole(student)) {
            throw new IllegalArgumentException("User is not a student: " + studentId);
        }

        // Check capacity using efficient method
        if (sharedUtilityService.isSectionAtCapacity(section)) {
            throw new IllegalArgumentException("Section has reached maximum capacity");
        }

        // Assign student to section
        student.setSectionId(sectionId);
        userV1Repository.save(student);
    }
}
