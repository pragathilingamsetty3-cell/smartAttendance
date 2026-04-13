package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offline_attendance_records",
       indexes = {
           @Index(name = "idx_offline_student", columnList = "student_id"),
           @Index(name = "idx_offline_session", columnList = "session_id"),
           @Index(name = "idx_offline_sync_status", columnList = "sync_status"),
           @Index(name = "idx_offline_timestamp", columnList = "client_timestamp")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OfflineAttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "client_timestamp", nullable = false)
    private Instant clientTimestamp;

    @Column(name = "server_timestamp")
    private Instant serverTimestamp;

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lng")
    private Double locationLng;

    @Column(name = "biometric_signature")
    private String biometricSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "sync_attempts", nullable = false)
    private Integer syncAttempts = 0;

    @Column(name = "last_sync_attempt")
    private Instant lastSyncAttempt;

    @Column(name = "sync_error_message")
    private String syncErrorMessage;

    @Column(name = "is_mocked", nullable = false)
    private Boolean isMocked = false;

    public enum SyncStatus {
        PENDING,
        SYNCED,
        FAILED,
        DUPLICATE,
        REJECTED
    }
}
