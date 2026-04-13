package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.*;
import com.example.smartAttendence.service.v1.FacultyV1Service;
import com.example.smartAttendence.service.v1.FacultyHallPassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/faculty")
public class FacultyV1Controller {

    private final FacultyV1Service facultyV1Service;
    private final FacultyHallPassService facultyHallPassService;
    private final com.example.smartAttendence.service.v1.AdminV1Service adminService;
    private final com.example.smartAttendence.util.SecurityUtils securityUtils;
    private final com.example.smartAttendence.repository.SectionRepository sectionRepository;

    public FacultyV1Controller(
            FacultyV1Service facultyV1Service, 
            FacultyHallPassService facultyHallPassService, 
            com.example.smartAttendence.service.v1.AdminV1Service adminService,
            com.example.smartAttendence.util.SecurityUtils securityUtils,
            com.example.smartAttendence.repository.SectionRepository sectionRepository) {
        this.facultyV1Service = facultyV1Service;
        this.facultyHallPassService = facultyHallPassService;
        this.adminService = adminService;
        this.securityUtils = securityUtils;
        this.sectionRepository = sectionRepository;
    }

    /**
     * Get all sections for the current faculty's department
     */
    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getMySections() {
        try {
            // 1. If Super Admin, return all sections
            if (securityUtils.isSuperAdmin()) {
                return ResponseEntity.ok(sectionRepository.findAll().stream()
                    .map(s -> Map.of("id", s.getId(), "name", s.getName()))
                    .toList());
            }

            // 2. Otherwise, filter by department (Faculty or Admin)
            java.util.UUID deptId = securityUtils.getCurrentUserDepartmentId()
                .orElseThrow(() -> new RuntimeException("No department assigned to your profile"));
            
            return ResponseEntity.ok(sectionRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(deptId))
                .map(s -> Map.of("id", s.getId(), "name", s.getName()))
                .toList());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get real-time dashboard statistics for faculty
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            // Get current user from security context
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            // Fetch faculty user details
            com.example.smartAttendence.domain.User faculty = adminService.getUserByEmail(email);
            
            if (faculty == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Faculty profile not found"));
            }

            return ResponseEntity.ok(facultyV1Service.getFacultyDashboardStats(faculty.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Dashboard sync failed: " + e.getMessage()));
        }
    }

    // ==========================================
    // HALL PASS ENDPOINTS
    // ==========================================

    /**
     * Students request hall passes
     */
    @PostMapping("/hall-pass/request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> requestHallPass(@Valid @RequestBody HallPassRequestDTO request) {
        try {
            facultyHallPassService.createHallPassRequest(request);

            return ResponseEntity.ok(Map.of(
                    "message", "Hall pass request submitted to faculty",
                    "studentId", request.studentId(),
                    "sessionId", request.sessionId(),
                    "status", "PENDING_FACULTY_APPROVAL"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to submit hall pass request: " + e.getMessage()));
        }
    }

    /**
     * Faculty views pending hall pass requests
     */
    @GetMapping("/hall-pass/pending")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getPendingHallPassRequests() {
        try {
            List<HallPassStatusDTO> pendingRequests = facultyHallPassService.getPendingHallPassRequests();

            return ResponseEntity.ok(Map.of(
                    "pendingRequests", pendingRequests,
                    "total", pendingRequests.size(),
                    "message", "Faculty control - pending hall pass requests"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get pending requests: " + e.getMessage()));
        }
    }

    /**
     * Faculty approves hall pass requests
     * This is the ONLY manual intervention faculty needs
     */
    @PostMapping("/hall-pass/approve")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> approveHallPass(@Valid @RequestBody HallPassApprovalRequest request) {
        try {
            facultyHallPassService.approveHallPass(request);

            return ResponseEntity.ok(Map.of(
                    "message", "Hall pass approved by faculty",
                    "studentId", request.studentId(),
                    "approvedMinutes", request.approvedMinutes(),
                    "approvedBy", "FACULTY",
                    "timestamp", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to approve hall pass: " + e.getMessage()));
        }
    }

    /**
     * Faculty denies hall pass requests
     */
    @PostMapping("/hall-pass/deny")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> denyHallPass(@Valid @RequestBody HallPassDenialRequest request) {
        try {
            facultyHallPassService.denyHallPass(request);

            return ResponseEntity.ok(Map.of(
                    "message", "Hall pass denied by faculty",
                    "studentId", request.studentId(),
                    "reason", request.reason(),
                    "deniedBy", "FACULTY",
                    "timestamp", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to deny hall pass: " + e.getMessage()));
        }
    }

    /**
     * Get hall pass history for faculty's sessions
     */
    @GetMapping("/hall-pass/history/{sessionId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getHallPassHistory(@PathVariable UUID sessionId) {
        try {
            List<HallPassStatusDTO> history = facultyHallPassService.getHallPassHistory(sessionId);

            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "history", history,
                    "total", history.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get hall pass history: " + e.getMessage()));
        }
    }
}