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
import java.util.UUID;
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

        if (session == null) {
            session = createAutomaticSession(slot, now);
        } else if (!session.isActive()) {
            session.setActive(true);
            session = sessionRepository.save(session);
        }

        if (timeIsAfterThreshold(slot.getStartTime(), now.toLocalDateTime(), 10)) {
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
        return savedSession;
    }

    private void notifySectionOnSessionStart(ClassroomSession session) {
        UUID sectionId = session.getSection().getId();
        List<User> students = userRepository.findBySectionIdExplicit(sectionId);
        
        log.info("📢 NOTIFY: Sending 'Mark Attendance' prompts to {} students in Section: {} (ID: {})", 
                students.size(), session.getSection().getName(), sectionId);
        
        for (User student : students) {
            notificationService.sendSessionStartPrompt(student, session);
            // Also send the Class Start notification for redundancy/clarity
            notificationService.sendClassStartNotification(
                student.getId(), session.getSubject(), session.getRoom().getName(),
                LocalDateTime.ofInstant(session.getStartTime(), IST)
            );
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
        // IMPORTANT: Wait 15 minutes after endTime before deactivating sessions.
        // The AIAbsentMarkerJob needs sessions to be active=true and processes them
        // 10 minutes after endTime. If we deactivate too early, absent emails never get sent.
        Instant cutoffTime = now.minus(15, java.time.temporal.ChronoUnit.MINUTES);
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
        // A. Digital Hall Pass Bypass (Firestore Persistent)
        try {
            String docId = session.getId() + "_" + student.getId();
            if (firestore != null && firestore.collection("hall_passes").document(docId).get().get().exists()) return true;
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

        notificationService.sendAttendanceAlert(student, slot, "AUTOMATED_ABSENCE");

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
            String docId = session.getId() + "_" + student.getId();
            // Also check drift_tracking (written by AttendanceV1Service.handleOutOfBounds)
            String driftDocId = session.getId() + ":" + student.getId();

            try {
                if (firestore != null) {
                    // Check walkout_timers first (legacy)
                    var timerDoc = firestore.collection("walkout_timers").document(docId).get().get();
                    
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
                                    if (!firestore.collection("hall_passes").document(docId).get().get().exists()) {
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
                        if (!firestore.collection("hall_passes").document(docId).get().get().exists()) {
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

                            firestore.collection("walkout_timers").document(docId).delete();
                        }
                    }
                }
            } catch (Exception e) { log.error("Walkout enforcement fail for {}: {}", student.getName(), e.getMessage()); }
        }
    }
}
