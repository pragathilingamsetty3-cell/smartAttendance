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
            // Get current user from security context
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            // Fetch student user details
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
}
