package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.StudentProfile;
import com.example.smartAttendence.domain.AcademicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for StudentProfile entities
 */
@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    /**
     * Find students by current semester
     */
    List<StudentProfile> findByCurrentSemester(Integer semester);

    /**
     * Find students by academic status
     */
    List<StudentProfile> findByAcademicStatus(AcademicStatus academicStatus);

    /**
     * Find students by total academic years
     */
    List<StudentProfile> findByTotalAcademicYears(String totalAcademicYears);

    /**
     * Check if student profile exists for user
     */
    boolean existsById(UUID userId);

    /**
     * Count students by semester
     */
    long countByCurrentSemester(Integer semester);

    /**
     * Count students by academic status
     */
    long countByAcademicStatus(AcademicStatus academicStatus);

    /**
     * Find students with low attendance
     */
    List<StudentProfile> findByAttendancePercentageLessThan(Double threshold);

    /**
     * Find students by GPA range
     */
    List<StudentProfile> findByGpaBetween(Double minGpa, Double maxGpa);
}
