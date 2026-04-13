package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.AcademicStatus;
import com.example.smartAttendence.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Student academic profile - separated from User entity for clean architecture
 * Only student roles will have this profile
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    private UUID userId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "user_id")
    private com.example.smartAttendence.domain.User user;

    @Column(name = "total_academic_years", nullable = false)
    private String totalAcademicYears;

    @Column(name = "current_semester", nullable = false)
    private Integer currentSemester = 1;

    @Column(name = "enrollment_date")
    private java.time.Instant enrollmentDate;

    @Column(name = "expected_graduation_date")
    private java.time.Instant expectedGraduationDate;

    @Column(name = "academic_status")
    @Enumerated(EnumType.STRING)
    private AcademicStatus academicStatus = AcademicStatus.REGULAR;

    @Column(name = "gpa", columnDefinition = "numeric")
    private Double gpa;

    @Column(name = "credits_completed")
    private Integer creditsCompleted = 0;

    @Column(name = "attendance_percentage", columnDefinition = "numeric")
    private Double attendancePercentage = 0.0;

    // Constructors
    public StudentProfile() {}

    public StudentProfile(UUID userId, String totalAcademicYears, Integer currentSemester) {
        this.userId = userId;
        this.totalAcademicYears = totalAcademicYears;
        this.currentSemester = currentSemester;
        this.enrollmentDate = java.time.Instant.now();
        calculateExpectedGraduationDate();
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public com.example.smartAttendence.domain.User getUser() {
        return user;
    }

    public void setUser(com.example.smartAttendence.domain.User user) {
        this.user = user;
    }

    public String getTotalAcademicYears() {
        return totalAcademicYears;
    }

    public void setTotalAcademicYears(String totalAcademicYears) {
        this.totalAcademicYears = totalAcademicYears;
        calculateExpectedGraduationDate();
    }

    public Integer getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(Integer currentSemester) {
        this.currentSemester = currentSemester;
    }

    public java.time.Instant getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(java.time.Instant enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public java.time.Instant getExpectedGraduationDate() {
        return expectedGraduationDate;
    }

    public void setExpectedGraduationDate(java.time.Instant expectedGraduationDate) {
        this.expectedGraduationDate = expectedGraduationDate;
    }

    public AcademicStatus getAcademicStatus() {
        return academicStatus;
    }

    public void setAcademicStatus(AcademicStatus academicStatus) {
        this.academicStatus = academicStatus;
    }

    public Double getGpa() {
        return gpa;
    }

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }

    public Integer getCreditsCompleted() {
        return creditsCompleted;
    }

    public void setCreditsCompleted(Integer creditsCompleted) {
        this.creditsCompleted = creditsCompleted;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    // Business Logic
    public void incrementSemester() {
        if (currentSemester < getMaxSemesters()) {
            currentSemester++;
            calculateExpectedGraduationDate();
        }
    }

    public int getMaxSemesters() {
        try {
            int years = Integer.parseInt(totalAcademicYears);
            return years * 2; // 2 semesters per year
        } catch (NumberFormatException e) {
            return 8; // default 4 years * 2 semesters
        }
    }

    private void calculateExpectedGraduationDate() {
        if (enrollmentDate != null && totalAcademicYears != null) {
            try {
                int years = Integer.parseInt(totalAcademicYears);
                expectedGraduationDate = enrollmentDate.plusSeconds(years * 365L * 24 * 60 * 60);
            } catch (NumberFormatException e) {
                // Default to 4 years
                expectedGraduationDate = enrollmentDate.plusSeconds(4L * 365 * 24 * 60 * 60);
            }
        }
    }

    public boolean canPromote() {
        return currentSemester < getMaxSemesters() && 
               academicStatus == AcademicStatus.REGULAR;
    }

    public void updateAttendance(Double attendedClasses, Double totalClasses) {
        if (totalClasses > 0) {
            this.attendancePercentage = (attendedClasses / totalClasses) * 100;
        }
    }
}
