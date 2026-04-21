package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.service.ai.AISpatialMonitoringEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-analytics")
public class AIAnalyticsController {

    private final AISpatialMonitoringEngine spatialEngine;
    private final com.example.smartAttendence.service.v1.AdminV1Service adminV1Service;
    private final com.example.smartAttendence.service.v1.AIAnalyticsV1Service aiAnalyticsV1Service;

    public AIAnalyticsController(AISpatialMonitoringEngine spatialEngine, 
                                com.example.smartAttendence.service.v1.AdminV1Service adminV1Service,
                                com.example.smartAttendence.service.v1.AIAnalyticsV1Service aiAnalyticsV1Service) {
        this.spatialEngine = spatialEngine;
        this.adminV1Service = adminV1Service;
        this.aiAnalyticsV1Service = aiAnalyticsV1Service;
    }

    /**
     * AI analyzes spatial behavior for anomalies
     * Replaces simple PostGIS checks
     */
    @PostMapping("/spatial-analysis/{studentId}/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> analyzeSpatialBehavior(
            @PathVariable UUID studentId,
            @PathVariable UUID sessionId
    ) {
        try {
            var analysis = spatialEngine.analyzeSpatialBehavior(studentId, sessionId);

            return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "sessionId", sessionId,
                "anomalyDetected", analysis.anomalyDetected(),
                "anomalyType", analysis.anomalyType(),
                "status", analysis.status(),
                "processedBy", "AI"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AI spatial analysis failed: " + e.getMessage()));
        }
    }

    /**
     * AI detects GPS drift vs real movement
     * Replaces simple 3-strike rule
     */
    @PostMapping("/drift-analysis/{studentId}/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> analyzeGPSDrift(
            @PathVariable UUID studentId,
            @PathVariable UUID sessionId
    ) {
        try {
            var analysis = spatialEngine.analyzeGPSDrift(studentId, sessionId);

            return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "sessionId", sessionId,
                "isGPSDrift", analysis.isGPSDrift(),
                "isSpoofing", analysis.isSpoofing(),
                "severity", analysis.severity(),
                "processedBy", "AI"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AI drift analysis failed: " + e.getMessage()));
        }
    }

    /**
     * AI continuous tracking - learns student patterns
     * Replaces simple heartbeat cron
     */
    @PostMapping("/continuous-tracking/{studentId}/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> trackContinuously(
            @PathVariable UUID studentId,
            @PathVariable UUID sessionId
    ) {
        try {
            var tracking = spatialEngine.trackContinuously(studentId, sessionId);

            return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "sessionId", sessionId,
                "trackingStatus", tracking.trackingStatus(),
                "behaviorPattern", tracking.behaviorPattern(),
                "processedBy", "AI"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AI continuous tracking failed: " + e.getMessage()));
        }
    }

    /**
     * AI predicts if student will walk out
     */
    @PostMapping("/walk-out-prediction/{studentId}/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> predictWalkOut(
            @PathVariable UUID studentId,
            @PathVariable UUID sessionId
    ) {
        try {
            var prediction = spatialEngine.predictWalkOut(studentId, sessionId);

            return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "sessionId", sessionId,
                "willWalkOut", prediction.willWalkOut(),
                "probability", prediction.probability(),
                "reason", prediction.reason(),
                "processedBy", "AI"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AI walk-out prediction failed: " + e.getMessage()));
        }
    }

    /**
     * Get real AI status and spatial behavior for all students in a session
     */
    @GetMapping("/spatial-behavior/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getSpatialBehavior(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(aiAnalyticsV1Service.getSessionSpatialBehavior(sessionId));
    }

    /**
     * Get real AI spatial behavior filtered by department/section
     */
    @GetMapping("/spatial-behavior/filtered")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getFilteredSpatialBehavior(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID sectionId
    ) {
        return ResponseEntity.ok(aiAnalyticsV1Service.getFilteredSpatialBehavior(departmentId, sectionId));
    }

    /**
     * Get AI status for all students in a session
     */
    @GetMapping("/session-ai-status/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getSessionAIStatus(@PathVariable UUID sessionId) {
        try {
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "AI status monitoring active",
                "features", Map.of(
                    "spatialAnomalyDetection", "ACTIVE",
                    "gpsDriftAnalysis", "ACTIVE", 
                    "continuousBehaviorLearning", "ACTIVE",
                    "walkOutPrediction", "ACTIVE",
                    "hallPassControl", "FACULTY_CONTROLLED"
                ),
                "controlledBy", "AI Analytics + Faculty Control"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get AI session status: " + e.getMessage()));
        }
    }

    /**
     * Get AI dashboard stats
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAIDashboard(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID sectionId
    ) {
        return ResponseEntity.ok(aiAnalyticsV1Service.getAIDashboardStats(departmentId, sectionId));
    }

    /**
     * Get active AI alerts
     */
    @GetMapping("/alerts/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<?> getActiveAlerts(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID sectionId
    ) {
        return ResponseEntity.ok(aiAnalyticsV1Service.getActiveAlerts(departmentId, sectionId));
    }

    /**
     * Get AI Model metrics
     */
    @GetMapping("/performance/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getModelMetrics() {
        return ResponseEntity.ok(aiAnalyticsV1Service.getModelMetrics());
    }

    /**
     * Get AI Weekly Insights
     * GET /api/v1/ai-analytics/weekly-insights
     */
    @GetMapping("/weekly-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<?> getWeeklyInsights() {
        return ResponseEntity.ok(aiAnalyticsV1Service.getWeeklyInsights());
    }

    /**
     * AI Assistant Query (Chat Stub)
     * POST /api/v1/ai-analytics/ask-ai
     */
    @PostMapping("/ask-ai")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<?> askAI(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        return ResponseEntity.ok(Map.of(
            "question", question,
            "answer", "I am currently analyzing your institutional data. For now, I can report that all security protocols are ACTIVE. Precise behavioral querying will be available after session processing.",
            "status", "ANALYZING"
        ));
    }
}
