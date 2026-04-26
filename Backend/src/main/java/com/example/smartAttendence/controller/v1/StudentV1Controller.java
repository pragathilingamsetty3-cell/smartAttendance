package com.example.smartAttendence.controller.v1;

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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@Slf4j
public class StudentV1Controller {

    private final StudentV1Service studentV1Service;
    private final AdminV1Service adminV1Service;
    private final SecurityUtils securityUtils;

    /**
     * Get real-time dashboard statistics for student
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('STUDENT')")
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getProfile() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            User student = adminV1Service.getUserByEmail(auth.getName());
            
            if (student == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Profile not found"));
            }

            return ResponseEntity.ok(Map.of(
                "id", student.getId(),
                "name", student.getName(),
                "email", student.getEmail(),
                "registrationNumber", student.getRegistrationNumber(),
                "department", student.getDepartment(),
                "section", student.getSection() != null ? student.getSection().getName() : "N/A",
                "semester", student.getSemester() != null ? student.getSemester() : "N/A",
                "status", student.getStatus(),
                "joinedAt", student.getCreatedAt(),
                "studentMobile", student.getStudentMobile() != null ? student.getStudentMobile() : "N/A",
                "parentEmail", student.getParentEmail() != null ? student.getParentEmail() : "N/A",
                "parentMobile", student.getParentMobile() != null ? student.getParentMobile() : "N/A"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch profile: " + e.getMessage()));
        }
    }
}
