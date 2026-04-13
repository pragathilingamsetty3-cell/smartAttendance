package com.example.smartAttendence.service;

import com.example.smartAttendence.domain.AcademicStatus;
import com.example.smartAttendence.entity.StudentProfile;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to manage student academic profiles
 * Ensures only student roles have academic data
 */
@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * Create student profile for a new student
     */
    public StudentProfile createStudentProfile(UUID userId, String totalAcademicYears, Integer currentSemester) {
        StudentProfile profile = new StudentProfile(userId, totalAcademicYears, currentSemester);
        return studentProfileRepository.save(profile);
    }

    /**
     * Get student profile by user ID
     */
    public StudentProfile getStudentProfile(UUID userId) {
        return studentProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found for user: " + userId));
    }

    /**
     * Update student semester (promotion)
     */
    public StudentProfile promoteStudent(UUID userId) {
        StudentProfile profile = getStudentProfile(userId);
        
        if (!profile.canPromote()) {
            throw new RuntimeException("Student cannot be promoted. Current status: " + profile.getAcademicStatus() + 
                                     ", Semester: " + profile.getCurrentSemester() + "/" + profile.getMaxSemesters());
        }
        
        profile.incrementSemester();
        return studentProfileRepository.save(profile);
    }

    /**
     * Update academic status
     */
    public StudentProfile updateAcademicStatus(UUID userId, AcademicStatus academicStatus) {
        StudentProfile profile = getStudentProfile(userId);
        profile.setAcademicStatus(academicStatus);
        return studentProfileRepository.save(profile);
    }

    /**
     * Update attendance percentage
     */
    public StudentProfile updateAttendance(UUID userId, Double attendedClasses, Double totalClasses) {
        StudentProfile profile = getStudentProfile(userId);
        profile.updateAttendance(attendedClasses, totalClasses);
        return studentProfileRepository.save(profile);
    }

    /**
     * Delete student profile when user is deleted or role changes
     */
    public void deleteStudentProfile(UUID userId) {
        studentProfileRepository.deleteById(userId);
    }

    /**
     * Check if user has student profile
     */
    public boolean hasStudentProfile(UUID userId) {
        return studentProfileRepository.existsById(userId);
    }

    /**
     * Get students by semester
     */
    public java.util.List<StudentProfile> getStudentsBySemester(Integer semester) {
        return studentProfileRepository.findByCurrentSemester(semester);
    }

    /**
     * Get students by academic status
     */
    public java.util.List<StudentProfile> getStudentsByAcademicStatus(AcademicStatus academicStatus) {
        return studentProfileRepository.findByAcademicStatus(academicStatus);
    }
}
