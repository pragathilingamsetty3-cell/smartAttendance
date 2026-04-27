package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.TimetableResponseDTO;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.StudentDashboardStatsDTO;
import com.example.smartAttendence.service.v1.AdminV1Service;
import com.example.smartAttendence.service.v1.StudentV1Service;
import com.example.smartAttendence.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.entity.Section;
import java.util.UUID;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@Slf4j
public class StudentV1Controller {

    private final StudentV1Service studentV1Service;
    private final AdminV1Service adminV1Service;
    private final SecurityUtils securityUtils;
    private final UserV1Repository userV1Repository;
    private final SectionRepository sectionRepository;

    /**
     * Get real-time dashboard statistics for student
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('STUDENT', 'CR', 'LR')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User student = adminV1Service.getUserByEmail(email);
            
            if (student == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Student profile not found"));
            }

            StudentDashboardStatsDTO stats = studentV1Service.getStudentDashboardStats(student.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Student dashboard sync failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Student dashboard sync failed: " + e.getMessage()));
        }
    }

    /**
     * Get complete student profile details
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT', 'CR', 'LR')")
    public ResponseEntity<?> getProfile() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            User student = adminV1Service.getUserByEmail(auth.getName());
            
            if (student == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Profile not found"));
            }

            java.util.Map<String, Object> profile = new java.util.HashMap<>();
            profile.put("id", student.getId());
            profile.put("name", student.getName());
            profile.put("email", student.getEmail());
            profile.put("registrationNumber", student.getRegistrationNumber());
            profile.put("department", student.getDepartment());
            profile.put("section", student.getSection() != null ? student.getSection().getName() : "N/A");
            profile.put("semester", student.getSemester() != null ? student.getSemester() : "N/A");
            profile.put("status", student.getStatus());
            profile.put("joinedAt", student.getCreatedAt());
            profile.put("studentMobile", student.getStudentMobile() != null ? student.getStudentMobile() : "N/A");
            profile.put("parentEmail", student.getParentEmail() != null ? student.getParentEmail() : "N/A");
            profile.put("parentMobile", student.getParentMobile() != null ? student.getParentMobile() : "N/A");

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch profile: " + e.getMessage()));
        }
    }

    /**
     * Get timetable for the student's section (Bypassing /admin security)
     */
    @GetMapping("/timetable")
    @PreAuthorize("hasAnyRole('STUDENT', 'CR', 'LR')")
    public ResponseEntity<?> getTimetable() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            User student = adminV1Service.getUserByEmail(auth.getName());
            
            if (student == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
            }

            UUID sectionId = student.getSectionId();
            
            // 🚨 RECOVERY: If ID is null, try to find by Name from the linked Section object
            if (sectionId == null && student.getSection() != null) {
                String sectionName = student.getSection().getName();
                log.warn("⚠️ [RECOVERY] Student {} has null SectionID. Searching by name: {}", student.getId(), sectionName);
                
                var sectionOpt = sectionRepository.findByName(sectionName);
                
                if (sectionOpt.isPresent()) {
                    Section s = sectionOpt.get();
                    sectionId = s.getId();
                    student.setSectionId(sectionId);
                    student.setSection(s);
                    userV1Repository.save(student);
                    log.info("✅ [RECOVERY] Repaired student {} with section ID {}", student.getId(), sectionId);
                }
            }

            if (sectionId == null) {
                log.error("❌ [FAILURE] Student {} has no section ID or Name link.", student.getId());
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            java.util.List<com.example.smartAttendence.entity.Timetable> timetables = 
                adminV1Service.getTimetablesForSection(sectionId);
            
            log.info("📅 [FETCH] Found {} timetable entries for section {}", timetables.size(), sectionId);
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .body(timetables.stream().map(this::mapToTimetableResponse).toList());
        } catch (Exception e) {
            log.error("Failed to fetch student timetable", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private TimetableResponseDTO mapToTimetableResponse(com.example.smartAttendence.entity.Timetable t) {
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
