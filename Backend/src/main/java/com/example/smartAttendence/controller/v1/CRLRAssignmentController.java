package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.CRLRAssignmentRequest;
import com.example.smartAttendence.entity.CRLRAssignment;
import com.example.smartAttendence.service.v1.CRLRAssignmentService;
import java.security.Principal;
import com.example.smartAttendence.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cr-lr-assignments")
public class CRLRAssignmentController {
    
    private final CRLRAssignmentService assignmentService;
    
    public CRLRAssignmentController(CRLRAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }
    
    /**
     * Assign CR/LR role to a student (Faculty Coordinator only)
     * POST /api/v1/cr-lr-assignments/assign
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Map<String, Object>> assignCRLRRole(
            @Valid @RequestBody CRLRAssignmentRequest request,
            Principal principal) {
        
        String email = principal.getName();
        try {
            CRLRAssignment assignment = assignmentService.assignCRLRRole(request, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "CR/LR role assigned successfully");
            response.put("assignmentId", assignment.getId());
            response.put("studentId", assignment.getUser().getId());
            response.put("studentName", assignment.getUser().getName());
            response.put("sectionId", assignment.getSection().getId());
            response.put("sectionName", assignment.getSection().getName());
            response.put("roleType", assignment.getRoleType());
            response.put("academicYear", assignment.getAcademicYear());
            response.put("semester", assignment.getSemester());
            response.put("assignedBy", assignment.getAssignedBy());
            response.put("assignedAt", assignment.getAssignedAt());
            
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to assign CR/LR role: " + e.getMessage()));
        }
    }
    
    /**
     * Revoke CR/LR role from a student (Faculty Coordinator only)
     * POST /api/v1/cr-lr-assignments/{assignmentId}/revoke
     */
    @PostMapping("/{assignmentId}/revoke")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Map<String, Object>> revokeCRLRRole(
            @PathVariable UUID assignmentId,
            @RequestParam String reason,
            Principal principal) {
        
        String email = principal.getName();
        try {
            CRLRAssignment assignment = assignmentService.revokeCRLRRole(assignmentId, reason, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "CR/LR role revoked successfully");
            response.put("assignmentId", assignment.getId());
            response.put("studentId", assignment.getUser().getId());
            response.put("studentName", assignment.getUser().getName());
            response.put("sectionId", assignment.getSection().getId());
            response.put("sectionName", assignment.getSection().getName());
            response.put("roleType", assignment.getRoleType());
            response.put("revokedBy", assignment.getRevokedBy());
            response.put("revokedAt", assignment.getRevokedAt());
            response.put("revocationReason", assignment.getRevocationReason());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to revoke CR/LR role: " + e.getMessage()));
        }
    }
    
    /**
     * Get all CR/LR assignments for a section
     * GET /api/v1/cr-lr-assignments/section/{sectionId}
     */
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSectionAssignments(@PathVariable UUID sectionId) {
        
        try {
            List<CRLRAssignment> assignments = assignmentService.getSectionAssignments(sectionId);
            
            return ResponseEntity.ok(Map.of(
                "sectionId", sectionId,
                "assignments", assignments.stream().map(assignment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("assignmentId", assignment.getId());
                    map.put("studentId", assignment.getUser().getId());
                    map.put("studentName", assignment.getUser().getName());
                    map.put("studentEmail", assignment.getUser().getEmail());
                    map.put("roleType", assignment.getRoleType());
                    map.put("assignedAt", assignment.getAssignedAt());
                    map.put("assignedBy", assignment.getAssignedBy());
                    map.put("academicYear", assignment.getAcademicYear());
                    map.put("semester", assignment.getSemester());
                    map.put("notes", assignment.getNotes());
                    return map;
                }).toList(),
                "totalAssignments", assignments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get section assignments: " + e.getMessage()));
        }
    }
    
    /**
     * Get CR/LR assignment history for a student
     * GET /api/v1/cr-lr-assignments/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStudentAssignmentHistory(@PathVariable UUID studentId) {
        
        try {
            List<CRLRAssignment> assignments = assignmentService.getStudentAssignmentHistory(studentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", studentId);
            response.put("assignments", assignments.stream().map(assignment -> {
                Map<String, Object> assignmentMap = new HashMap<>();
                assignmentMap.put("assignmentId", assignment.getId());
                assignmentMap.put("sectionId", assignment.getSection().getId());
                assignmentMap.put("sectionName", assignment.getSection().getName());
                assignmentMap.put("roleType", assignment.getRoleType());
                assignmentMap.put("assignedAt", assignment.getAssignedAt());
                assignmentMap.put("assignedBy", assignment.getAssignedBy());
                assignmentMap.put("active", assignment.getActive());
                assignmentMap.put("academicYear", assignment.getAcademicYear());
                assignmentMap.put("semester", assignment.getSemester());
                assignmentMap.put("notes", assignment.getNotes());
                assignmentMap.put("revokedAt", assignment.getRevokedAt());
                assignmentMap.put("revokedBy", assignment.getRevokedBy());
                assignmentMap.put("revocationReason", assignment.getRevocationReason());
                return assignmentMap;
            }).toList());
            response.put("totalAssignments", assignments.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get student assignment history: " + e.getMessage()));
        }
    }
    
    /**
     * Get all assignments managed by current coordinator
     * GET /api/v1/cr-lr-assignments/my-assignments
     */
    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Map<String, Object>> getCoordinatorAssignments(Principal principal) {
        
        String email = principal.getName();
        try {
            List<CRLRAssignment> assignments = assignmentService.getCoordinatorAssignments(email);
            
            return ResponseEntity.ok(Map.of(
                "assignments", assignments.stream().map(assignment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("assignmentId", assignment.getId());
                    map.put("studentId", assignment.getUser().getId());
                    map.put("studentName", assignment.getUser().getName());
                    map.put("sectionId", assignment.getSection().getId());
                    map.put("sectionName", assignment.getSection().getName());
                    map.put("roleType", assignment.getRoleType());
                    map.put("assignedAt", assignment.getAssignedAt());
                    map.put("active", assignment.getActive());
                    map.put("academicYear", assignment.getAcademicYear());
                    map.put("semester", assignment.getSemester());
                    return map;
                }).toList(),
                "totalAssignments", assignments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get coordinator assignments: " + e.getMessage()));
        }
    }
    
    /**
     * Quick assign CR to a student
     * POST /api/v1/cr-lr-assignments/quick-assign-cr
     */
    @PostMapping("/quick-assign-cr")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Map<String, Object>> quickAssignCR(
            @RequestParam UUID studentId,
            @RequestParam UUID sectionId,
            @RequestParam String academicYear,
            @RequestParam String semester,
            Principal principal) {
        
        String email = principal.getName();
        try {
            CRLRAssignmentRequest request = new CRLRAssignmentRequest(
                studentId, sectionId, CRLRAssignmentRequest.RoleType.CR,
                academicYear, semester, null, null, false
            );
            
            CRLRAssignment assignment = assignmentService.assignCRLRRole(request, email);
            
            return ResponseEntity.status(201).body(Map.of(
                "message", "CR role assigned successfully",
                "assignmentId", assignment.getId(),
                "studentId", assignment.getUser().getId(),
                "studentName", assignment.getUser().getName(),
                "sectionId", assignment.getSection().getId(),
                "sectionName", assignment.getSection().getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to assign CR role: " + e.getMessage()));
        }
    }
}
