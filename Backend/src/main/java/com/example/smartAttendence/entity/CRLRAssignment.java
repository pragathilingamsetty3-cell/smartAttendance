package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.Section;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "cr_lr_assignments")
public class CRLRAssignment {
    
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false)
    private UUID id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private com.example.smartAttendence.entity.Section section;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;
    
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
    
    @Column(name = "assigned_by_id")
    private UUID assignedBy;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "academic_year", nullable = false)
    private String academicYear;
    
    @Column(name = "semester", nullable = false)
    private String semester;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "revoked_by_id")
    private UUID revokedBy;
    
    @Column(name = "revocation_reason")
    private String revocationReason;
    
    public enum RoleType {
        CR,    // Class Representative
        LR     // Lab Representative
    }
    
    // Constructors
    public CRLRAssignment() {
        this.assignedAt = Instant.now();
    }
    
    public CRLRAssignment(User user, Section section, RoleType roleType, 
                         String academicYear, String semester, UUID assignedBy) {
        this();
        this.user = user;
        this.section = section;
        this.roleType = roleType;
        this.academicYear = academicYear;
        this.semester = semester;
        this.assignedBy = assignedBy;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    
    public RoleType getRoleType() { return roleType; }
    public void setRoleType(RoleType roleType) { this.roleType = roleType; }
    
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    
    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    
    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }
    
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }
    
    // Utility methods
    public void revokeAssignment(UUID revokedBy, String reason) {
        this.active = false;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
        this.revocationReason = reason;
    }
}
