package com.example.smartAttendence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Academic Calendar Entity
 * 
 * Manages academic calendar entries including holidays, exam days, and special events
 * that affect class scheduling and attendance operations.
 */
@Entity
@Table(name = "academic_calendar")
public class AcademicCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "day_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DayType dayType;

    @Column(name = "description")
    private String description;

    @Column(name = "affects_all_sections", nullable = false)
    private Boolean affectsAllSections = true;

    @Column(name = "affected_sections")
    private String affectedSections;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DayType {
        REGULAR_DAY,
        HOLIDAY,
        EXAM_DAY,
        HALF_DAY,
        SPECIAL_EVENT
    }

    // Constructors
    public AcademicCalendar() {
        this.createdAt = LocalDateTime.now();
    }

    public AcademicCalendar(LocalDate date, DayType dayType, String description) {
        this();
        this.date = date;
        this.dayType = dayType;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        this.updatedAt = LocalDateTime.now();
    }

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getAffectsAllSections() {
        return affectsAllSections;
    }

    public void setAffectsAllSections(Boolean affectsAllSections) {
        this.affectsAllSections = affectsAllSections;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAffectedSections() {
        return affectedSections;
    }

    public void setAffectedSections(String affectedSections) {
        this.affectedSections = affectedSections;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "AcademicCalendar{" +
                "id=" + id +
                ", date=" + date +
                ", dayType=" + dayType +
                ", description='" + description + '\'' +
                ", affectsAllSections=" + affectsAllSections +
                ", affectedSections='" + affectedSections + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AcademicCalendar that = (AcademicCalendar) o;

        return date != null ? date.equals(that.date) : that.date == null;
    }

    @Override
    public int hashCode() {
        return date != null ? date.hashCode() : 0;
    }
}
