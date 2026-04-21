package com.example.smartAttendence.entity;

import com.example.smartAttendence.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "timetables", 
       indexes = {
           @Index(name = "idx_timetable_faculty", columnList = "faculty_id"),
           @Index(name = "idx_timetable_room", columnList = "room_id"),
           @Index(name = "idx_timetable_section", columnList = "section_id"),
           @Index(name = "idx_timetable_schedule", columnList = "day_of_week, start_time, end_time")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = true)
    private User faculty;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_exam_day", nullable = false)
    private Boolean isExamDay = false;

    @Column(name = "is_holiday", nullable = false)
    private Boolean isHoliday = false;

    @Column(name = "holiday_date")
    private java.time.LocalDate holidayDate;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "semester", nullable = false)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "is_adhoc", nullable = false)
    private Boolean isAdhoc = false;

    @Column(name = "overrides_holiday", nullable = false)
    private Boolean overridesHoliday = false;

    private String description;

    // 🕐 BREAK TIME MANAGEMENT FIELDS
    @Column(name = "has_lunch_break")
    private Boolean hasLunchBreak = false;

    @Column(name = "lunch_break_start")
    private LocalTime lunchBreakStart;

    @Column(name = "lunch_break_end")
    private LocalTime lunchBreakEnd;

    @Column(name = "has_short_break")
    private Boolean hasShortBreak = false;

    @Column(name = "short_break_start")
    private LocalTime shortBreakStart;

    @Column(name = "short_break_end")
    private LocalTime shortBreakEnd;

    @Column(name = "break_tolerance_minutes")
    private Integer breakToleranceMinutes = 10; // Grace period during breaks

    // Explicit getters and setters for Lombok compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public Boolean getIsExamDay() { return isExamDay; }
    public void setIsExamDay(Boolean isExamDay) { this.isExamDay = isExamDay; }
    
    public Boolean getIsHoliday() { return isHoliday; }
    public void setIsHoliday(Boolean isHoliday) { this.isHoliday = isHoliday; }

    public java.time.LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(java.time.LocalDate holidayDate) { this.holidayDate = holidayDate; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    
    public Boolean getIsAdhoc() { return isAdhoc; }
    public void setIsAdhoc(Boolean isAdhoc) { this.isAdhoc = isAdhoc; }
    
    public Boolean getOverridesHoliday() { return overridesHoliday; }
    public void setOverridesHoliday(Boolean overridesHoliday) { this.overridesHoliday = overridesHoliday; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // 🕐 BREAK TIME MANAGEMENT GETTERS/SETTERS
    public Boolean getHasLunchBreak() { return hasLunchBreak; }
    public void setHasLunchBreak(Boolean hasLunchBreak) { this.hasLunchBreak = hasLunchBreak; }
    
    public LocalTime getLunchBreakStart() { return lunchBreakStart; }
    public void setLunchBreakStart(LocalTime lunchBreakStart) { this.lunchBreakStart = lunchBreakStart; }
    
    public LocalTime getLunchBreakEnd() { return lunchBreakEnd; }
    public void setLunchBreakEnd(LocalTime lunchBreakEnd) { this.lunchBreakEnd = lunchBreakEnd; }
    
    public Boolean getHasShortBreak() { return hasShortBreak; }
    public void setHasShortBreak(Boolean hasShortBreak) { this.hasShortBreak = hasShortBreak; }
    
    public LocalTime getShortBreakStart() { return shortBreakStart; }
    public void setShortBreakStart(LocalTime shortBreakStart) { this.shortBreakStart = shortBreakStart; }
    
    public LocalTime getShortBreakEnd() { return shortBreakEnd; }
    public void setShortBreakEnd(LocalTime shortBreakEnd) { this.shortBreakEnd = shortBreakEnd; }
    
    public Integer getBreakToleranceMinutes() { return breakToleranceMinutes; }
    public void setBreakToleranceMinutes(Integer breakToleranceMinutes) { this.breakToleranceMinutes = breakToleranceMinutes; }

    // 🕐 BREAK TIME HELPER METHODS
    public boolean isDuringLunchBreak(LocalTime time) {
        return hasLunchBreak && lunchBreakStart != null && lunchBreakEnd != null &&
               !time.isBefore(lunchBreakStart) && !time.isAfter(lunchBreakEnd);
    }

    public boolean isDuringShortBreak(LocalTime time) {
        return hasShortBreak && shortBreakStart != null && shortBreakEnd != null &&
               !time.isBefore(shortBreakStart) && !time.isAfter(shortBreakEnd);
    }

    public boolean isDuringAnyBreak(LocalTime time) {
        return isDuringLunchBreak(time) || isDuringShortBreak(time);
    }

    public int getWalkOutThresholdForTime(LocalTime time) {
        // More lenient during breaks (10 minutes vs 3 minutes)
        return isDuringAnyBreak(time) ? 10 : 3;
    }
}
