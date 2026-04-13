package com.example.smartAttendence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sensor_readings",
       indexes = {
           @Index(name = "idx_sensor_student_session", columnList = "student_id, session_id"),
           @Index(name = "idx_sensor_timestamp", columnList = "reading_timestamp"),
           @Index(name = "idx_sensor_student_timestamp", columnList = "student_id, reading_timestamp")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, insertable = false, updatable = false)
    private com.example.smartAttendence.domain.User student;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, insertable = false, updatable = false)
    private com.example.smartAttendence.domain.ClassroomSession session;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "step_count", nullable = false)
    private Integer stepCount = 0;

    @Column(name = "acceleration_x", nullable = false)
    private Double accelerationX = 0.0;

    @Column(name = "acceleration_y", nullable = false)
    private Double accelerationY = 0.0;

    @Column(name = "acceleration_z", nullable = false)
    private Double accelerationZ = 0.0;

    @Column(name = "is_device_moving", nullable = false)
    private Boolean isDeviceMoving = false;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant readingTimestamp;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculate acceleration magnitude for AI analysis
     */
    @Transient
    public double getAccelerationMagnitude() {
        return Math.sqrt(
            accelerationX * accelerationX +
            accelerationY * accelerationY +
            accelerationZ * accelerationZ
        );
    }

    // ===== EXPLICIT GETTERS FOR COMPILATION =====
    
    public UUID getStudentId() {
        return studentId;
    }

    public Instant getReadingTimestamp() {
        return readingTimestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    // ===== EXPLICIT SETTERS FOR COMPILATION =====
    
    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public void setAccelerationX(Double accelerationX) {
        this.accelerationX = accelerationX;
    }

    public void setAccelerationY(Double accelerationY) {
        this.accelerationY = accelerationY;
    }

    public void setAccelerationZ(Double accelerationZ) {
        this.accelerationZ = accelerationZ;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setReadingTimestamp(Instant readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public Double getAccelerationX() {
        return accelerationX;
    }

    public Double getAccelerationY() {
        return accelerationY;
    }

    public Double getAccelerationZ() {
        return accelerationZ;
    }

    public Boolean getIsDeviceMoving() {
        return isDeviceMoving;
    }

    public void setIsDeviceMoving(Boolean isDeviceMoving) {
        this.isDeviceMoving = isDeviceMoving;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
