package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.service.v1.AttendanceV1Service;
import com.example.smartAttendence.service.SensorFusionService;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import com.example.smartAttendence.util.SecurityUtils;
import com.example.smartAttendence.exception.SpoofingException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceV1Controller {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceV1Controller.class);

    private final AttendanceV1Service attendanceService;
    private final SensorFusionService sensorFusionService;
    private final AILearningOptimizer aiLearningOptimizer;
    private final SecurityUtils securityUtils;
    private final Cache<String, Long> pulseThrottleCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();
    private final UserV1Repository userRepository;
    private final com.example.smartAttendence.repository.v1.SecurityAlertV1Repository securityAlertRepository;

    public AttendanceV1Controller(
            AttendanceV1Service attendanceService,
            SensorFusionService sensorFusionService,
            AILearningOptimizer aiLearningOptimizer,
            SecurityUtils securityUtils,
            UserV1Repository userRepository,
            com.example.smartAttendence.repository.v1.SecurityAlertV1Repository securityAlertRepository) {
        this.attendanceService = attendanceService;
        this.sensorFusionService = sensorFusionService;
        this.aiLearningOptimizer = aiLearningOptimizer;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.securityAlertRepository = securityAlertRepository;
    }

    @PostMapping("/heartbeat")
    @RateLimiter(name = "attendance-heartbeat", fallbackMethod = "heartbeatFallback")
    public ResponseEntity<?> heartbeat(
            @RequestBody EnhancedHeartbeatPing ping,
            HttpServletRequest httpServletRequest
    ) {
        Object isCellularAttr = httpServletRequest.getAttribute("isCellularData");
        boolean isCellular = isCellularAttr instanceof Boolean && (Boolean) isCellularAttr;

        attendanceService.processEnhancedHeartbeat(ping, isCellular);

        return ResponseEntity.ok(Map.of(
                "message", "Heartbeat recorded successfully"
        ));
    }
    
    public ResponseEntity<?> heartbeatFallback(EnhancedHeartbeatPing ping, HttpServletRequest httpServletRequest, Exception ex) {
        return ResponseEntity.status(429).body(Map.of(
                "error", "Too many heartbeat requests. Please reduce frequency.",
                "message", "Heartbeat rate limit exceeded. Try again in a few seconds."
        ));
    }

    @PostMapping("/hall-pass")
    public ResponseEntity<?> grantHallPass(
            @RequestBody HallPassRequestDTO request
    ) {
        attendanceService.grantHallPass(request);
        return ResponseEntity.ok(Map.of(
                "message", "Hall pass granted for " + request.requestedMinutes() + " minutes."
        ));
    }

    /**
     * Get the currently active session for the authenticated student
     */
    @GetMapping("/session/active")
    public ResponseEntity<?> getActiveSession() {
        return securityUtils.getCurrentUser().map(user -> {
            try {
                logger.info("🔍 AI SESSION LOOKUP: Finding active session for student: {}", user.getEmail());
                var session = attendanceService.getActiveSessionForUser(user.getId());
                
                // Use a safe map to avoid NullPointerException with Map.of()
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("id", session.getId());
                response.put("subject", session.getSubject() != null ? session.getSubject() : "Unknown");
                response.put("startTime", session.getStartTime());
                response.put("endTime", session.getEndTime());
                
                logger.info("✅ AI SESSION FOUND: {} (ID: {})", session.getSubject(), session.getId());
                return ResponseEntity.ok(response);
            } catch (IllegalStateException e) {
                logger.warn("⚠️ AI SESSION NOT FOUND: {}", e.getMessage());
                return ResponseEntity.status(404).body(java.util.Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.error("❌ AI SESSION ERROR: {}", e.getMessage(), e);
                java.util.Map<String, Object> errorMap = new java.util.HashMap<>();
                errorMap.put("error", "Internal Server Error");
                errorMap.put("message", e.getMessage() != null ? e.getMessage() : "An unexpected null error occurred");
                return ResponseEntity.status(500).body(errorMap);
            }
        }).orElse(ResponseEntity.status(401).body(java.util.Map.of("error", "Authentication required")));
    }

    // ========== ENHANCED ATTENDANCE FEATURES ==========

    /**
     * Enhanced heartbeat endpoint with sensor fusion and spoofing detection
     */
    @PostMapping("/heartbeat-enhanced")
    public ResponseEntity<?> enhancedHeartbeat(
            @RequestBody EnhancedHeartbeatPing ping,
            HttpServletRequest httpServletRequest
    ) {
        Object isCellularAttr = httpServletRequest.getAttribute("isCellularData");
        boolean isCellular = isCellularAttr instanceof Boolean && (Boolean) isCellularAttr;

        // 🛡️ SMART SECURITY: Individual Student Throttling (Prevents spam from single user)
        String userRateKey = "pulse:" + ping.studentId();
        if (pulseThrottleCache.getIfPresent(userRateKey) != null) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Too many requests from your device",
                "message", "Please wait for the current heartbeat window to complete."
            ));
        }
        // Set a 5-second mandatory cooldown for this specific student locally
        pulseThrottleCache.put(userRateKey, System.currentTimeMillis());

        try {
            // Process sensor fusion and spoofing detection
            sensorFusionService.processEnhancedHeartbeat(ping);
            
            // Get recent readings for spoofing detection
            var recentReadings = sensorFusionService.getRecentReadings(ping.studentId(), ping.sessionId(), 10);
            boolean spoofingDetected = sensorFusionService.detectSpoofing(recentReadings);
            
            if (spoofingDetected) {
                // 🔐 SECURITY: Log spoofing attempt securely
                logger.warn("Location spoofing detected for student: {}", ping.studentId());
                
                // Save Security Alert so it appears on Dashboard (Unifed via Service)
                securityUtils.getCurrentUser().ifPresent(student -> {
                    attendanceService.logSecurityAlert(
                        student, 
                        "LOCATION_SPOOFING", 
                        "High-acceleration anomaly detected (Possible sensor hack/spoof).", 
                        "HIGH", 
                        0.95
                    );
                    
                    // Mark as ABSENT for security violation (Implicitly handled by processEnhancedHeartbeat next)
                    attendanceService.processEnhancedHeartbeat(ping, isCellular);
                });

                return ResponseEntity.status(403)
                    .body(Map.of(
                        "error", "Spoofing detected",
                        "reason", "Unusual sensor patterns detected (Security Violation)",
                        "severity", "HIGH",
                        "timestamp", java.time.Instant.now()
                    ));
            }

            // 🔋 BATTERY OPTIMIZATION LOGIC
            Long recommendedInterval = calculateOptimalHeartbeatInterval(ping);
            
            // 🛰️ PHASE 2: ADAPTIVE GPS OPTIMIZATION
            var gpsOptimization = sensorFusionService.determineOptimalGPSMode(ping);
            var recentReadingsForGPS = sensorFusionService.getRecentReadings(ping.studentId(), ping.sessionId(), 5);
            boolean needsHighAccuracy = sensorFusionService.needsHighAccuracyGPS(ping, recentReadingsForGPS);
            
            // 🤖 PHASE 3: AI LEARNING OPTIMIZATION
            var aiOptimization = aiLearningOptimizer.optimizeForStudent(ping);
            
            // Process enhanced heartbeat with time-based smoothing
            attendanceService.processEnhancedHeartbeat(ping, isCellular);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Enhanced heartbeat processed successfully");
            response.put("timestamp", java.time.Instant.now());
            response.put("sensorDataProcessed", true);
            
            java.util.Map<String, Object> batteryOpt = new java.util.HashMap<>();
            batteryOpt.put("currentBatteryLevel", ping.batteryLevel() != null ? ping.batteryLevel() : 100);
            batteryOpt.put("deviceState", ping.deviceState() != null ? ping.deviceState() : "UNKNOWN");
            batteryOpt.put("recommendedInterval", recommendedInterval);
            batteryOpt.put("batteryMode", getBatteryMode(ping.batteryLevel()));
            batteryOpt.put("lowPowerAction", ping.batteryLevel() != null && ping.batteryLevel() < 20 ? "MINIMIZE_GPS" : "NORMAL");
            response.put("batteryOptimization", batteryOpt);

            // 🛡️ AI SECURITY STATUS
            java.util.Map<String, Object> securityStatus = new java.util.HashMap<>();
            User student = userRepository.findById(ping.studentId()).orElse(null);
            if (student != null) {
                securityStatus.put("hardwareVerified", student.getDeviceId() != null && student.getDeviceId().equals(ping.deviceFingerprint()));
                securityStatus.put("biometricVerified", student.getBiometricSignature() != null && student.getBiometricSignature().equals(ping.biometricSignature()));
                securityStatus.put("deviceLocked", student.getDeviceId() != null);
            }
            response.put("securityVerification", securityStatus);

            java.util.Map<String, Object> gpsOpt = new java.util.HashMap<>();
            gpsOpt.put("gpsMode", gpsOptimization.mode().toString());
            gpsOpt.put("accuracyMeters", gpsOptimization.accuracyMeters());
            gpsOpt.put("updateIntervalMs", gpsOptimization.updateIntervalMs());
            gpsOpt.put("reason", gpsOptimization.reason());
            gpsOpt.put("needsHighAccuracy", needsHighAccuracy);
            gpsOpt.put("deviceMotionState", gpsOptimization.deviceState());
            response.put("gpsOptimization", gpsOpt);

            java.util.Map<String, Object> aiOpt = new java.util.HashMap<>();
            aiOpt.put("optimalHeartbeatInterval", aiOptimization.optimalHeartbeatInterval());
            aiOpt.put("recommendedGPSMode", aiOptimization.recommendedGPSMode());
            aiOpt.put("confidence", aiOptimization.confidence());
            aiOpt.put("reasoning", aiOptimization.reasoning());
            aiOpt.put("totalSessionsAnalyzed", aiOptimization.totalSessionsAnalyzed());
            aiOpt.put("accuracyScore", aiOptimization.accuracyScore());
            aiOpt.put("learningStatus", aiOptimization.confidence() > 0.8 ? "HIGH_CONFIDENCE" : "LEARNING");
            response.put("aiLearning", aiOpt);

            return ResponseEntity.ok(response);

        } catch (SpoofingException e) {
            return ResponseEntity.status(403)
                .body(Map.of(
                    "error", "Spoofing detected",
                    "reason", e.getReason(),
                    "severity", e.getSeverity(),
                    "timestamp", java.time.Instant.now()
                ));
        } catch (Exception e) {
            java.util.Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "Failed to process enhanced heartbeat");
            errorMap.put("message", e.getMessage() != null ? e.getMessage() : "Unknown internal error");
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * 🤖 AI FEEDBACK LEARNING ENDPOINT
     * Allows faculty to correct an attendance record
     */
    @PostMapping("/manual-correction")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> manualCorrection(
            @Valid @RequestBody com.example.smartAttendence.dto.v1.AttendanceCorrectionRequest request
    ) {
        try {
            attendanceService.correctAttendance(
                request.recordId(), 
                request.newStatus(), 
                request.correctorId()
            );

            return ResponseEntity.ok(Map.of(
                "message", "Attendance corrected successfully. AI learning feedback processed.",
                "recordId", request.recordId(),
                "newStatus", request.newStatus(),
                "timestamp", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            java.util.Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "Failed to process manual correction");
            errorMap.put("message", e.getMessage() != null ? e.getMessage() : "Internal error during correction");
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * Fallback method for rate limiting
     */
    public ResponseEntity<?> enhancedHeartbeatFallback(EnhancedHeartbeatPing ping, HttpServletRequest httpServletRequest, Exception ex) {
        return ResponseEntity.status(429)
            .body(Map.of(
                "error", "Too many enhanced heartbeat requests. Please reduce frequency.",
                "message", "Enhanced heartbeat rate limit exceeded. Try again in a few seconds."
            ));
    }

    /**
     * Get sensor fusion status for a student in a session
     */
    @GetMapping("/sensor-status/{sessionId}/{studentId}")
    public ResponseEntity<?> getSensorStatus(
            @PathVariable String sessionId,
            @PathVariable String studentId
    ) {
        try {
            var recentReadings = sensorFusionService.getRecentReadings(
                java.util.UUID.fromString(studentId),
                java.util.UUID.fromString(sessionId),
                10 // Last 10 minutes
            );

            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "studentId", studentId,
                "recentReadingsCount", recentReadings.size(),
                "lastReading", recentReadings.isEmpty() ? null : recentReadings.get(0).getReadingTimestamp(),
                "motionAnalysis", recentReadings.isEmpty() ? null : 
                    sensorFusionService.calculateMotionState(
                        new com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing(
                            java.util.UUID.fromString(studentId),
                            java.util.UUID.fromString(sessionId),
                            recentReadings.get(0).getLatitude(),
                            recentReadings.get(0).getLongitude(),
                            recentReadings.get(0).getStepCount(),
                            recentReadings.get(0).getAccelerationX(),
                            recentReadings.get(0).getAccelerationY(),
                            recentReadings.get(0).getAccelerationZ(),
                            recentReadings.get(0).getIsDeviceMoving(),
                            recentReadings.get(0).getReadingTimestamp(),
                            recentReadings.get(0).getDeviceFingerprint(),
                            null, // biometricSignature
                            null, // batteryLevel
                            null, // isCharging
                            null, // isScreenOn
                            null, // deviceState
                            null, // gpsAccuracy
                            null  // nextHeartbeatInterval
                        )
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of(
                    "error", "Failed to get sensor status",
                    "message", e.getMessage()
                ));
        }
    }

    // 🔋 BATTERY OPTIMIZATION METHODS
    
    /**
     * Calculate optimal heartbeat interval based on device state and battery
     */
    private Long calculateOptimalHeartbeatInterval(EnhancedHeartbeatPing ping) {
        // Base intervals in seconds
        long baseInterval = 30L; // Default 30 seconds
        
        // Adjust based on device state
        switch (ping.deviceState()) {
            case "STATIONARY":
                baseInterval = ping.isScreenOn() ? 60L : 120L; // 1-2 minutes
                break;
            case "MOVING":
                baseInterval = 15L; // 15 seconds (potential walk-out)
                break;
            case "WALKING":
                baseInterval = 10L; // 10 seconds (leaving class)
                break;
            default:
                baseInterval = 30L; // Default
        }
        
        // Adjust based on battery level
        if (ping.batteryLevel() != null) {
            if (ping.batteryLevel() < 20) {
                baseInterval = baseInterval * 3; // Triple interval for low battery
            } else if (ping.batteryLevel() < 50) {
                baseInterval = baseInterval * 2; // Double interval for medium battery
            }
        }
        
        // If charging, use normal intervals
        if (Boolean.TRUE.equals(ping.isCharging())) {
            baseInterval = Math.max(baseInterval / 2, 10L); // More frequent when charging
        }
        
        return Math.min(Math.max(baseInterval, 5L), 300L); // Between 5 seconds and 5 minutes
    }
    
    /**
     * Get battery mode description
     */
    private String getBatteryMode(Integer batteryLevel) {
        if (batteryLevel == null) return "UNKNOWN";
        
        if (batteryLevel < 20) return "EMERGENCY_MODE";
        if (batteryLevel < 50) return "BATTERY_SAVER";
        if (batteryLevel < 80) return "BALANCED";
        return "PERFORMANCE_MODE";
    }
}

