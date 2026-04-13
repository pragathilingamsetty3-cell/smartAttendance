package com.example.smartAttendence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weekly_room_swaps")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class WeeklyRoomSwap {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false)
    private UUID id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "original_room_id", nullable = false)
    private Room originalRoom;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "new_room_id", nullable = false)
    private Room newRoom;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "timetable_id", nullable = false)
    private com.example.smartAttendence.entity.Timetable timetable;
    
    @Column(name = "swap_date", nullable = false)
    private java.time.LocalDate swapDate;
    
    private String reason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private com.example.smartAttendence.domain.User approvedBy;
    
    @Column(name = "approved_at")
    private Instant approvedAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    
    // Explicit getters and setters for Lombok compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Room getOriginalRoom() { return originalRoom; }
    public void setOriginalRoom(Room originalRoom) { this.originalRoom = originalRoom; }
    
    public Room getNewRoom() { return newRoom; }
    public void setNewRoom(Room newRoom) { this.newRoom = newRoom; }
    
    public com.example.smartAttendence.entity.Timetable getTimetable() { return timetable; }
    public void setTimetable(com.example.smartAttendence.entity.Timetable timetable) { this.timetable = timetable; }
    
    public java.time.LocalDate getSwapDate() { return swapDate; }
    public void setSwapDate(java.time.LocalDate swapDate) { this.swapDate = swapDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public com.example.smartAttendence.domain.User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(com.example.smartAttendence.domain.User approvedBy) { this.approvedBy = approvedBy; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
