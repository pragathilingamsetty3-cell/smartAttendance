package com.example.smartAttendence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassroomSession session;

    @Column(nullable = false)
    private String status;

    @Column(name = "biometric_signature")
    private String biometricSignature;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "is_mocked", nullable = false)
    private boolean mocked = false;

    @Column(name = "is_ai_decision")
    private boolean aiDecision = false;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "note")
    private String note;

    @Column(name = "manually_corrected", nullable = false)
    private boolean manuallyCorrected = false;

    @Column(name = "original_ai_status")
    private String originalAiStatus;

    @Column(name = "corrector_id")
    private UUID correctorId;

    // 🛡️ AI SECURITY AUDIT FIELDS
    @Column(name = "device_signature")
    private String deviceSignature;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "is_hardware_verified")
    private boolean hardwareVerified = false;

    @Column(name = "biometric_verified")
    private boolean biometricVerified = false;

    // 🚀 AI SPATIAL & GPS FIELDS
    @Column(name = "is_moving")
    private boolean moving = false;

    @Column(name = "acceleration_magnitude")
    private Double accelerationMagnitude = 0.0;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "gps_accuracy")
    private Double gpsAccuracy;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public ClassroomSession getSession() {
        return session;
    }

    public void setSession(ClassroomSession session) {
        this.session = session;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBiometricSignature() {
        return biometricSignature;
    }

    public void setBiometricSignature(String biometricSignature) {
        this.biometricSignature = biometricSignature;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isMocked() {
        return mocked;
    }

    public void setMocked(boolean mocked) {
        this.mocked = mocked;
    }

    public boolean isAiDecision() {
        return aiDecision;
    }

    public void setAiDecision(boolean aiDecision) {
        this.aiDecision = aiDecision;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public boolean isManuallyCorrected() {
        return manuallyCorrected;
    }

    public void setManuallyCorrected(boolean manuallyCorrected) {
        this.manuallyCorrected = manuallyCorrected;
    }

    public String getOriginalAiStatus() {
        return originalAiStatus;
    }

    public void setOriginalAiStatus(String originalAiStatus) {
        this.originalAiStatus = originalAiStatus;
    }

    public UUID getCorrectorId() {
        return correctorId;
    }

    public void setCorrectorId(UUID correctorId) {
        this.correctorId = correctorId;
    }

    public String getDeviceSignature() {
        return deviceSignature;
    }

    public void setDeviceSignature(String deviceSignature) {
        this.deviceSignature = deviceSignature;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean isHardwareVerified() {
        return hardwareVerified;
    }

    public void setHardwareVerified(boolean hardwareVerified) {
        this.hardwareVerified = hardwareVerified;
    }

    public boolean isBiometricVerified() {
        return biometricVerified;
    }

    public void setBiometricVerified(boolean biometricVerified) {
        this.biometricVerified = biometricVerified;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public Double getAccelerationMagnitude() {
        return accelerationMagnitude;
    }

    public void setAccelerationMagnitude(Double accelerationMagnitude) {
        this.accelerationMagnitude = accelerationMagnitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Double gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }
}


