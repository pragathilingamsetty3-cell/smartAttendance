package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 📅 TIMETABLE REQUEST DTO
 * Used for creating and updating recurring class schedules.
 */
public record TimetableRequestDTO(
    @NotBlank(message = "Subject is required")
    String subject,

    UUID roomId,
    UUID facultyId,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @NotBlank(message = "Academic year is required")
    String academicYear,

    @NotBlank(message = "Semester is required")
    String semester,

    UUID sectionId,
    Boolean hasLunchBreak,
    LocalTime lunchBreakStart,
    LocalTime lunchBreakEnd,
    Boolean hasShortBreak,
    LocalTime shortBreakStart,
    LocalTime shortBreakEnd,
    Integer breakToleranceMinutes,
    Boolean isExamDay,
    Boolean isHoliday,
    java.time.LocalDate holidayDate
) {}
