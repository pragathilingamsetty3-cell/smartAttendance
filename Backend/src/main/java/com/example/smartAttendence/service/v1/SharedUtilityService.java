package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Shared utility service for common operations to avoid code duplication
 */
@Service
public class SharedUtilityService {

    private final SectionRepository sectionRepository;
    private final UserV1Repository userV1Repository;

    public SharedUtilityService(SectionRepository sectionRepository, UserV1Repository userV1Repository) {
        this.sectionRepository = sectionRepository;
        this.userV1Repository = userV1Repository;
    }

    /**
     * Validates and retrieves a section by ID
     * @param sectionId The section ID to validate
     * @return The section entity
     * @throws IllegalArgumentException if section not found
     */
    public Section validateAndGetSection(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
    }

    /**
     * Validates and retrieves a user by ID
     * @param userId The user ID to validate
     * @return The user entity
     * @throws IllegalArgumentException if user not found
     */
    public User validateAndGetUser(UUID userId) {
        return userV1Repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * Gets all users in a specific section efficiently using database query
     * @param sectionId The section ID
     * @return List of users in the section
     */
    @Cacheable(value = "users", key = "#sectionId", unless = "#result == null || #result.isEmpty()")
    public List<User> getUsersBySection(UUID sectionId) {
        return userV1Repository.findBySectionId(sectionId);
    }

    /**
     * Gets all students in a specific section efficiently
     * @param sectionId The section ID
     * @return List of student users in the section
     */
    @Cacheable(value = "students", key = "#sectionId", unless = "#result == null || #result.isEmpty()")
    public List<User> getStudentsBySection(UUID sectionId) {
        return userV1Repository.findBySectionIdAndRole(sectionId, Role.STUDENT);
    }

    /**
     * Counts users in a section efficiently
     * @param sectionId The section ID
     * @return Number of users in the section
     */
    public long countUsersBySection(UUID sectionId) {
        return userV1Repository.findBySectionId(sectionId).size();
    }

    /**
     * Counts students in a section efficiently
     * @param sectionId The section ID
     * @return Number of students in the section
     */
    @Cacheable(value = "userCounts", key = "#sectionId")
    public long countStudentsBySection(UUID sectionId) {
        return userV1Repository.countBySectionIdAndRole(sectionId, Role.STUDENT);
    }

    /**
     * Validates if a user has the specified role using enum comparison
     * @param user The user to check
     * @param expectedRole The expected role
     * @return true if user has the expected role
     */
    public boolean validateUserRole(User user, Role expectedRole) {
        return expectedRole.equals(user.getRole());
    }

    /**
     * Validates if a user is a student
     * @param user The user to check
     * @return true if user is a student
     */
    public boolean validateStudentRole(User user) {
        return validateUserRole(user, Role.STUDENT);
    }

    /**
     * Checks if a section has reached its capacity
     * @param section The section to check
     * @return true if section is at full capacity
     */
    public boolean isSectionAtCapacity(Section section) {
        long currentUsers = countUsersBySection(section.getId());
        return currentUsers >= section.getCapacity();
    }

    /**
     * Gets available capacity for a section
     * @param section The section to check
     * @return Number of available spots
     */
    public int getAvailableCapacity(Section section) {
        long currentUsers = countUsersBySection(section.getId());
        return (int) Math.max(0, section.getCapacity() - currentUsers);
    }
}
