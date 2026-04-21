package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.TimetableRequestDTO;
import com.example.smartAttendence.dto.v1.TimetableResponseDTO;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.service.v1.AdminV1Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/timetables")
@RequiredArgsConstructor
@Slf4j
public class TimetableController {

    private final AdminV1Service adminV1Service;

    /**
     * Create a new timetable slot
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createTimetable(@Valid @RequestBody TimetableRequestDTO request) {
        try {
            Timetable timetable = adminV1Service.createTimetable(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToTimetableResponse(timetable));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create timetable: " + e.getMessage()));
        }
    }

    /**
     * Update an existing timetable slot
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateTimetable(@PathVariable UUID id, @Valid @RequestBody TimetableRequestDTO request) {
        try {
            Timetable timetable = adminV1Service.updateTimetable(id, request);
            return ResponseEntity.ok(mapToTimetableResponse(timetable));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update timetable: " + e.getMessage()));
        }
    }

    /**
     * Delete a timetable slot
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteTimetable(@PathVariable UUID id) {
        try {
            adminV1Service.deleteTimetable(id);
            return ResponseEntity.ok(Map.of("message", "Timetable entry deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete timetable: " + e.getMessage()));
        }
    }

    /**
     * Get timetable for a specific faculty
     */
    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<?> getTimetablesForFaculty(@PathVariable UUID facultyId) {
        try {
            List<Timetable> timetables = adminV1Service.getTimetablesForFaculty(facultyId);
            List<TimetableResponseDTO> dtos = timetables.stream()
                    .map(this::mapToTimetableResponse)
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve faculty timetable: " + e.getMessage()));
        }
    }

    /**
     * Get timetable for a specific section
     */
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<?> getTimetablesForSection(@PathVariable UUID sectionId) {
        try {
            List<Timetable> timetables = adminV1Service.getTimetablesForSection(sectionId);
            List<TimetableResponseDTO> dtos = timetables.stream()
                    .map(this::mapToTimetableResponse)
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ FAILED TO FETCH SECTION TIMETABLE for section {}: {}", sectionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve section timetable: " + e.getMessage()));
        }
    }

    /**
     * Map Timetable entity to TimetableResponseDTO
     */
    private TimetableResponseDTO mapToTimetableResponse(Timetable t) {
        if (t == null) return null;
        
        return TimetableResponseDTO.builder()
                .id(t.getId())
                .subject(t.getSubject())
                .dayOfWeek(t.getDayOfWeek() != null ? t.getDayOfWeek().name() : null)
                .startTime(t.getStartTime() != null ? t.getStartTime().toString() : null)
                .endTime(t.getEndTime() != null ? t.getEndTime().toString() : null)
                .isExamDay(t.getIsExamDay())
                .isHoliday(t.getIsHoliday())
                .holidayDate(t.getHolidayDate() != null ? t.getHolidayDate().toString() : null)
                .isAdhoc(t.getIsAdhoc())
                .startDate(t.getStartDate() != null ? t.getStartDate().toString() : null)
                .endDate(t.getEndDate() != null ? t.getEndDate().toString() : null)
                .description(t.getDescription())
                // Relation Info
                .room(t.getRoom() != null ? TimetableResponseDTO.RoomInfo.builder()
                        .id(t.getRoom().getId())
                        .name(t.getRoom().getName())
                        .building(t.getRoom().getBuilding())
                        .floor(t.getRoom().getFloor())
                        .build() : null)
                .faculty(t.getFaculty() != null ? TimetableResponseDTO.FacultyInfo.builder()
                        .id(t.getFaculty().getId())
                        .name(t.getFaculty().getName())
                        .email(t.getFaculty().getEmail())
                        .build() : null)
                .section(t.getSection() != null ? TimetableResponseDTO.SectionInfo.builder()
                        .id(t.getSection().getId())
                        .name(t.getSection().getName())
                        .build() : null)
                // Break Info
                .hasLunchBreak(t.getHasLunchBreak())
                .lunchBreakStart(t.getLunchBreakStart() != null ? t.getLunchBreakStart().toString() : null)
                .lunchBreakEnd(t.getLunchBreakEnd() != null ? t.getLunchBreakEnd().toString() : null)
                .hasShortBreak(t.getHasShortBreak())
                .shortBreakStart(t.getShortBreakStart() != null ? t.getShortBreakStart().toString() : null)
                .shortBreakEnd(t.getShortBreakEnd() != null ? t.getShortBreakEnd().toString() : null)
                .build();
    }
}
