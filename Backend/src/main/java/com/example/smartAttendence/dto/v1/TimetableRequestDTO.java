package com.example.smartAttendence.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 📅 TIMETABLE REQUEST DTO
 * Used for creating and updating recurring class schedules.
 */
@Schema(description = "Request object for creating or updating a timetable slot")
public record TimetableRequestDTO(
    @NotBlank(message = "Subject is required")
    @Schema(description = "Subject name", example = "Advanced AI Systems")
    String subject,

    @Schema(description = "ID of the designated classroom")
    UUID roomId,

    @Schema(description = "ID of the assigned faculty member")
    UUID facultyId,

    @NotNull(message = "Day of week is required")
    @Schema(description = "Day of the week for this slot", example = "MONDAY")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    @Schema(description = "Start time (HH:mm:ss)", example = "10:00:00")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    @Schema(description = "End time (HH:mm:ss)", example = "11:30:00")
    LocalTime endTime,

    @NotBlank(message = "Academic year is required")
    @Schema(description = "Academic year string", example = "2025-2026")
    String academicYear,

    @NotBlank(message = "Semester is required")
    @Schema(description = "Current semester", example = "Spring 2026")
    String semester,

    @Schema(description = "ID of the student section assigned to this slot")
    UUID sectionId,

    @Schema(description = "Enable lunch break during this session")
    Boolean hasLunchBreak,

    @Schema(description = "Lunch break start time")
    LocalTime lunchBreakStart,

    @Schema(description = "Lunch break end time")
    LocalTime lunchBreakEnd,

    @Schema(description = "Enable short break during this session")
    Boolean hasShortBreak,

    @Schema(description = "Short break start time")
    LocalTime shortBreakStart,

    @Schema(description = "Short break end time")
    LocalTime shortBreakEnd,

    @Schema(description = "Grace period for walk-out detection during breaks (minutes)", example = "10")
    Integer breakToleranceMinutes,

    @Schema(description = "Whether this is an exam day session", example = "false")
    Boolean isExamDay,

    @Schema(description = "Whether this is a holiday session", example = "false")
    Boolean isHoliday,

    @Schema(description = "Specific date for the holiday (YYYY-MM-DD)", example = "2026-04-05")
    java.time.LocalDate holidayDate
) {}
