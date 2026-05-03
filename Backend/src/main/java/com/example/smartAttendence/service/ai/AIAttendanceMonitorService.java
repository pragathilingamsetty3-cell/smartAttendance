package com.example.smartAttendence.service.ai;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.SecurityAlert;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.repository.v1.AcademicCalendarV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.SecurityAlertV1Repository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.v1.NotificationService;
import com.google.cloud.firestore.Firestore;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIAttendanceMonitorService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final TimetableRepository timetableRepository;
    private final ClassroomSessionV1Repository sessionRepository;
    private final AttendanceRecordV1Repository attendanceRepository;
    private final UserV1Repository userRepository;
    private final SecurityAlertV1Repository alertRepository;
    private final NotificationService notificationService;
    private final AcademicCalendarV1Repository calendarRepository;
    private final AISpatialMonitoringEngine spatialEngine;
    private final AILearningOptimizer learningOptimizer;
    private final com.example.smartAttendence.repository.EmergencySessionChangeRepository emergencyRepository;
    @Nullable
    private final Firestore firestore;

    /**
     * 🤖 AI AUTONOMOUS MONITORING TASK
     * Runs every 30 seconds (High RAM Unlock) to sync timetable with live presence
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorActiveSessions() {
        ZonedDateTime now = ZonedDateTime.now(IST);
        LocalDate today = now.toLocalDate();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (calendarRepository.isHoliday(today)) {
            log.info("🤖 AI MONITOR [RESTING MODE]: Today is a holiday. Skipping autonomous scan.");
            return;
        }

        log.debug("🤖 AI MONITOR: Starting autonomous scan at {} {}", day, time);

        // 1. PROCESS ACTIVE SESSIONS
        java.time.LocalDate todayDate = now.toLocalDate();
        List<Timetable> activeSlots = timetableRepository.findByDayOfWeekAndStartTimeBeforeAndEndTimeAfter(day, time, time)
                .stream()
                .filter(t -> (t.getStartDate() == null || !todayDate.isBefore(t.getStartDate())) && 
                             (t.getEndDate() == null || !todayDate.isAfter(t.getEndDate())))
                .collect(Collectors.toList());

        log.info("🤖 AI MONITOR: Scan found {} active timetable slots for current time: {}", activeSlots.size(), time);
        for (Timetable slot : activeSlots) {
            log.info("📍 AI MONITOR: Processing Slot - Subject: {}, Section: {}, Room: {}", 
                    slot.getSubject(), 
                    slot.getSection() != null ? slot.getSection().getName() : "N/A",
                    slot.getRoom() != null ? slot.getRoom().getName() : "N/A");
            
            if (Boolean.TRUE.equals(slot.getIsHoliday())) {
                if (slot.getHolidayDate() != null && !slot.getHolidayDate().equals(today)) {
                    log.debug("🤖 AI MONITOR: Session '{}' - Holiday mismatch date. Proceeding.", slot.getSubject());
                } else {
                    log.info("🤖 AI MONITOR [RESTING MODE]: Session '{}' is holiday. Skipping.", slot.getSubject());
                    continue;
                }
            }

            // 🚀 EMERGENCY OVERRIDE: Check if this slot was cancelled or modified
            if (isSessionCancelled(slot, now)) {
                log.info("🤖 AI MONITOR [CANCELLED]: Session '{}' was cancelled via Emergency Override. Skipping.", slot.getSubject());
                continue;
            }

            processActiveSlot(slot, now);
        }

        // 2. SEND REMINDERS (Consolidated from AutonomousSessionScheduler)
        sendClassReminders(now);

        // 3. AUTO-END SESSIONS (Consolidated from AutonomousSessionScheduler)
        autoEndExpiredSessions(now.toInstant());
    }

    private void processActiveSlot(Timetable slot, ZonedDateTime now) {
        Instant startOfDay = now.toLocalDate().atStartOfDay(IST).toInstant();
        Instant endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(IST).toInstant();
        
        ClassroomSession session = sessionRepository.findByTimetableIdAndDateRange(slot.getId(), startOfDay, endOfDay)
                .orElse(null);

        boolean justCreated = false;
        if (session == null) {
            session = createAutomaticSession(slot, now);
            justCreated = true;
        } else if (!session.isActive()) {
            // 🚀 SMART REACTIVATION: 
            // Only reactivate if it's before the scheduled endTime and it WASN'T explicitly ended early.
            // If now is > endTime, it's being/has been processed by AIAbsentMarkerJob (grace period check).
            Instant nowInstant = now.toInstant();
            if (nowInstant.isBefore(session.getEndTime())) {
                // If a human ended it early, we respect that. 
                // We assume it's a glitch and reactivate only if it was auto-generated and active within the last scan.
                // For now, let's just avoid re-activating sessions that are inactive during their scheduled time
                // to allow Faculty to end classes early if they finish the lecture.
                log.info("🤖 AI MONITOR: Session '{}' is inactive during its scheduled time. Respecting current state (Faculty may have ended it).", slot.getSubject());
            } else {
                Instant graceThreshold = session.getEndTime().plus(1, java.time.temporal.ChronoUnit.MINUTES);
                if (nowInstant.isBefore(graceThreshold)) {
                    session.setActive(true);
                    session = sessionRepository.save(session);
                    log.info("🤖 AI MONITOR: Reactivated session '{}' (within 1m grace period).", slot.getSubject());
                }
            }
            return; // Stop processing this slot for now
        }

        // Send "Mark Attendance" notification if not already sent for this session.
        // This covers sessions pre-created by resolveOrCreateSession() (heartbeat path)
        // which doesn't send notifications.
        if (!justCreated) {
            sendNotificationIfNotAlreadySent(session);
        }

        int gracePeriodMinutes = isFirstPeriodOfDay(slot) ? 15 : 10;
        if (timeIsAfterThreshold(slot.getStartTime(), now.toLocalDateTime(), gracePeriodMinutes)) {
            enforceAttendance(session, slot);
        }

        enforceWalkoutRules(session, slot);
        monitorStudentBehavior(session);
    }

    private ClassroomSession createAutomaticSession(Timetable slot, ZonedDateTime now) {
        ClassroomSession session = new ClassroomSession();
        session.setTimetable(slot);
        session.setRoom(slot.getRoom());
        session.setFaculty(slot.getFaculty());
        session.setSection(slot.getSection());
        session.setSubject(slot.getSubject());
        session.setAutoGenerated(true);
        session.setActive(true);
        session.setIsExamDay(Boolean.TRUE.equals(slot.getIsExamDay()));
        session.setIsHoliday(Boolean.TRUE.equals(slot.getIsHoliday()));
        
        Instant start = now.toLocalDate().atTime(slot.getStartTime()).atZone(IST).toInstant();
        Instant end = now.toLocalDate().atTime(slot.getEndTime()).atZone(IST).toInstant();
        if (end.isBefore(start)) end = now.toLocalDate().plusDays(1).atTime(slot.getEndTime()).atZone(IST).toInstant();
        
        session.setStartTime(start);
        session.setEndTime(end);
        if (slot.getRoom() != null) session.setGeofencePolygon(slot.getRoom().getBoundaryPolygon());
        
        ClassroomSession savedSession = sessionRepository.save(session);
        notifySectionOnSessionStart(savedSession);
        markNotificationSent(savedSession);
        return savedSession;
    }

    private void notifySectionOnSessionStart(ClassroomSession session) {
        UUID sectionId = session.getSection().getId();
        List<User> students = userRepository.findBySectionIdExplicit(sectionId);
        
        log.info("📢 NOTIFY: Sending 'Mark Attendance' prompts to {} students in Section: {} (ID: {})", 
                students.size(), session.getSection().getName(), sectionId);
        
        for (User student : students) {
            notificationService.sendSessionStartPrompt(student, session);
            notificationService.sendClassStartNotification(
                student.getId(), session.getSubject(), session.getRoom().getName(),
                LocalDateTime.ofInstant(session.getStartTime(), IST)
            );
        }
    }

    /**
     * Track whether we already sent "Mark Attendance" notification for this session.
     * Uses Firestore so the flag survives server restarts.
     */
    private void markNotificationSent(ClassroomSession session) {
        if (firestore == null) return;
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("sentAt", Instant.now().toString());
            data.put("subject", session.getSubject());
            firestore.collection("session_notifications").document(session.getId().toString()).set(data);
        } catch (Exception e) {
            log.error("Failed to mark notification sent for session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * For sessions created by resolveOrCreateSession (heartbeat path),
     * check if notification was already sent. If not, send it now.
     */
    private void sendNotificationIfNotAlreadySent(ClassroomSession session) {
        if (firestore == null) return;
        try {
            var doc = firestore.collection("session_notifications").document(session.getId().toString()).get().get();
            if (!doc.exists()) {
                log.info("📢 NOTIFY: Session {} was pre-created without notification. Sending now.", session.getId());
                notifySectionOnSessionStart(session);
                markNotificationSent(session);
            }
        } catch (Exception e) {
            log.error("Failed to check notification status for session {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendClassReminders(ZonedDateTime now) {
        ZonedDateTime reminderTime = now.plusMinutes(5);
        List<Timetable> reminders = timetableRepository.findByDayOfWeekAndStartTime(
                reminderTime.getDayOfWeek(), reminderTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        );

        for (Timetable slot : reminders) {
            if (Boolean.TRUE.equals(slot.getIsExamDay())) continue;
            
            List<User> students = userRepository.findBySectionId(slot.getSection().getId());
            for (User student : students) {
                try {
                    notificationService.sendClassReminderNotification(
                        student.getId(), slot.getSubject(), slot.getRoom().getName(), slot.getStartTime()
                    );
                } catch (Exception e) {
                    log.error("Failed reminder to student {}: {}", student.getId(), e.getMessage());
                }
            }
        }
    }

    private void autoEndExpiredSessions(Instant now) {
        // IMPORTANT: Wait 5 minutes after endTime before deactivating sessions as fallback.
        // The AIAbsentMarkerJob now processes them immediately (0m grace) after endTime.
        // We keep a small 5m buffer here to ensure the Marker Job has at least one run.
        Instant cutoffTime = now.minus(5, java.time.temporal.ChronoUnit.MINUTES);
        List<ClassroomSession> expired = sessionRepository.findByEndTimeBeforeAndActiveTrue(cutoffTime);
        
        for (ClassroomSession session : expired) {
            session.setActive(false);
            sessionRepository.save(session);
            log.info("🏁 AI CLEANUP: Auto-ended expired session: {} for '{}' (endTime was: {})", 
                    session.getId(), session.getSubject(), session.getEndTime());
        }
    }

    private void monitorStudentBehavior(ClassroomSession session) {
        List<AttendanceRecord> presenceList = attendanceRepository.findBySessionIdOrderByRecordedAtDesc(session.getId());
        for (AttendanceRecord record : presenceList) {
            if ("PRESENT".equalsIgnoreCase(record.getStatus())) {
                boolean isSuspicious = spatialEngine.checkSuspiciousBehavior(record.getStudent().getId(), session.getId());
                if (isSuspicious) {
                    SecurityAlert alert = new SecurityAlert();
                    alert.setUser(record.getStudent());
                    alert.setAlertType("SUSPICIOUS_MOVEMENT");
                    alert.setAlertMessage("AI detected suspicious movement vector toward boundary.");
                    alert.setSeverity("MEDIUM");
                    alert.setConfidence(0.85);
                    alertRepository.save(alert);
                }
            }
        }
    }

    private boolean timeIsAfterThreshold(LocalTime startTime, LocalDateTime now, int minutes) {
        return now.toLocalTime().isAfter(startTime.plusMinutes(minutes));
    }

    private boolean isFirstPeriodOfDay(Timetable slot) {
        if (slot.getSection() == null) return false;
        List<Timetable> daySlots = timetableRepository.findBySectionAndDayOfWeek(slot.getSection().getId(), slot.getDayOfWeek());
        if (daySlots == null || daySlots.isEmpty()) return false;
        
        LocalTime earliestTime = daySlots.stream()
            .map(Timetable::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(null);
            
        return slot.getStartTime().equals(earliestTime);
    }

    private void enforceAttendance(ClassroomSession session, Timetable slot) {
        if (Boolean.TRUE.equals(session.getIsExamDay())) return;

        List<User> students = userRepository.findBySectionId(slot.getSection().getId());
        for (User student : students) {
            if (attendanceRepository.existsBySession_IdAndStudent_Id(session.getId(), student.getId())) continue;
            if (shouldBypassEnforcement(student, session)) continue;
            markAbsent(student, session, slot);
        }
    }

    private boolean shouldBypassEnforcement(User student, ClassroomSession session) {
        // A. Digital Hall Pass Bypass (check expiry, not just existence)
        try {
            if (isHallPassActive(session.getId(), student.getId())) return true;
        } catch (Exception e) { log.error("Hall pass check fail: {}", e.getMessage()); }

        // B. Transition Bypass
        if (spatialEngine.isRoomTransitionInProgress(session.getSection().getId())) return true;

        // C. Network Glitch Bypass
        return learningOptimizer.isGlobalNetworkGlitch(session.getSection().getId());
    }

    private void markAbsent(User student, ClassroomSession session, Timetable slot) {
        AttendanceRecord absentRecord = new AttendanceRecord();
        absentRecord.setStudent(student);
        absentRecord.setSession(session);
        absentRecord.setStatus("ABSENT");
        absentRecord.setRecordedAt(Instant.now());
        absentRecord.setNote("AI Automated Absence Marking");
        absentRecord.setAiDecision(true);
        absentRecord.setConfidence(1.0);
        attendanceRepository.save(absentRecord);

        // notificationService.sendAttendanceAlert(student, slot, "AUTOMATED_ABSENCE");

        SecurityAlert alert = new SecurityAlert();
        alert.setUser(student);
        alert.setAlertType("AI_AUTO_ABSENT");
        alert.setAlertMessage("Student marked absent automatically (10m threshold)");
        alert.setSeverity("LOW");
        alert.setConfidence(0.98);
        alertRepository.save(alert);
    }

    private void enforceWalkoutRules(ClassroomSession session, Timetable slot) {
        List<AttendanceRecord> walkouts = attendanceRepository.findBySessionIdAndStatus(session.getId(), "WALK_OUT");
        log.info("🚶 WALKOUT CHECK: Session '{}' has {} WALK_OUT records", session.getSubject(), walkouts.size());

        for (AttendanceRecord record : walkouts) {
            User student = record.getStudent();
            String hallPassKey = "hallpass:" + session.getId() + ":" + student.getId();
            // Also check drift_tracking (written by AttendanceV1Service.handleOutOfBounds)
            String driftDocId = session.getId() + ":" + student.getId();

            try {
                if (firestore != null) {
                    // Check walkout_timers first (legacy)
                    var timerDoc = firestore.collection("walkout_timers").document(driftDocId).get().get();
                    
                    // If not found, check drift_tracking (used by heartbeat handler)
                    if (!timerDoc.exists()) {
                        var driftDoc = firestore.collection("drift_tracking").document(driftDocId).get().get();
                        if (driftDoc.exists()) {
                            log.info("🚶 WALKOUT: Found drift_tracking doc for student {}. Using firstDrift as walkout start.", student.getName());
                            // Use drift start time
                            Object firstDriftObj = driftDoc.get("firstDrift");
                            if (firstDriftObj instanceof com.google.cloud.Timestamp) {
                                Instant walkoutStart = ((com.google.cloud.Timestamp) firstDriftObj).toDate().toInstant();
                                if (Duration.between(walkoutStart, Instant.now()).toMinutes() >= 5) {
                                    // 🚀 TRANSITION PROTECTION: Don't mark absent if a room move is in progress
                                    if (spatialEngine.isRoomTransitionInProgress(session.getSection().getId())) {
                                        log.info("🟡 [WALKOUT_EXPIRED] Student {} - Move in progress. Holding status.", student.getName());
                                        continue;
                                    }

                                    if (!isHallPassActive(session.getId(), student.getId())) {
                                        log.warn("🤖 AI MONITOR [WALKOUT_EXPIRED]: Student {} - No return/pass after {}min. Revoking.", 
                                                student.getName(), Duration.between(walkoutStart, Instant.now()).toMinutes());
                                        record.setStatus("ABSENT");
                                        record.setAiDecision(true);
                                        record.setNote("AI Automated Absence: Unauthorized Walkout > 5m");
                                        attendanceRepository.save(record);
                                        notificationService.sendAttendanceAlert(student, slot, "UNAUTHORIZED_WALKOUT");
                                        firestore.collection("drift_tracking").document(driftDocId).delete();
                                    }
                                }
                            }
                        } else {
                            log.debug("🚶 WALKOUT: No timer/drift doc found for student {}. Skipping.", student.getName());
                        }
                        continue;
                    }

                    Instant walkoutStart = Instant.parse(timerDoc.getString("startTime"));
                    if (Duration.between(walkoutStart, Instant.now()).toMinutes() >= 5) {
                        if (!isHallPassActive(session.getId(), student.getId())) {
                            log.warn("🤖 AI MONITOR [WALKOUT_EXPIRED]: Student {} - No return/pass. Revoking.", student.getName());
                            record.setStatus("ABSENT");
                            record.setAiDecision(true);
                            record.setNote("AI Automated Absence: Unauthorized Walkout > 5m");
                            attendanceRepository.save(record);

                            notificationService.sendAttendanceAlert(student, slot, "UNAUTHORIZED_WALKOUT");
                            
                            SecurityAlert alert = new SecurityAlert();
                            alert.setUser(student);
                            alert.setAlertType("WALKOUT_ABSENCE");
                            alert.setSeverity("HIGH");
                            alertRepository.save(alert);

                            firestore.collection("walkout_timers").document(driftDocId).delete();
                        }
                    }
                }
            } catch (Exception e) { log.error("Walkout enforcement fail for {}: {}", student.getName(), e.getMessage()); }
        }
    }

    /**
     * Checks if a hall pass is both present AND not expired.
     * Fixes the bug where only document existence was checked, ignoring expiresAt.
     */
    private boolean isHallPassActive(UUID sessionId, UUID studentId) {
        if (firestore == null) return false;
        try {
            String hallPassKey = "hallpass:" + sessionId + ":" + studentId;
            var doc = firestore.collection("hall_passes").document(hallPassKey).get().get();
            if (!doc.exists()) return false;
            
            var data = doc.getData();
            if (data == null || !data.containsKey("expiresAt")) return false;
            
            Instant expiresAt = Instant.parse((String) data.get("expiresAt"));
            boolean active = Instant.now().isBefore(expiresAt);
            if (!active) {
                log.info("🕐 Hall pass EXPIRED for student {} in session {} (expired at {})", studentId, sessionId, expiresAt);
            }
            return active;
        } catch (Exception e) {
            log.error("Hall pass active check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isSessionCancelled(Timetable slot, ZonedDateTime now) {
        // 🚀 AI EMERGENCY BRAKE: Check for cancellation overrides
        // We look for any CANCELLATION type changes effective today for this section/subject
        Instant startOfDay = now.toLocalDate().atStartOfDay(IST).toInstant();
        Instant endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(IST).toInstant();
        
        return emergencyRepository.findAll().stream()
                .filter(esc -> esc.getChangeType() == com.example.smartAttendence.entity.EmergencySessionChange.EmergencyChangeType.CANCELLATION)
                .filter(esc -> esc.getEffectiveTimestamp().isAfter(startOfDay) && esc.getEffectiveTimestamp().isBefore(endOfDay))
                .anyMatch(esc -> {
                    ClassroomSession s = esc.getSession();
                    return s != null && s.getSection().getId().equals(slot.getSection().getId()) 
                            && s.getSubject().equalsIgnoreCase(slot.getSubject());
                });
    }
}
