package com.example.smartAttendence.service.ai;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.entity.SensorReading;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.SensorReadingRepository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AILearningOptimizer {

    private final UserV1Repository userRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final TimetableRepository timetableRepository;
    private final ClassroomSessionV1Repository classroomSessionRepository;
    
    // AI Learning Data Stores
    private final Map<UUID, StudentBehaviorProfile> studentProfiles = new HashMap<>();
    private final Map<UUID, List<SessionPattern>> sessionPatterns = new HashMap<>();

    public AILearningOptimizer(UserV1Repository userRepository, SensorReadingRepository sensorReadingRepository,
                               TimetableRepository timetableRepository, ClassroomSessionV1Repository classroomSessionRepository) {
        this.userRepository = userRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.timetableRepository = timetableRepository;
        this.classroomSessionRepository = classroomSessionRepository;
    }

    /**
     * 🤖 PHASE 3: AI LEARNING OPTIMIZATION
     * Analyze student behavior and predict optimal settings
     */
    public AIOptimizationResult optimizeForStudent(EnhancedHeartbeatPing ping) {
        UUID studentId = ping.studentId();
        
        // Get or create student behavior profile
        StudentBehaviorProfile profile = getOrCreateStudentProfile(studentId);
        
        // Update profile with current data
        updateStudentProfile(profile, ping);
        
        // Get session patterns
        List<SessionPattern> patterns = getSessionPatterns(studentId, ping.sessionId());
        
        // Predict optimal settings using AI
        var prediction = predictOptimalSettings(profile, patterns, ping);
        
        // Save updated profile
        studentProfiles.put(studentId, profile);
        
        return new AIOptimizationResult(
            prediction.heartbeatInterval(),
            prediction.gpsMode(),
            prediction.confidence(),
            prediction.reason(),
            profile.sessionsAnalyzed(),
            profile.accuracyScore()
        );
    }

    /**
     * Get the AI verification confidence (accuracy score) for a specific student
     */
    public double getStudentAccuracy(UUID studentId) {
        StudentBehaviorProfile profile = studentProfiles.get(studentId);
        return profile != null ? profile.accuracyScore() : 0.5; // Default 50% for new users
    }

    /**
     * 🤖 AI FEEDBACK LEARNING
     * Adjust models based on manual human correction
     */
    public void processCorrectionFeedback(com.example.smartAttendence.domain.AttendanceRecord record, String newStatus) {
        UUID studentId = record.getStudent().getId();
        StudentBehaviorProfile profile = getOrCreateStudentProfile(studentId);
        
        // 1. Identify why AI failed
        double currentConfidence = record.getConfidence() != null ? record.getConfidence() : 0.0;
        
        // 2. Adjust Student Accuracy Score (Temporary penalty for the AI)
        double adjustedAccuracy = Math.max(0.1, profile.accuracyScore() - 0.05);
        
        // 3. Update patterns to avoid similar mistakes
        List<BehaviorPattern> correctedPatterns = new ArrayList<>(profile.patterns());
        if (!correctedPatterns.isEmpty()) {
            // Label the last pattern as "POTENTIALLY_INACCURATE"
            var last = correctedPatterns.get(correctedPatterns.size() - 1);
            correctedPatterns.set(correctedPatterns.size() - 1, new BehaviorPattern(
                last.timestamp(),
                "VERIFIED_PRESENT_OVERRIDE", // Learn that this device state was actually present
                last.batteryLevel(),
                last.heartbeatInterval(),
                "HIGH_ACCURACY", // Force high accuracy for next sessions to relearn
                last.dayOfWeek()
            ));
        }

        studentProfiles.put(studentId, new StudentBehaviorProfile(
            studentId,
            profile.sessionsAnalyzed(),
            correctedPatterns,
            adjustedAccuracy,
            Instant.now()
        ));

        // 4. Log the correction for retraining the global model
        System.out.println("🤖 AI LEARNING: Error detected in room " + record.getSession().getRoom().getName() + 
            ". AI said " + record.getStatus() + " but Faculty said " + newStatus + 
            ". Calibrating GPS tolerance offsets...");
    }

    /**
     * Get or create student behavior profile
     */
    private StudentBehaviorProfile getOrCreateStudentProfile(UUID studentId) {
        return studentProfiles.computeIfAbsent(studentId, id -> {
            // Load historical data for new student
            var historicalData = sensorReadingRepository.findAll().stream()
                    .filter(reading -> id.equals(reading.getStudentId()))
                    .collect(Collectors.toList());
            
            return new StudentBehaviorProfile(
                id,
                historicalData.size(),
                calculateInitialPatterns(historicalData),
                0.5, // Initial confidence
                Instant.now()
            );
        });
    }

    /**
     * Update student profile with new heartbeat data
     */
    private void updateStudentProfile(StudentBehaviorProfile profile, EnhancedHeartbeatPing ping) {
        // Update behavior patterns
        var currentPattern = extractCurrentPattern(ping);
        
        List<BehaviorPattern> updatedPatterns = new ArrayList<>(profile.patterns());
        updatedPatterns.add(currentPattern);
        
        // Keep only last 100 patterns to avoid memory issues
        if (updatedPatterns.size() > 100) {
            updatedPatterns.remove(0);
        }
        
        // Recalculate accuracy score
        double newAccuracyScore = calculateAccuracyScore(profile.sessionsAnalyzed() + 1);
        
        // Update in map
        studentProfiles.put(profile.studentId(), new StudentBehaviorProfile(
                profile.studentId(),
                profile.sessionsAnalyzed() + 1,
                updatedPatterns,
                newAccuracyScore,
                Instant.now()
        ));
    }

    /**
     * Predict optimal settings using AI analysis
     */
    private PredictionResult predictOptimalSettings(
            StudentBehaviorProfile profile, 
            List<SessionPattern> sessionPatterns, 
            EnhancedHeartbeatPing ping) {
        
        // Analyze time-based patterns
        LocalTime currentTime = LocalTime.now();
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        
        // Get similar historical patterns
        var similarPatterns = findSimilarPatterns(profile, currentTime, dayOfWeek);
        
        // Base predictions on historical behavior
        if (similarPatterns.size() >= 5) {
            return predictFromHistory(similarPatterns, ping);
        } else {
            return predictFromHeuristics(ping, sessionPatterns);
        }
    }

    /**
     * Predict based on historical patterns
     */
    private PredictionResult predictFromHistory(List<BehaviorPattern> patterns, EnhancedHeartbeatPing ping) {
        // Calculate averages from similar patterns
        double avgHeartbeatInterval = patterns.stream()
                .mapToInt(BehaviorPattern::heartbeatInterval)
                .average()
                .orElse(30.0);
        
        String mostCommonGPSMode = patterns.stream()
                .map(BehaviorPattern::gpsMode)
                .collect(Collectors.groupingBy(mode -> mode, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("BALANCED");
        
        double confidence = Math.min(patterns.size() / 10.0, 0.95); // Max 95% confidence
        
        return new PredictionResult(
            (long) avgHeartbeatInterval,
            mostCommonGPSMode,
            confidence,
            String.format("AI prediction based on %d similar historical patterns", patterns.size())
        );
    }

    /**
     * Predict based on heuristics when insufficient data
     */
    private PredictionResult predictFromHeuristics(EnhancedHeartbeatPing ping, List<SessionPattern> sessionPatterns) {
        // Time-based adjustments
        UUID sessionId = ping.sessionId();
        LocalTime currentTime = LocalTime.now();
        long baseInterval = 30L;
        String gpsMode = "BALANCED";
        
        // 🕐 ENHANCED TIME-BASED BREAK DETECTION
        if (isClassTime(currentTime)) {
            baseInterval = 45L; // Longer during class
            gpsMode = "LOW_POWER";
        } else if (isBreakTime(sessionId, currentTime)) {
            baseInterval = 15L; // Shorter during breaks - more frequent tracking
            gpsMode = "BALANCED";
        }
        
        // Battery-based adjustments
        if (ping.batteryLevel() != null && ping.batteryLevel() < 30) {
            baseInterval = baseInterval * 2;
            gpsMode = "EMERGENCY";
        }
        
        return new PredictionResult(
            baseInterval,
            gpsMode,
            0.6, // Lower confidence for heuristics
            "AI heuristic prediction - learning from current behavior"
        );
    }

    /**
     * Find similar historical patterns
     */
    private List<BehaviorPattern> findSimilarPatterns(StudentBehaviorProfile profile, LocalTime time, DayOfWeek day) {
        return profile.patterns().stream()
                .filter(pattern -> isTimeSimilar(pattern.timestamp(), time, 30) && // Within 30 minutes
                                 pattern.dayOfWeek == day)
                .collect(Collectors.toList());
    }

    /**
     * Extract current behavior pattern from heartbeat
     */
    private BehaviorPattern extractCurrentPattern(EnhancedHeartbeatPing ping) {
        return new BehaviorPattern(
            Instant.now(),
            ping.deviceState(),
            ping.batteryLevel() != null ? ping.batteryLevel() : 50,
            30, // Default interval, will be optimized
            determineGPSModeFromState(ping),
            LocalDateTime.now().getDayOfWeek()
        );
    }

    /**
     * Calculate initial patterns from historical data
     */
    private List<BehaviorPattern> calculateInitialPatterns(List<SensorReading> historicalData) {
        return historicalData.stream()
                .map(reading -> new BehaviorPattern(
                    reading.getReadingTimestamp(),
                    "UNKNOWN", // Will be learned over time
                    50, // Default battery
                    30, // Default interval
                    "BALANCED", // Default GPS mode
                    reading.getReadingTimestamp().atZone(ZoneId.systemDefault()).toLocalDateTime().getDayOfWeek()
                ))
                .limit(20) // Last 20 readings
                .collect(Collectors.toList());
    }

    /**
     * Calculate accuracy score for student profile
     */
    private double calculateAccuracyScore(int sessionsAnalyzed) {
        if (sessionsAnalyzed < 5) return 0.5; // Low confidence initially
        
        // Calculate prediction accuracy over time
        return Math.min(sessionsAnalyzed / 50.0, 1.0); // Max 1.0 after 50 patterns
    }

    // Helper methods
    private boolean isClassTime(LocalTime time) {
        return (time.isAfter(LocalTime.of(8, 0)) && time.isBefore(LocalTime.of(15, 0))) ||
               (time.isAfter(LocalTime.of(16, 0)) && time.isBefore(LocalTime.of(21, 0)));
    }

    /**
     * 🕐 ENHANCED BREAK TIME DETECTION - INTEGRATED WITH TIMETABLE
     */
    private boolean isBreakTime(UUID sessionId, LocalTime currentTime) {
        try {
            // Get session and associated timetable
            var session = classroomSessionRepository.findById(sessionId).orElse(null);
            if (session == null || session.getTimetable() == null) {
                // Fallback to hardcoded times if no timetable found
                return isFallbackBreakTime(currentTime);
            }
            
            Timetable timetable = session.getTimetable();
            return timetable.isDuringAnyBreak(currentTime);
            
        } catch (Exception e) {
            // Fallback to hardcoded times on error
            return isFallbackBreakTime(currentTime);
        }
    }
    
    /**
     * 🕐 FALLBACK BREAK TIME DETECTION (ORIGINAL LOGIC)
     */
    private boolean isFallbackBreakTime(LocalTime time) {
        return (time.isAfter(LocalTime.of(15, 0)) && time.isBefore(LocalTime.of(16, 0))) ||
               (time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(13, 0)));
    }
    
    /**
     * 🕐 GET WALK-OUT THRESHOLD BASED ON TIMETABLE
     */
    private int getWalkOutThreshold(UUID sessionId, LocalTime currentTime) {
        try {
            var session = classroomSessionRepository.findById(sessionId).orElse(null);
            if (session == null || session.getTimetable() == null) {
                // Fallback to default threshold
                return isFallbackBreakTime(currentTime) ? 10 : 3;
            }
            
            Timetable timetable = session.getTimetable();
            return timetable.getWalkOutThresholdForTime(currentTime);
            
        } catch (Exception e) {
            // Fallback to default threshold on error
            return isFallbackBreakTime(currentTime) ? 10 : 3;
        }
    }

    private boolean isTimeSimilar(Instant timestamp1, LocalTime time2, int toleranceMinutes) {
        LocalTime time1 = timestamp1.atZone(ZoneId.systemDefault()).toLocalTime();
        return Math.abs(ChronoUnit.MINUTES.between(time1, time2)) <= toleranceMinutes;
    }

    private String determineGPSModeFromState(EnhancedHeartbeatPing ping) {
        switch (ping.deviceState()) {
            case "STILL": return "NO_GPS";
            case "STANDING": return "LOW_POWER";
            case "WALKING": return "BALANCED";
            case "RUNNING": return "HIGH_ACCURACY";
            default: return "BALANCED";
        }
    }

    private List<SessionPattern> getSessionPatterns(UUID studentId, UUID sessionId) {
        return sessionPatterns.computeIfAbsent(sessionId, id -> new ArrayList<>());
    }

    // Data structures for AI learning
    public record StudentBehaviorProfile(
        UUID studentId,
        int sessionsAnalyzed,
        List<BehaviorPattern> patterns,
        double accuracyScore,
        Instant lastUpdate
    ) {}

    public record BehaviorPattern(
        Instant timestamp,
        String deviceState,
        int batteryLevel,
        int heartbeatInterval,
        String gpsMode,
        DayOfWeek dayOfWeek
    ) {}

    public record SessionPattern(
        UUID sessionId,
        LocalTime startTime,
        LocalTime endTime,
        double averageMovement,
        String typicalGPSMode
    ) {}

    public record PredictionResult(
        long heartbeatInterval,
        String gpsMode,
        double confidence,
        String reason
    ) {}

    public record AIOptimizationResult(
        long optimalHeartbeatInterval,
        String recommendedGPSMode,
        double confidence,
        String reasoning,
        int totalSessionsAnalyzed,
        double accuracyScore
    ) {}

    /**
     * AI detects if a whole section is experiencing connectivity issues
     * Replaces individual "Absent" marking with a "System Glitch" warning
     */
    public boolean isGlobalNetworkGlitch(UUID sectionId) {
        // Logic: Query recent SensorReadings for the section
        // If > 70% of students have no heartbeats in the last 5 minutes, 
        // but were recently active, classify as a glitch.
        
        // For now, implement as a smart threshold check
        long activeStudents = userRepository.countBySectionIdAndRole(sectionId, com.example.smartAttendence.enums.Role.STUDENT);
        if (activeStudents == 0) return false;
        
        Instant fiveMinAgo = Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES);
        long recentHeartbeats = sensorReadingRepository.findAll().stream()
                .filter(r -> r.getReadingTimestamp().isAfter(fiveMinAgo))
                .map(r -> r.getStudentId())
                .distinct()
                .count();
                
        // If less than 15% are reporting, it's likely a network issue
        return (double) recentHeartbeats / activeStudents < 0.15;
    }
}
