package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "room_change_transitions")
public class RoomChangeTransition {
    
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
    
    @Column(name = "original_room_id")
    private UUID originalRoomId;
    
    @Column(name = "new_room_id")
    private UUID newRoomId;
    
    @Column(name = "transition_start_time", nullable = false)
    private Instant transitionStartTime;
    
    @Column(name = "transition_end_time", nullable = false)
    private Instant transitionEndTime;
    
    @Column(name = "grace_period_minutes", nullable = false)
    private Integer gracePeriodMinutes = 15;
    
    @Column(name = "room_change_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomChangeType roomChangeType;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "notifications_sent", nullable = false)
    private Boolean notificationsSent = false;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    public enum RoomChangeType {
        SUDDEN_CHANGE,
        PRE_PLANNED,
        WEEKLY_SWAP,
        EMERGENCY_MOVE
    }
    
    // Constructors
    public RoomChangeTransition() {
        this.transitionStartTime = Instant.now();
        this.transitionEndTime = Instant.now().plusSeconds(900); // 15 minutes
    }
    
    public RoomChangeTransition(ClassroomSession session, User changedBy, 
                               UUID originalRoomId, UUID newRoomId, 
                               RoomChangeType changeType, String reason) {
        this();
        this.session = session;
        this.changedBy = changedBy;
        this.originalRoomId = originalRoomId;
        this.newRoomId = newRoomId;
        this.roomChangeType = changeType;
        this.reason = reason;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ClassroomSession getSession() { return session; }
    public void setSession(ClassroomSession session) { this.session = session; }
    
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    
    public UUID getOriginalRoomId() { return originalRoomId; }
    public void setOriginalRoomId(UUID originalRoomId) { this.originalRoomId = originalRoomId; }
    
    public UUID getNewRoomId() { return newRoomId; }
    public void setNewRoomId(UUID newRoomId) { this.newRoomId = newRoomId; }
    
    public Instant getTransitionStartTime() { return transitionStartTime; }
    public void setTransitionStartTime(Instant transitionStartTime) { this.transitionStartTime = transitionStartTime; }
    
    public Instant getTransitionEndTime() { return transitionEndTime; }
    public void setTransitionEndTime(Instant transitionEndTime) { this.transitionEndTime = transitionEndTime; }
    
    public Integer getGracePeriodMinutes() { return gracePeriodMinutes; }
    public void setGracePeriodMinutes(Integer gracePeriodMinutes) { this.gracePeriodMinutes = gracePeriodMinutes; }
    
    public RoomChangeType getRoomChangeType() { return roomChangeType; }
    public void setRoomChangeType(RoomChangeType roomChangeType) { this.roomChangeType = roomChangeType; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Boolean getNotificationsSent() { return notificationsSent; }
    public void setNotificationsSent(Boolean notificationsSent) { this.notificationsSent = notificationsSent; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    // Utility methods
    public boolean isInGracePeriod() {
        return Instant.now().isBefore(transitionEndTime);
    }
    
    public boolean isTransitionComplete() {
        return Instant.now().isAfter(transitionEndTime);
    }
}
