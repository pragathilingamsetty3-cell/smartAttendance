package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.ExamBarcodeScanRequest;
import com.example.smartAttendence.service.v1.ExamDayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/exam")
public class ExamDayController {

    private final ExamDayService examDayService;

    public ExamDayController(ExamDayService examDayService) {
        this.examDayService = examDayService;
    }

    /**
     * Scan student ID barcode for exam day attendance
     * Only faculty can perform this action
     */
    @PostMapping("/scan-barcode")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> scanStudentBarcode(@Valid @RequestBody ExamBarcodeScanRequest request) {
        try {
            var result = examDayService.processBarcodeScan(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Barcode scanned successfully",
                "studentId", result.getStudentId(),
                "studentName", result.getStudentName(),
                "registrationNumber", result.getRegistrationNumber(),
                "status", result.getStatus(),
                "scanTime", result.getScanTime()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process barcode scan: " + e.getMessage()));
        }
    }

    /**
     * Get exam session details for faculty
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getExamSession(@PathVariable String sessionId) {
        try {
            var session = examDayService.getExamSession(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "subject", session.getSubject(),
                "room", session.getRoom().getName(),
                "startTime", session.getStartTime(),
                "endTime", session.getEndTime(),
                "totalStudents", session.getTotalStudents(),
                "scannedStudents", session.getScannedStudents(),
                "isActive", session.isActive()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get exam session: " + e.getMessage()));
        }
    }

    /**
     * Get all exam sessions for current faculty today
     */
    @GetMapping("/today-sessions")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getTodayExamSessions() {
        try {
            var sessions = examDayService.getTodayExamSessions();
            
            return ResponseEntity.ok(Map.of(
                "sessions", sessions,
                "totalSessions", sessions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get today's exam sessions: " + e.getMessage()));
        }
    }
}
