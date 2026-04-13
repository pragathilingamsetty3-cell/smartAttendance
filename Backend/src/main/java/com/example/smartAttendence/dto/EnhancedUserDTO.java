package com.example.smartAttendence.dto;

import com.example.smartAttendence.domain.UserStatus;
import com.example.smartAttendence.entity.StudentProfile;
import com.example.smartAttendence.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced User DTO that properly handles academic data separation
 */
public class EnhancedUserDTO {

    private UUID id;
    private String name;
    private String email;
    private String registrationNumber;
    private Role role;
    private UserStatus status;
    private String department;
    private Instant createdAt;
    private boolean firstLogin;
    private String deviceId;
    private UUID sectionId;
    
    // Academic data - only for students
    private StudentProfileDTO academicProfile;
    
    // Contact information
    private String studentMobile;
    private String parentMobile;
    private String studentEmail;
    private String parentEmail;

    // Constructors
    public EnhancedUserDTO() {}

    public EnhancedUserDTO(UUID id, String name, String email, Role role, UserStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public StudentProfileDTO getAcademicProfile() {
        return academicProfile;
    }

    public void setAcademicProfile(StudentProfileDTO academicProfile) {
        this.academicProfile = academicProfile;
    }

    public String getStudentMobile() {
        return studentMobile;
    }

    public void setStudentMobile(String studentMobile) {
        this.studentMobile = studentMobile;
    }

    public String getParentMobile() {
        return parentMobile;
    }

    public void setParentMobile(String parentMobile) {
        this.parentMobile = parentMobile;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    /**
     * Inner class for student academic profile
     */
    public static class StudentProfileDTO {
        private String totalAcademicYears;
        private Integer currentSemester;
        private Instant enrollmentDate;
        private Instant expectedGraduationDate;
        private String academicStatus;
        private Double gpa;
        private Integer creditsCompleted;
        private Double attendancePercentage;

        // Constructors
        public StudentProfileDTO() {}

        public StudentProfileDTO(StudentProfile profile) {
            this.totalAcademicYears = profile.getTotalAcademicYears();
            this.currentSemester = profile.getCurrentSemester();
            this.enrollmentDate = profile.getEnrollmentDate();
            this.expectedGraduationDate = profile.getExpectedGraduationDate();
            this.academicStatus = profile.getAcademicStatus() != null ? profile.getAcademicStatus().toString() : null;
            this.gpa = profile.getGpa();
            this.creditsCompleted = profile.getCreditsCompleted();
            this.attendancePercentage = profile.getAttendancePercentage();
        }

        // Getters and Setters
        public String getTotalAcademicYears() {
            return totalAcademicYears;
        }

        public void setTotalAcademicYears(String totalAcademicYears) {
            this.totalAcademicYears = totalAcademicYears;
        }

        public Integer getCurrentSemester() {
            return currentSemester;
        }

        public void setCurrentSemester(Integer currentSemester) {
            this.currentSemester = currentSemester;
        }

        public Instant getEnrollmentDate() {
            return enrollmentDate;
        }

        public void setEnrollmentDate(Instant enrollmentDate) {
            this.enrollmentDate = enrollmentDate;
        }

        public Instant getExpectedGraduationDate() {
            return expectedGraduationDate;
        }

        public void setExpectedGraduationDate(Instant expectedGraduationDate) {
            this.expectedGraduationDate = expectedGraduationDate;
        }

        public String getAcademicStatus() {
            return academicStatus;
        }

        public void setAcademicStatus(String academicStatus) {
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
    }
}
