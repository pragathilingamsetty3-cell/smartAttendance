package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "emergency_session_changes")
public class EmergencySessionChange {
    
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false)
    private UUID id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassroomSession session;
    
    @ManyToOne
    @JoinColumn(name = "changed_by_user_id")
    private User changedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private EmergencyChangeType changeType;
    
    @Column(name = "original_faculty_id")
    private UUID originalFacultyId;
    
    @Column(name = "new_faculty_id")
    private UUID newFacultyId;
    
    @Column(name = "original_room_id")
    private UUID originalRoomId;
    
    @Column(name = "new_room_id")
    private UUID newRoomId;
    
    @Column(name = "original_start_time")
    private Instant originalStartTime;
    
    @Column(name = "new_start_time")
    private Instant newStartTime;
    
    @Column(name = "original_end_time")
    private Instant originalEndTime;
    
    @Column(name = "new_end_time")
    private Instant newEndTime;
    
    @Column(name = "reason", nullable = false)
    private String reason;
    
    @Column(name = "admin_notes")
    private String adminNotes;
    
    @Column(name = "change_timestamp", nullable = false)
    private Instant changeTimestamp;
    
    @Column(name = "effective_timestamp", nullable = false)
    private Instant effectiveTimestamp;
    
    @Column(name = "emergency_override", nullable = false)
    private Boolean emergencyOverride = false;
    
    @Column(name = "notify_students", nullable = false)
    private Boolean notifyStudents = true;
    
    @Column(name = "notify_parents", nullable = false)
    private Boolean notifyParents = false;
    
    public enum EmergencyChangeType {
        FACULTY_SUBSTITUTION,
        ROOM_CHANGE,
        TIME_CHANGE,
        CANCELLATION,
        MERGE_SESSIONS,
        SPLIT_SESSION
    }
    
    // Constructors
    public EmergencySessionChange() {
        this.changeTimestamp = Instant.now();
        this.effectiveTimestamp = Instant.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ClassroomSession getSession() { return session; }
    public void setSession(ClassroomSession session) { this.session = session; }
    
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    
    public EmergencyChangeType getChangeType() { return changeType; }
    public void setChangeType(EmergencyChangeType changeType) { this.changeType = changeType; }
    
    public UUID getOriginalFacultyId() { return originalFacultyId; }
    public void setOriginalFacultyId(UUID originalFacultyId) { this.originalFacultyId = originalFacultyId; }
    
    public UUID getNewFacultyId() { return newFacultyId; }
    public void setNewFacultyId(UUID newFacultyId) { this.newFacultyId = newFacultyId; }
    
    public UUID getOriginalRoomId() { return originalRoomId; }
    public void setOriginalRoomId(UUID originalRoomId) { this.originalRoomId = originalRoomId; }
    
    public UUID getNewRoomId() { return newRoomId; }
    public void setNewRoomId(UUID newRoomId) { this.newRoomId = newRoomId; }
    
    public Instant getOriginalStartTime() { return originalStartTime; }
    public void setOriginalStartTime(Instant originalStartTime) { this.originalStartTime = originalStartTime; }
    
    public Instant getNewStartTime() { return newStartTime; }
    public void setNewStartTime(Instant newStartTime) { this.newStartTime = newStartTime; }
    
    public Instant getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(Instant originalEndTime) { this.originalEndTime = originalEndTime; }
    
    public Instant getNewEndTime() { return newEndTime; }
    public void setNewEndTime(Instant newEndTime) { this.newEndTime = newEndTime; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    
    public Instant getChangeTimestamp() { return changeTimestamp; }
    public void setChangeTimestamp(Instant changeTimestamp) { this.changeTimestamp = changeTimestamp; }
    
    public Instant getEffectiveTimestamp() { return effectiveTimestamp; }
    public void setEffectiveTimestamp(Instant effectiveTimestamp) { this.effectiveTimestamp = effectiveTimestamp; }
    
    public Boolean getEmergencyOverride() { return emergencyOverride; }
    public void setEmergencyOverride(Boolean emergencyOverride) { this.emergencyOverride = emergencyOverride; }
    
    public Boolean getNotifyStudents() { return notifyStudents; }
    public void setNotifyStudents(Boolean notifyStudents) { this.notifyStudents = notifyStudents; }
    
    public Boolean getNotifyParents() { return notifyParents; }
    public void setNotifyParents(Boolean notifyParents) { this.notifyParents = notifyParents; }
}
