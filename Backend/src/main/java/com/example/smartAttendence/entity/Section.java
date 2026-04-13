package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.Instant;
import java.io.Serializable;

@Entity
@Table(name = "sections",
       indexes = {
           @Index(name = "idx_section_department", columnList = "department_id"),
           @Index(name = "idx_section_program", columnList = "program"),
           @Index(name = "idx_section_year_semester", columnList = "academic_year, semester")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Section implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "CS-A", "CS-B", "EE-1"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String program; // e.g., "Computer Science", "Electrical Engineering"

    @Column(nullable = false)
    private Integer batchYear;

    @Column(name = "total_academic_years", nullable = false)
    private String totalAcademicYears;

    @Column(name = "current_semester", nullable = false)
    private Integer currentSemester;

    @Column(nullable = false)
    private Integer capacity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_advisor_id")
    private User classAdvisor;

    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Explicit getters and setters for Lombok compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    
    public Integer getBatchYear() { return batchYear; }
    public void setBatchYear(Integer batchYear) { this.batchYear = batchYear; }
    
    public String getTotalAcademicYears() { return totalAcademicYears; }
    public void setTotalAcademicYears(String totalAcademicYears) { this.totalAcademicYears = totalAcademicYears; }
    
    public Integer getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(Integer currentSemester) { this.currentSemester = currentSemester; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public User getClassAdvisor() { return classAdvisor; }
    public void setClassAdvisor(User classAdvisor) { this.classAdvisor = classAdvisor; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Additional getters for compatibility
    public String getAcademicYear() { return totalAcademicYears; }
    public void setAcademicYear(String academicYear) { this.totalAcademicYears = academicYear; }
    
    public String getSemester() { return currentSemester.toString(); }
    public void setSemester(String semester) { this.currentSemester = Integer.parseInt(semester); }
}
