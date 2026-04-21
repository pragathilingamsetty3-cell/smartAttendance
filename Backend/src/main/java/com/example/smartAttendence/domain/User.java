package com.example.smartAttendence.domain;

import com.example.smartAttendence.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.io.Serializable;

@Entity(name = "V1User")
@Table(name = "users",
       indexes = {
           @Index(name = "idx_user_department", columnList = "department"),
           @Index(name = "idx_user_role", columnList = "role"),
           @Index(name = "idx_user_status", columnList = "status"),
           @Index(name = "idx_user_section", columnList = "section_id")
       })
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "department")
    private String department;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean firstLogin = true;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "biometric_signature")
    private String biometricSignature;

    @Column(name = "device_registered_at")
    private Instant deviceRegisteredAt;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "total_academic_years")
    private String totalAcademicYears;

    @Column(name = "semester")
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // Production IAM Fields
    @Column(name = "student_mobile")
    private String studentMobile;

    @Column(name = "parent_mobile")
    private String parentMobile;

    @Column(name = "student_email")
    private String studentEmail;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "is_temporary_password", nullable = false)
    private Boolean isTemporaryPassword = true;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private Instant otpExpiry;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Instant getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(Instant resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
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

    public Boolean getIsTemporaryPassword() {
        return isTemporaryPassword;
    }

    public void setIsTemporaryPassword(Boolean isTemporaryPassword) {
        this.isTemporaryPassword = isTemporaryPassword;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Instant getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(Instant otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getBiometricSignature() {
        return biometricSignature;
    }

    public void setBiometricSignature(String biometricSignature) {
        this.biometricSignature = biometricSignature;
    }

    public Instant getDeviceRegisteredAt() {
        return deviceRegisteredAt;
    }

    public void setDeviceRegisteredAt(Instant deviceRegisteredAt) {
        this.deviceRegisteredAt = deviceRegisteredAt;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public String getTotalAcademicYears() {
        return totalAcademicYears;
    }

    public void setTotalAcademicYears(String totalAcademicYears) {
        this.totalAcademicYears = totalAcademicYears;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    // AI and Autonomous System Support
    @ManyToOne
    @JoinColumn(name = "section_id", insertable = false, updatable = false)
    private com.example.smartAttendence.entity.Section section;

    public com.example.smartAttendence.entity.Section getSection() {
        return section;
    }

    public void setSection(com.example.smartAttendence.entity.Section section) {
        this.section = section;
    }
}

