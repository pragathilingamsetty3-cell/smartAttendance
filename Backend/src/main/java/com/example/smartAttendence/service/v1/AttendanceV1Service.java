package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.event.WalkOutEvent;
import com.example.smartAttendence.entity.SecurityAlert;
import com.example.smartAttendence.exception.OutsideGeofenceException;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.SecurityAlertV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.smartAttendence.security.SecurityAuditLogger;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceV1Service {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceV1Service.class);

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final String DRIFT_KEY_PATTERN = "drift:%s:%s";

    private final ClassroomSessionV1Repository classroomSessionRepository;
    private final AttendanceRecordV1Repository attendanceRecordRepository;
    private final UserV1Repository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final HandlerInterceptor securityAuditLogger;
    private final AILearningOptimizer aiLearningOptimizer;
    private final SecurityAlertV1Repository securityAlertRepository;

    @Autowired
    public AttendanceV1Service(
            ClassroomSessionV1Repository classroomSessionRepository,
            AttendanceRecordV1Repository attendanceRecordRepository,
            UserV1Repository userRepository,
            StringRedisTemplate redisTemplate,
            ApplicationEventPublisher eventPublisher,
            HandlerInterceptor securityAuditLogger,
            AILearningOptimizer aiLearningOptimizer,
            SecurityAlertV1Repository securityAlertRepository
    ) {
        this.classroomSessionRepository = classroomSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.securityAuditLogger = securityAuditLogger;
        this.aiLearningOptimizer = aiLearningOptimizer;
        this.securityAlertRepository = securityAlertRepository;
    }

    /**
     * 🤖 AI AUTONOMOUS WATCHER
     * Runs every minute to enforce security grace periods for students who have stopped heartbeating.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void monitorGracePeriods() {
        try {
            List<ClassroomSession> activeSessions = classroomSessionRepository.findByActiveTrue();
            Instant now = Instant.now();

            for (ClassroomSession session : activeSessions) {
                // Get all students who were recently active in this session
                List<AttendanceRecord> latestRecords = attendanceRecordRepository.findBySessionIdOrderByRecordedAtDesc(session.getId());
                
                // Track unique students to process only their latest state
                java.util.Set<UUID> processedStudents = new java.util.HashSet<>();

                for (AttendanceRecord record : latestRecords) {
                    UUID studentId = record.getStudent().getId();
                    if (processedStudents.contains(studentId)) continue;
                    processedStudents.add(studentId);

                    // Skip if already absent
                    if ("ABSENT".equalsIgnoreCase(record.getStatus())) continue;

                    // 🎯 PRECISION WATCHER: Only transition students who are in WALK_OUT state
                    // If they are PRESENT, we assume they are still inside until a new heartbeat proves otherwise.
                    if (!"WALK_OUT".equalsIgnoreCase(record.getStatus())) continue;

                    // Check if they have been silent too long after a walk-out detection
                    Instant lastSeen = record.getRecordedAt();
                    int threshold = getWalkOutThresholdSeconds(session.getId());

                    // Enforce 5-minute grace period (300s + Buffer)
                    if (now.isAfter(lastSeen.plusSeconds(threshold + 60))) { 
                        // ⚠️ GRACE PERIOD EXPIRED: Student walked out and never returned
                        record.setStatus("ABSENT");
                        record.setRecordedAt(now);
                        record.setNote("AI Watcher: Grace period expired for Walk-out student.");
                        attendanceRecordRepository.save(record);
                        
                        logger.warn("🤖 AI WATCHER [EXIT-ENFORCED]: Student {} marked ABSENT after Walk-out timeout.", studentId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("AI Watcher Error: {}", e.getMessage());
        }
    }

    @Transactional
    public void grantHallPass(HallPassRequestDTO request) {
        Objects.requireNonNull(request, "request is required");
        UUID sessionId = Objects.requireNonNull(request.sessionId(), "sessionId is required");
        UUID studentId = Objects.requireNonNull(request.studentId(), "studentId is required");

        if (request.requestedMinutes() <= 0) {
            throw new IllegalArgumentException("requestedMinutes must be positive");
        }

        String hallPassKey = hallPassKey(sessionId, studentId);
        redisTemplate.opsForValue().set(hallPassKey, "ACTIVE");
        redisTemplate.expire(hallPassKey, java.time.Duration.ofMinutes(request.requestedMinutes()));
    }

    @Transactional
    public void processHeartbeat(com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing ping, boolean isCellular) {
        Objects.requireNonNull(ping, "ping is required");
        UUID sessionId = Objects.requireNonNull(ping.sessionId(), "sessionId is required");
        UUID studentId = Objects.requireNonNull(ping.studentId(), "studentId is required");

        String hallPassKey = hallPassKey(sessionId, studentId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(hallPassKey))) {
            // Digital Hall Pass active - AI sleeps for this heartbeat.
            return;
        }

        ClassroomSession session = classroomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getGeofencePolygon() == null) {
            throw new IllegalStateException("Session geofence is not configured: " + sessionId);
        }

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(ping.longitude(), ping.latitude()));
        point.setSRID(SRID_WGS84);

        boolean inside = session.getGeofencePolygon().contains(point);

        String driftKey = driftKey(sessionId, studentId);

        if (!inside) {
            Long driftCount = redisTemplate.opsForValue().increment(driftKey);
            if (driftCount != null && driftCount == 3L) {
                attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(
                                studentId, sessionId)
                        .ifPresent(record -> {
                            if (!"WALK_OUT".equalsIgnoreCase(record.getStatus())) {
                                record.setStatus("WALK_OUT");
                                attendanceRecordRepository.save(record);

                                WalkOutEvent event = new WalkOutEvent(studentId, sessionId, LocalDateTime.now());
                                eventPublisher.publishEvent(event);
                            }
                        });
            }
        } else {
            redisTemplate.delete(driftKey);
        }

        // Battery mode logging - no sensitive data exposure
        if (isCellular) {
            logger.debug("Heartbeat over cellular data for student {} in session {}", studentId, sessionId);
        }
    }

    /**
     * Enhanced heartbeat processing with sensor fusion and time-based smoothing
     */
    @Transactional
    public void processEnhancedHeartbeat(com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing ping, boolean isCellular) {
        Objects.requireNonNull(ping, "enhanced ping is required");
        UUID sessionId = Objects.requireNonNull(ping.sessionId(), "sessionId is required");
        UUID studentId = Objects.requireNonNull(ping.studentId(), "studentId");

        String hallPassKey = hallPassKey(sessionId, studentId);
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().get(hallPassKey))) {
            // Digital Hall Pass active - AI sleeps for this heartbeat.
            return;
        }
        
        // --- 🛡️ AI SECURITY: STRICT LOCKDOWN HANDSHAKE ---
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        // 1. Hardware Signature Lock (REQUIRED for every heartbeat)
        if (!verifyHardwareSignature(student, ping.deviceFingerprint())) {
            logger.warn("🛑 AI SECURITY [DEVICE_VIOLATION]: Student {} attempting login from unauthorized device!", student.getEmail());
            
            // ⚠️ Mark as ABSENT as per user request
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setSession(classroomSessionRepository.findById(sessionId).orElse(null));
            record.setStatus("ABSENT");
            record.setAiDecision(true);
            record.setConfidence(1.0);
            record.setNote("AI Security: Hardware signature mismatch (Device Spoofing).");
            attendanceRecordRepository.save(record);
            
            logSecurityAlert(student, "ABSENCE_DEVICE_LOG", "Device signature mismatch. Student marked ABSENT.", "HIGH", 1.0);
            
            return; // Stop processing this heartbeat
        }

        // 2. Biometric Signature Verification (ONLY for initial session check-in)
        boolean isInitialCheckIn = !attendanceRecordRepository.existsBySession_IdAndStudent_Id(sessionId, studentId);
        if (isInitialCheckIn) {
            if (ping.biometricSignature() == null || !verifyBiometricSignature(student, ping.biometricSignature())) {
                String reason = (ping.biometricSignature() == null) ? "MISSING_BIOMETRIC" : "BIOMETRIC_MISMATCH";
                logger.warn("🛑 AI SECURITY [{}]: Student {} failed biometric handshake!", reason, student.getEmail());
                
                // ⚠️ Mark as ABSENT as per user request
                AttendanceRecord record = new AttendanceRecord();
                record.setStudent(student);
                record.setSession(classroomSessionRepository.findById(sessionId).orElse(null));
                record.setStatus("ABSENT");
                record.setAiDecision(true);
                record.setConfidence(0.99);
                record.setNote("AI Security: Biometric verification failed.");
                attendanceRecordRepository.save(record);
                
                // Log but do NOT make it an "Anomaly" in the summary card 
                // We'll use a type that doesn't trigger the dashboard's anomaly allow-list
                logSecurityAlert(student, "ABSENCE_BIOMETRIC_LOG", "Biometric mismatch detected. Student marked ABSENT.", "MEDIUM", 0.99);
                
                return; // Stop processing this heartbeat
            }
        }
        
        // 🕐 AUTOMATIC BREAK PASS DURING SCHEDULED BREAKS
        if (hasAutomaticBreakPass(sessionId, studentId)) {
            // Student has automatic break pass - AI sleeps for this heartbeat
            return;
        }

        ClassroomSession session = classroomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getGeofencePolygon() == null) {
            throw new IllegalStateException("Session geofence is not configured: " + sessionId);
        }

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(ping.longitude(), ping.latitude()));
        point.setSRID(SRID_WGS84);

        boolean inside = session.getGeofencePolygon().contains(point);

        String driftKey = driftKey(sessionId, studentId);
        String driftTimestampKey = driftTimestampKey(sessionId, studentId);

        if (!inside) {
            // Store timestamp of first out-of-bounds detection
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(driftTimestampKey, Instant.now().toString()))) {
                redisTemplate.expire(driftTimestampKey, java.time.Duration.ofMinutes(10)); // 10-minute TTL
            }

            // 🛰️ LIVE TRACKING: Persist outside coordinates immediately for spatial visibility
            attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(studentId, sessionId)
                .ifPresent(record -> {
                    record.setLatitude(ping.latitude());
                    record.setLongitude(ping.longitude());
                    record.setGpsAccuracy(ping.gpsAccuracy() != null ? ping.gpsAccuracy() : 15.0);
                    record.setRecordedAt(Instant.now());
                    
                    // If they are outside, the AI marks the pattern as MOVING/ERRATIC
                    if (!"ABSENT".equalsIgnoreCase(record.getStatus())) {
                        record.setStatus("WALK_OUT"); // Provisional Walk-out
                    }
                    attendanceRecordRepository.save(record);
                });

            // 🕐 ENHANCED BREAK-TIME AWARE WALK-OUT DETECTION
            String firstOutOfBoundsTime = redisTemplate.opsForValue().get(driftTimestampKey);
            if (firstOutOfBoundsTime != null) {
                Instant firstTime = Instant.parse(firstOutOfBoundsTime);
                Instant now = Instant.now();
                
                // Get walk-out threshold based on timetable
                int walkOutThresholdSeconds = getWalkOutThresholdSeconds(sessionId);
                
                if (now.isAfter(firstTime.plusSeconds(walkOutThresholdSeconds))) {
                    // ⚠️ AUTO-ABSENCE: Student has been outside too long
                    attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(
                                    studentId, sessionId)
                            .ifPresent(record -> {
                                if (!"ABSENT".equalsIgnoreCase(record.getStatus())) {
                                    record.setStatus("ABSENT");
                                    record.setRecordedAt(Instant.now());
                                    record.setNote("AI Autonomous: Student marked ABSENT after " + (walkOutThresholdSeconds/60) + "m walk-out.");
                                    attendanceRecordRepository.save(record);

                                    WalkOutEvent event = new WalkOutEvent(studentId, sessionId, LocalDateTime.now());
                                    eventPublisher.publishEvent(event);
                                    
                                    logger.warn("🤖 AI MONITOR [RECOVR-FAIL]: Student {} failed to return. Status: ABSENT", studentId);
                                }
                            });
                    
                    // Clean up drift tracking
                    redisTemplate.delete(driftKey);
                    redisTemplate.delete(driftTimestampKey);
                }
            }
        } else {
            // Back inside geofence - clear drift tracking
            redisTemplate.delete(driftKey);
            redisTemplate.delete(driftTimestampKey);

            // Helper to resolve zombie alerts
            Runnable resolveAlerts = () -> {
                securityAlertRepository.findByUser_IdAndResolvedFalse(studentId).stream()
                    .filter(alert -> "AI_AUTO_ABSENT".equals(alert.getAlertType()) || "ABSENCE_ANOMALY".equals(alert.getAlertType()))
                    .forEach(alert -> {
                        alert.setResolved(true);
                        alert.setResolvedAt(java.time.LocalDateTime.now());
                        securityAlertRepository.save(alert);
                    });
            };

            // 🤖 AI AUTONOMOUS MARKING: Ensure active record exists
            attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(studentId, sessionId)
                .ifPresentOrElse(
                    record -> {
                        if ("ABSENT".equalsIgnoreCase(record.getStatus()) || "WALK_OUT".equalsIgnoreCase(record.getStatus())) {
                            String recoveryStatus = determineStatus(session, Instant.now());
                            record.setStatus(recoveryStatus);
                            record.setRecordedAt(Instant.now());
                            record.setLatitude(ping.latitude());
                            record.setLongitude(ping.longitude());
                            record.setDeviceSignature(ping.deviceFingerprint());
                            record.setBatteryLevel(ping.batteryLevel());
                            record.setMoving(Boolean.TRUE.equals(ping.isDeviceMoving()));
                            
                            double accel = Math.sqrt(
                                ping.accelerationX()*ping.accelerationX() + 
                                ping.accelerationY()*ping.accelerationY() + 
                                ping.accelerationZ()*ping.accelerationZ()
                            );
                            record.setAccelerationMagnitude(accel);
                            
                            attendanceRecordRepository.save(record);
                            logger.info("🤖 AI MONITOR [RECOVERY]: Student {} returned. Status updated to {}.", studentId, recoveryStatus);
                        }
                        resolveAlerts.run();
                    },
                    () -> {
                        // First valid heartbeat - Mark PRESENT or LATE automatically
                        AttendanceRecord record = new AttendanceRecord();
                        record.setStudent(student);
                        record.setSession(session);
                        String status = determineStatus(session, Instant.now());
                        record.setStatus(status);
                        
                        // Save security markers
                        record.setDeviceSignature(ping.deviceFingerprint());
                        record.setBatteryLevel(ping.batteryLevel());
                        record.setHardwareVerified(student.getDeviceId() != null && student.getDeviceId().equals(ping.deviceFingerprint()));
                        record.setBiometricVerified(student.getBiometricSignature() != null && student.getBiometricSignature().equals(ping.biometricSignature()));
                        
                        // 🚀 Persist Spatial & GPS Data
                        record.setLatitude(ping.latitude());
                        record.setLongitude(ping.longitude());
                        record.setMoving(Boolean.TRUE.equals(ping.isDeviceMoving()));
                        
                        double accel = Math.sqrt(
                            ping.accelerationX()*ping.accelerationX() + 
                            ping.accelerationY()*ping.accelerationY() + 
                            ping.accelerationZ()*ping.accelerationZ()
                        );
                        record.setAccelerationMagnitude(accel);

                        attendanceRecordRepository.save(record);
                        record.setStatus(status);
                        record.setRecordedAt(Instant.now());
                        record.setAiDecision(true);
                        record.setConfidence(0.99);
                        attendanceRecordRepository.save(record);
                        resolveAlerts.run();
                        logger.info("🤖 AI MONITOR [AUTO-PRESENT]: Student {} marked {} via zero-effort heartbeat.", studentId, status);
                    }
                );
        }

        // 'isCellular' is available for future AI rules; logged for now.
        if (isCellular) {
            logger.debug("Enhanced heartbeat over cellular data for student {} in session {}", studentId, sessionId);
        }
    }

    private void resolveAlerts(UUID studentId) {
        securityAlertRepository.findByUser_IdAndResolvedFalse(studentId).stream()
            .filter(alert -> "AI_AUTO_ABSENT".equals(alert.getAlertType()) || "ABSENCE_ANOMALY".equals(alert.getAlertType()))
            .forEach(alert -> {
                alert.setResolved(true);
                alert.setResolvedAt(java.time.LocalDateTime.now());
                securityAlertRepository.save(alert);
            });
    }

    /**
     * Finds the active session for a student based on their section
     */
    public ClassroomSession getActiveSessionForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        if (user.getSectionId() == null) {
            logger.error("❌ AI ERROR: Student {} has no assigned section_id!", user.getEmail());
            throw new IllegalStateException("Student has no assigned section");
        }

        logger.info("🔍 AI LOOKUP: Checking active sessions for Section: {} at {}", user.getSectionId(), Instant.now());
        List<ClassroomSession> activeSessions = classroomSessionRepository.findActiveSessionsForSection(
                user.getSectionId(), Instant.now());
        
        if (activeSessions.isEmpty()) {
            logger.warn("⚠️ AI WARNING: No active session found for Section ID: {}. Checking all active sessions...", user.getSectionId());
            // Fallback: Just for demo purposes, if no session for section, check if ANY session is active
            List<ClassroomSession> globalActive = classroomSessionRepository.findByActiveTrue();
            if (!globalActive.isEmpty()) {
                 logger.info("ℹ️ AI INFO: Found {} global active sessions. Using first for demo.", globalActive.size());
                 return globalActive.get(0);
            }
            throw new IllegalStateException("No active session found for your section right now.");
        }

        return activeSessions.get(0);
    }

    /**
     * 🤖 AI FEEDBACK LEARNING SERVICE
     * Corrects an attendance record and triggers AI learning
     */
    @Transactional
    public void correctAttendance(UUID recordId, String newStatus, UUID facultyId) {
        AttendanceRecord record = attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        String oldStatus = record.getStatus();
        
        // 1. Update the record
        record.setStatus(newStatus);
        record.setManuallyCorrected(true);
        record.setOriginalAiStatus(oldStatus);
        record.setCorrectorId(facultyId);
        record.setNote("Manual correction by facultyId: " + facultyId);
        
        attendanceRecordRepository.save(record);

        // 2. Trigger AI Learning Feedback
        aiLearningOptimizer.processCorrectionFeedback(record, newStatus);
        
        logger.info("🤖 AI LEARNING: Attendance corrected for student {} from {} to {}. Feedback sent to optimizer.", 
                record.getStudent().getId(), oldStatus, newStatus);
    }

    private String determineStatus(ClassroomSession session, Instant now) {
        // 10-minute grace period for 'PRESENT'
        Instant lateThreshold = session.getStartTime().plusSeconds(600);
        return now.isAfter(lateThreshold) ? "LATE" : "PRESENT";
    }

    private String driftTimestampKey(UUID sessionId, UUID studentId) {
        return DRIFT_KEY_PATTERN.formatted(sessionId, studentId) + ":timestamp";
    }

    private String driftKey(UUID sessionId, UUID studentId) {
        return DRIFT_KEY_PATTERN.formatted(sessionId, studentId);
    }

    private String hallPassKey(UUID sessionId, UUID studentId) {
        return "hallpass:" + sessionId + ":" + studentId;
    }

    // 🔐 ENHANCED MULTI-LAYER LOCATION VERIFICATION
    public boolean verifyLocationEnhanced(UUID sessionId, UUID studentId, double latitude, double longitude, 
                                          String deviceFingerprint, String wifiNetworks, String ipAddress) {
        
        try {
            // 🔍 LAYER 1: BASIC GPS VERIFICATION
            Point reportedLocation = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
            reportedLocation.setSRID(SRID_WGS84);
            
            ClassroomSession session = classroomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            Polygon roomBoundary = session.getGeofencePolygon();
            if (roomBoundary == null || !roomBoundary.contains(reportedLocation)) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("LOCATION_VIOLATION", studentId.toString(), 
                    String.format("GPS outside boundary: session=%s, lat=%s, lng=%s", sessionId, latitude, longitude));
                return false;
            }
            
            // 🔍 LAYER 2: DEVICE FINGERPRINT VERIFICATION
            if (!verifyDeviceFingerprint(studentId, deviceFingerprint)) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("DEVICE_MISMATCH", studentId.toString(), 
                    String.format("Device fingerprint mismatch: session=%s", sessionId));
                return false;
            }
            
            // 🔍 LAYER 3: WIFI NETWORK VERIFICATION (Optional/Soft Layer)
            if (wifiNetworks != null && !wifiNetworks.isEmpty()) {
                if (!verifyWiFiNetworks(studentId, wifiNetworks, session.getRoom().getId().toString())) {
                    ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("WIFI_MISMATCH", studentId.toString(), 
                        String.format("WiFi networks mismatch: session=%s", sessionId));
                    return false;
                }
            } else {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("WIFI_SKIPPED", studentId.toString(), 
                    String.format("WiFi unavailable, relying on GPS only: session=%s", sessionId));
            }
            
            // 🔍 LAYER 4: IP LOCATION VERIFICATION (Optional/Soft Layer)
            if (ipAddress != null && !ipAddress.isEmpty()) {
                if (!verifyIPLocation(studentId, ipAddress, session.getRoom().getId().toString())) {
                    ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("IP_MISMATCH", studentId.toString(), 
                        String.format("IP location mismatch: session=%s", sessionId));
                    return false;
                }
            } else {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("IP_SKIPPED", studentId.toString(), 
                    String.format("Mobile data detected, IP range check skipped: session=%s", sessionId));
            }
            
            // 🔍 LAYER 5: BEHAVIORAL PATTERN VERIFICATION
            if (!verifyBehavioralPattern(studentId, sessionId, latitude, longitude)) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("BEHAVIORAL_ANOMALY", studentId.toString(), 
                    String.format("Behavioral anomaly detected: session=%s", sessionId));
                return false;
            }
            
            // 🔍 LAYER 6: TIME-BASED VERIFICATION
            if (!verifyTimeBasedAccess(studentId, sessionId)) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("TIME_VIOLATION", studentId.toString(), 
                    String.format("Time-based access violation: session=%s", sessionId));
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("LOCATION_VERIFICATION_ERROR", studentId.toString(), 
                String.format("Location verification error: session=%s, error=%s", sessionId, e.getMessage()));
            return false;
        }
    }
    
    // 🔐 ENFORCED HARDWARE SIGNATURE LOCK
    boolean verifyHardwareSignature(User student, String incomingSignature) {
        if (incomingSignature == null || incomingSignature.isEmpty()) return false;
        
        // Silent Capture: If device isn't locked, lock it to the first signature seen
        if (student.getDeviceId() == null) {
            logger.info("📱 AI SECURITY [LOCKING]: Silently anchoring device ID for student: {}", student.getEmail());
            student.setDeviceId(incomingSignature);
            student.setDeviceRegisteredAt(Instant.now());
            userRepository.save(student);
            return true;
        }
        
        return student.getDeviceId().equals(incomingSignature);
    }

    // 🔐 ENFORCED BIOMETRIC SIGNATURE LOCK
    boolean verifyBiometricSignature(User student, String incomingBiometric) {
        if (incomingBiometric == null || incomingBiometric.isEmpty()) return false;
        
        // If biometric isn't set, allow the first one to set it
        if (student.getBiometricSignature() == null) {
            student.setBiometricSignature(incomingBiometric);
            userRepository.save(student);
            return true;
        }
        
        return student.getBiometricSignature().equals(incomingBiometric);
    }

    // 🔋 AI BATTERY ADAPTIVE POLLING (60s Stationary / 10s Moving)
    public long calculateOptimalInterval(EnhancedHeartbeatPing ping) {
        // AI Logic: If student is stationary and screen is off, set long interval
        if (Boolean.FALSE.equals(ping.isDeviceMoving()) && (ping.isScreenOn() == null || !ping.isScreenOn())) {
            return 120; // 2 minutes (Stationary/Still)
        }
        return 15; // 15 seconds (In-room movement)
    }

    boolean verifyDeviceFingerprint(UUID studentId, String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            return false;
        }
        
        String key = "device_fingerprint:" + studentId;
        try {
            String storedFingerprint = redisTemplate.opsForValue().get(key);
            if (storedFingerprint == null) {
                // 🔐 FIRST TIME DEVICE - REGISTER
                redisTemplate.opsForValue().set(key, deviceFingerprint, java.time.Duration.ofDays(30));
                return true;
            }
            return storedFingerprint.equals(deviceFingerprint);
        } catch (Exception e) {
            return false;
        }
    }
    
    boolean verifyWiFiNetworks(UUID studentId, String wifiNetworks, String roomId) {
        if (wifiNetworks == null || wifiNetworks.isEmpty()) {
            return true; // Soft pass - calling method handles logging
        }
        
        // 🔐 VERIFY STUDENT IS CONNECTED TO CAMPUS WIFI
        String[] networks = wifiNetworks.split(",");
        boolean hasCampusWiFi = false;
        
        for (String network : networks) {
            if (network.contains("TechUniversity") || network.contains("University")) {
                hasCampusWiFi = true;
                break;
            }
        }
        
        return hasCampusWiFi;
    }
    
    boolean verifyIPLocation(UUID studentId, String ipAddress, String roomId) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true; // Soft pass - calling method handles logging
        }
        
        // 🔐 VERIFY IP IS WITHIN CAMPUS RANGE
        // This would integrate with your campus network infrastructure
        String campusIPRange = "192.168.1."; // Example campus IP range
        
        return ipAddress.startsWith(campusIPRange);
    }
    
    protected boolean verifyBehavioralPattern(UUID studentId, UUID sessionId, double latitude, double longitude) {
        // 🔐 ANALYZE HISTORICAL MOVEMENT PATTERNS
        String patternKey = "movement_pattern:" + studentId;
        
        try {
            // Store current location for pattern analysis
            String locationData = String.format("%s,%s,%s", sessionId, latitude, longitude);
            redisTemplate.opsForList().rightPush(patternKey, locationData);
            redisTemplate.expire(patternKey, java.time.Duration.ofDays(7));
            
            // Analyze pattern consistency
            List<String> recentLocations = redisTemplate.opsForList().range(patternKey, 0, 9);
            
            if (recentLocations.size() > 5) {
                // 🔐 CHECK FOR UNUSAL MOVEMENT PATTERNS
                // This would integrate with AI pattern analysis
                return true; // Simplified for now
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 🕐 GET WALK-OUT THRESHOLD BASED ON TIMETABLE BREAK TIMES
     */
    private int getWalkOutThresholdSeconds(UUID sessionId) {
        try {
            ClassroomSession session = classroomSessionRepository.findById(sessionId).orElse(null);
            if (session == null || session.getTimetable() == null) {
                // Fallback to 5 minutes (300 seconds)
                return 300;
            }
            
            Timetable timetable = session.getTimetable();
            LocalTime currentTime = LocalTime.now();
            int thresholdMinutes = timetable.getWalkOutThresholdForTime(currentTime);
            
            // Override with strict 5-minute rule if timetable doesn't define specific behavior
            return Math.max(300, thresholdMinutes * 60); 
            
        } catch (Exception e) {
            // Fallback to 5 minutes on error
            return 300;
        }
    }
    
    /**
     * 🕐 CHECK IF STUDENT HAS AUTOMATIC BREAK PASS
     */
    private boolean hasAutomaticBreakPass(UUID sessionId, UUID studentId) {
        try {
            ClassroomSession session = classroomSessionRepository.findById(sessionId).orElse(null);
            if (session == null || session.getTimetable() == null) {
                return false;
            }
            
            Timetable timetable = session.getTimetable();
            LocalTime currentTime = LocalTime.now();
            
            // Automatic break pass during scheduled breaks
            return timetable.isDuringAnyBreak(currentTime);
            
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean verifyTimeBasedAccess(UUID studentId, UUID sessionId) {
        // 🔐 VERIFY STUDENT IS WITHIN ALLOWED TIME WINDOW
        ClassroomSession session = classroomSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Instant sessionStart = session.getStartTime();
        Instant sessionEnd = session.getEndTime();
        
        // Allow 15-minute buffer before and after session
        LocalDateTime allowedStart = LocalDateTime.ofInstant(sessionStart.minusSeconds(900), ZoneId.systemDefault());
        LocalDateTime allowedEnd = LocalDateTime.ofInstant(sessionEnd.plusSeconds(900), ZoneId.systemDefault());
        
        return now.isAfter(allowedStart) && now.isBefore(allowedEnd);
    }

    /**
     * 🛡️ UNIFIED SECURITY ALERT LOGGING
     * Prevents duplicate alerts for the same student within a short window.
     */
    @Transactional
    public void logSecurityAlert(User user, String type, String message, String severity, Double confidence) {
        if (user == null) return;

        // 🕵️ DE-DUPLICATION: Check for similar alerts in the last 60 seconds
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        boolean exists = securityAlertRepository.findByUser_IdAndResolvedFalse(user.getId()).stream()
                .anyMatch(a -> a.getCreatedAt().isAfter(oneMinuteAgo) && 
                              (a.getAlertType().equals(type) || 
                               (type.contains("SPOOF") && a.getAlertType().contains("DEVICE")) ||
                               (type.contains("DEVICE") && a.getAlertType().contains("SPOOF"))));

        if (exists) {
            logger.debug("🛡️ AI SECURITY: Suppressed duplicate alert ({}) for student: {}", type, user.getEmail());
            return;
        }

        SecurityAlert alert = new SecurityAlert();
        alert.setUser(user);
        alert.setAlertType(type);
        alert.setAlertMessage(message);
        alert.setSeverity(severity);
        alert.setConfidence(confidence != null ? confidence : 0.95);
        alert.setResolved(false);
        securityAlertRepository.save(alert);
        
        logger.info("🛡️ AI SECURITY ALERT LOGGED: {} for {}", type, user.getEmail());
    }
}

