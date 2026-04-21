package com.example.smartAttendence.dto.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for sending Timetable data to the frontend without circular references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableResponseDTO {
    private UUID id;
    private String subject;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private Boolean isExamDay;
    private Boolean isHoliday;
    private String holidayDate;
    private Boolean isAdhoc;
    private String startDate;
    private String endDate;
    private String description;

    // Simplified relations
    private RoomInfo room;
    private FacultyInfo faculty;
    private SectionInfo section;

    // Break management
    private Boolean hasLunchBreak;
    private String lunchBreakStart;
    private String lunchBreakEnd;
    private Boolean hasShortBreak;
    private String shortBreakStart;
    private String shortBreakEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private UUID id;
        private String name;
        private String building;
        private Integer floor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacultyInfo {
        private UUID id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionInfo {
        private UUID id;
        private String name;
    }
}
