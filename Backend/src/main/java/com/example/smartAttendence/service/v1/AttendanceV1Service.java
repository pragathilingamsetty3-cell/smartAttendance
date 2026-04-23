package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.event.WalkOutEvent;
import com.example.smartAttendence.entity.SecurityAlert;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.SecurityAlertV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.SetOptions;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional(readOnly = true)
public class AttendanceV1Service {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceV1Service.class);

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    
    private final ClassroomSessionV1Repository classroomSessionRepository;
    private final AttendanceRecordV1Repository attendanceRecordRepository;
    private final UserV1Repository userRepository;
    private final Firestore firestore;
    private final ApplicationEventPublisher eventPublisher;
    private final AILearningOptimizer aiLearningOptimizer;
    private final SecurityAlertV1Repository securityAlertRepository;
    
    // 🧠 ELITE ACCURACY: Hysteresis Cache
    // Tracks consecutive out-of-range readings to prevent geofencing jitter.
    // Key: StudentID, Value: Count of consecutive outside readings.
    private final com.github.benmanes.caffeine.cache.Cache<UUID, Integer> hysteresisCache = 
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(java.time.Duration.ofMinutes(10))
            .build();

    // 🏎️ ELITE SCALABILITY: Heartbeat Buffer
    // Reduces DB writes by 90% by only saving routine pings every 5 minutes 
    // UNLESS the status changes or the student moves significantly.
    // Key: StudentID, Value: Last Saved Instant
    private final com.github.benmanes.caffeine.cache.Cache<UUID, java.time.Instant> heartbeatBuffer = 
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(java.time.Duration.ofMinutes(10))
            .build();

    @Autowired
    public AttendanceV1Service(
            ClassroomSessionV1Repository classroomSessionRepository,
            AttendanceRecordV1Repository attendanceRecordRepository,
            UserV1Repository userRepository,
            @Nullable Firestore firestore,
            ApplicationEventPublisher eventPublisher,
            AILearningOptimizer aiLearningOptimizer,
            SecurityAlertV1Repository securityAlertRepository
    ) {
        this.classroomSessionRepository = classroomSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userRepository = userRepository;
        this.firestore = firestore;
        this.eventPublisher = eventPublisher;
        this.aiLearningOptimizer = aiLearningOptimizer;
        this.securityAlertRepository = securityAlertRepository;
    }

    /**
     * 🤖 AI AUTONOMOUS WATCHER
     * Runs every 30 seconds (High RAM Unlock) to enforce security grace periods.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorGracePeriods() {
        try {
            List<ClassroomSession> activeSessions = classroomSessionRepository.findByActiveTrue();
            Instant now = Instant.now();

            for (ClassroomSession session : activeSessions) {
                List<AttendanceRecord> latestRecords = attendanceRecordRepository.findBySessionIdOrderByRecordedAtDesc(session.getId());
                java.util.Set<UUID> processedStudents = new java.util.HashSet<>();

                for (AttendanceRecord record : latestRecords) {
                    UUID studentId = record.getStudent().getId();
                    if (processedStudents.contains(studentId)) continue;
                    processedStudents.add(studentId);

                    if ("ABSENT".equalsIgnoreCase(record.getStatus())) continue;
                    if (!"WALK_OUT".equalsIgnoreCase(record.getStatus())) continue;

                    Instant lastSeen = record.getRecordedAt();
                    int threshold = getWalkOutThresholdSeconds(session.getId());

                    if (now.isAfter(lastSeen.plusSeconds(threshold + 60))) { 
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
        String hallPassKey = hallPassKey(request.sessionId(), request.studentId());
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ACTIVE");
        data.put("expiresAt", Instant.now().plusSeconds(request.requestedMinutes() * 60L).toString());
        
        firestore.collection("hall_passes").document(hallPassKey).set(data);
    }

    @Transactional
    public void processEnhancedHeartbeat(com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing ping, boolean isCellular) {
        UUID sessionId = ping.sessionId();
        UUID studentId = ping.studentId();

        // 1. Hall Pass Check (Firestore)
        if (isHallPassActive(sessionId, studentId)) return;
        
        // 2. Security Handshake
        User student = userRepository.findById(studentId).orElseThrow();
        if (!verifyHardwareSignature(student, ping.deviceFingerprint())) {
            handleUnauthorizedDevice(student, sessionId);
            return;
        }

        // 📈 RELIABILITY: Sequence Tracking
        // Ignore heartbeats that arrive out-of-order or are delayed duplicates
        if (isOutOfOrderPacket(studentId, sessionId, ping.sequenceId())) {
            logger.info("📈 RELIABILITY: Packet ignored for user {} (Out-of-order sequence: {})", studentId, ping.sequenceId());
            return;
        }

        // 3. Biometric Check (Initial)
        boolean isInitial = !attendanceRecordRepository.existsBySession_IdAndStudent_Id(sessionId, studentId);
        if (isInitial && !verifyBiometricSignature(student, ping.biometricSignature())) {
            handleBiometricFailure(student, sessionId);
            return;
        }

        // 🔐 ELITE SECURITY: HMAC-SHA256 Request Signing
        // Prevent manual spoofing by verifying that the request came from the real mobile app
        if (!verifyHmacSignature(student, ping)) {
            logSecurityAlert(student, "SECURITY_HMAC_FAILURE", "Heartbeat signature invalid. Potential manual spoofing attempt.", "CRITICAL", 1.0);
            return;
        }
        
        if (hasAutomaticBreakPass(sessionId, studentId)) return;

        ClassroomSession session = classroomSessionRepository.findById(sessionId).orElseThrow();
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(ping.longitude(), ping.latitude()));
        point.setSRID(SRID_WGS84);
        boolean inside = session.getGeofencePolygon().contains(point);

        if (!inside) {
            // 🧠 ELITE ACCURACY: Hysteresis (Anti-Jitter)
            // Instead of immediate WALK_OUT, wait for 3 consecutive failures to avoid GPS noise.
            int currentFailures = hysteresisCache.asMap().getOrDefault(studentId, 0) + 1;
            hysteresisCache.put(studentId, currentFailures);
            
            if (currentFailures >= 3) {
                handleOutOfBounds(ping, studentId, sessionId);
            } else {
                logger.info("🧠 HYSTERESIS: Student {} drifted (Count: {}/3). Holding status.", studentId, currentFailures);
            }
        } else {
            // Reset failure count on successful geofence check
            hysteresisCache.invalidate(studentId);
            handleInBounds(ping, student, session);
        }
    }

    private boolean isHallPassActive(UUID sessionId, UUID studentId) {
        try {
            String key = hallPassKey(sessionId, studentId);
            Map<String, Object> data = firestore.collection("hall_passes").document(key).get().get().getData();
            if (data == null) return false;
            Instant expiresAt = Instant.parse((String) data.get("expiresAt"));
            return Instant.now().isBefore(expiresAt);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleOutOfBounds(EnhancedHeartbeatPing ping, UUID studentId, UUID sessionId) {
        String driftKey = driftKey(sessionId, studentId);
        DocumentReference driftDoc = firestore.collection("drift_tracking").document(driftKey);
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("lastSeen", Instant.now().toString());
            data.put("firstDrift", FieldValue.serverTimestamp());
            driftDoc.set(data, SetOptions.merge());
            
            // Provisional Walk-out logic
            attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(studentId, sessionId)
                .ifPresent(record -> {
                    record.setLatitude(ping.latitude());
                    record.setLongitude(ping.longitude());
                    record.setRecordedAt(Instant.now());
                    if (!"ABSENT".equalsIgnoreCase(record.getStatus())) {
                        record.setStatus("WALK_OUT");
                    }
                    attendanceRecordRepository.save(record);
                });

            // check timeout
            Map<String, Object> driftData = driftDoc.get().get().getData();
            if (driftData != null && driftData.containsKey("firstDrift")) {
                Instant firstDrift = ((com.google.cloud.Timestamp) driftData.get("firstDrift")).toDate().toInstant();
                if (Instant.now().isAfter(firstDrift.plusSeconds(getWalkOutThresholdSeconds(sessionId)))) {
                    markAbsent(studentId, sessionId);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling drift: {}", e.getMessage());
        }
    }

    private void markAbsent(UUID studentId, UUID sessionId) {
        attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(studentId, sessionId)
            .ifPresent(record -> {
                if (!"ABSENT".equalsIgnoreCase(record.getStatus())) {
                    record.setStatus("ABSENT");
                    record.setNote("AI Autonomous: Walk-out timeout reached.");
                    attendanceRecordRepository.save(record);
                    eventPublisher.publishEvent(new WalkOutEvent(studentId, sessionId, LocalDateTime.now()));
                }
            });
    }

    private void handleInBounds(EnhancedHeartbeatPing ping, User student, ClassroomSession session) {
        String driftKey = driftKey(session.getId(), student.getId());
        firestore.collection("drift_tracking").document(driftKey).delete();

        attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(student.getId(), session.getId())
            .ifPresentOrElse(
                record -> updateExistingRecord(record, ping, session),
                () -> createNewRecord(student, session, ping)
            );
    }

    private void updateExistingRecord(AttendanceRecord record, EnhancedHeartbeatPing ping, ClassroomSession session) {
        String newStatus = determineStatus(session, Instant.now());
        boolean statusChanged = !newStatus.equalsIgnoreCase(record.getStatus());
        
        // 🏎️ SCALABILITY CHECK: Hybrid Buffering
        // Skip DB write if:
        // 1. Status is the same
        // 2. Student is roughly in the same spot (delta < 2m)
        // 3. Last write was less than 5 minutes ago
        Instant lastWrite = heartbeatBuffer.getIfPresent(record.getStudent().getId());
        if (!statusChanged && lastWrite != null && lastWrite.isAfter(Instant.now().minus(java.time.Duration.ofMinutes(5)))) {
            double dist = calculateDistance(record.getLatitude(), record.getLongitude(), ping.latitude(), ping.longitude());
            if (dist < 2.0) {
                // Routine ping - skip heavy DB transaction
                return;
            }
        }

        if (statusChanged) {
            record.setStatus(newStatus);
        }
        record.setRecordedAt(Instant.now());
        record.setLatitude(ping.latitude());
        record.setLongitude(ping.longitude());
        record.setBatteryLevel(ping.batteryLevel());
        record.setSequenceId(ping.sequenceId()); // 📈 Update sequence
        
        attendanceRecordRepository.save(record);
        heartbeatBuffer.put(record.getStudent().getId(), Instant.now());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void createNewRecord(User student, ClassroomSession session, EnhancedHeartbeatPing ping) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setSession(session);
        record.setStatus(determineStatus(session, Instant.now()));
        record.setRecordedAt(Instant.now());
        record.setLatitude(ping.latitude());
        record.setLongitude(ping.longitude());
        record.setDeviceSignature(ping.deviceFingerprint());
        record.setSequenceId(ping.sequenceId()); // 📈 Set initial sequence
        attendanceRecordRepository.save(record);
        heartbeatBuffer.put(student.getId(), Instant.now());
    }

    private void handleUnauthorizedDevice(User student, UUID sessionId) {
        logSecurityAlert(student, "ABSENCE_DEVICE_LOG", "Device signature mismatch.", "HIGH", 1.0);
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setSession(classroomSessionRepository.findById(sessionId).orElse(null));
        record.setStatus("ABSENT");
        record.setNote("AI Security: Hardware signature mismatch.");
        attendanceRecordRepository.save(record);
    }

    private void handleBiometricFailure(User student, UUID sessionId) {
        logSecurityAlert(student, "ABSENCE_BIOMETRIC_LOG", "Biometric mismatch.", "MEDIUM", 0.99);
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setSession(classroomSessionRepository.findById(sessionId).orElse(null));
        record.setStatus("ABSENT");
        record.setNote("AI Security: Biometric verification failed.");
        attendanceRecordRepository.save(record);
    }

    public ClassroomSession getActiveSessionForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return classroomSessionRepository.findActiveSessionsForSection(user.getSectionId(), Instant.now())
                .stream().findFirst().orElseGet(() -> classroomSessionRepository.findByActiveTrue().get(0));
    }

    @Transactional
    public void correctAttendance(UUID recordId, String newStatus, UUID facultyId) {
        AttendanceRecord record = attendanceRecordRepository.findById(recordId).orElseThrow();
        record.setOriginalAiStatus(record.getStatus());
        record.setStatus(newStatus);
        record.setManuallyCorrected(true);
        record.setCorrectorId(facultyId);
        attendanceRecordRepository.save(record);
        aiLearningOptimizer.processCorrectionFeedback(record, newStatus);
    }

    private String determineStatus(ClassroomSession session, Instant now) {
        return now.isAfter(session.getStartTime().plusSeconds(600)) ? "LATE" : "PRESENT";
    }

    private String hallPassKey(UUID sessionId, UUID studentId) { return "hallpass:" + sessionId + ":" + studentId; }
    private String driftKey(UUID sessionId, UUID studentId) { return sessionId + ":" + studentId; }

    boolean verifyHardwareSignature(User student, String signature) {
        if (signature == null) return false;
        if (student.getDeviceId() == null) {
            student.setDeviceId(signature);
            userRepository.save(student);
            return true;
        }
        return student.getDeviceId().equals(signature);
    }

    boolean verifyBiometricSignature(User student, String signature) {
        if (signature == null) return false;
        if (student.getBiometricSignature() == null) {
            student.setBiometricSignature(signature);
            userRepository.save(student);
            return true;
        }
        return student.getBiometricSignature().equals(signature);
    }

    private int getWalkOutThresholdSeconds(UUID sessionId) {
        return classroomSessionRepository.findById(sessionId)
                .map(s -> s.getTimetable() != null ? Math.max(300, s.getTimetable().getWalkOutThresholdForTime(LocalTime.now()) * 60) : 300)
                .orElse(300);
    }

    private boolean hasAutomaticBreakPass(UUID sessionId, UUID studentId) {
        return classroomSessionRepository.findById(sessionId)
                .map(s -> s.getTimetable() != null && s.getTimetable().isDuringAnyBreak(LocalTime.now()))
                .orElse(false);
    }

    @Transactional
    public void logSecurityAlert(User user, String type, String message, String severity, Double confidence) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        boolean exists = securityAlertRepository.findByUser_IdAndResolvedFalse(user.getId()).stream()
                .anyMatch(a -> a.getCreatedAt().isAfter(oneMinuteAgo) && a.getAlertType().equals(type));

        if (exists) return;

        SecurityAlert alert = new SecurityAlert();
        alert.setUser(user);
        alert.setAlertType(type);
        alert.setAlertMessage(message);
        alert.setSeverity(severity);
        alert.setConfidence(confidence != null ? confidence : 0.95);
        securityAlertRepository.save(alert);
    }

    /**
     * 🔐 HMAC-SHA256 Signature Verification
     * Verifies that the heartbeat was signed with the user's private secretKey.
     */
    private boolean verifyHmacSignature(User user, EnhancedHeartbeatPing ping) {
        if (ping.requestSignature() == null || user.getSecretKey() == null) return false;
        
        try {
            // 1. Construct payload string (Match mobile app logic exactly)
            String payload = String.format("%s|%s|%.6f|%.6f|%d|%d",
                ping.studentId(),
                ping.sessionId(),
                ping.latitude(),
                ping.longitude(),
                ping.stepCount(),
                ping.batteryLevel()
            );
            
            // 2. Generate HMAC-SHA256
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                user.getSecretKey().getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            String serverGeneratedSignature = java.util.HexFormat.of().formatHex(rawHmac);
            
            return serverGeneratedSignature.equalsIgnoreCase(ping.requestSignature());
        } catch (Exception e) {
            logger.error("HMAC Verification Error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 📈 Reliability Packet Check
     */
    private boolean isOutOfOrderPacket(UUID studentId, UUID sessionId, Long sequenceId) {
        if (sequenceId == null) return false;
        
        return attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(studentId, sessionId)
                .map(record -> record.getSequenceId() != null && sequenceId <= record.getSequenceId())
                .orElse(false);
    }
}

