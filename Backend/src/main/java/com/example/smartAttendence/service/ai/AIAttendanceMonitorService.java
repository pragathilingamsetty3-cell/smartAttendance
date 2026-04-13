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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private final TimetableRepository timetableRepository;
    private final ClassroomSessionV1Repository sessionRepository;
    private final AttendanceRecordV1Repository attendanceRepository;
    private final UserV1Repository userRepository;
    private final SecurityAlertV1Repository alertRepository;
    private final NotificationService notificationService;
    private final AcademicCalendarV1Repository calendarRepository;
    private final AISpatialMonitoringEngine spatialEngine;
    private final AILearningOptimizer learningOptimizer;
    private final StringRedisTemplate redisTemplate;

    /**
     * 🤖 AI AUTONOMOUS MONITORING TASK
     * Runs every minute to sync timetable with live presence
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void monitorActiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        // 🤖 AI RESTING MODE: Check if today is a Holiday
        if (calendarRepository.isHoliday(today)) {
            log.info("🤖 AI MONITOR [RESTING MODE]: Today is a holiday. Skipping autonomous scan.");
            return;
        }

        log.debug("🤖 AI MONITOR: Starting autonomous scan at {} {}", day, time);

        // 1. Find all active timetable slots for this minute
        List<Timetable> activeSlots = timetableRepository.findByDayOfWeekAndStartTimeBeforeAndEndTimeAfter(day, time, time);

        for (Timetable slot : activeSlots) {
            // 🤖 AI SESSION-SPECIFIC HOLIDAY CHECK
            if (Boolean.TRUE.equals(slot.getIsHoliday())) {
                // If a specific date is set, it must match today to be skipped
                if (slot.getHolidayDate() != null && !slot.getHolidayDate().equals(now.toLocalDate())) {
                    // This holiday is for a different date, proceed as normal class (or skip if you prefer)
                    // But usually, if isHoliday is true, we ONLY care if the date matches.
                    log.debug("🤖 AI MONITOR: Session '{}' is a holiday for {}, but today is {}. Proceeding with cautious scan.", 
                            slot.getSubject(), slot.getHolidayDate(), now.toLocalDate());
                } else {
                    log.info("🤖 AI MONITOR [RESTING MODE]: Session '{}' for {} marked as holiday. Skipping marking.", 
                            slot.getSubject(), slot.getSection().getName());
                    continue;
                }
            }
            processActiveSlot(slot, now);
        }
    }

    private void processActiveSlot(Timetable slot, LocalDateTime now) {
        UUID sectionId = slot.getSection().getId();
        LocalTime startTime = slot.getStartTime();
        
        // 2. Identify or create the active ClassroomSession for this slot
        Instant startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        ClassroomSession session = sessionRepository.findByTimetableIdAndDateRange(slot.getId(), startOfDay, endOfDay)
                .orElse(null);

        if (session == null) {
            log.info("🤖 AI MONITOR: No session found for active timetable [{} - {}]. Autogenerating session...", 
                    slot.getSubject(), slot.getSection().getName());
            session = createAutomaticSession(slot, now);
        } else if (!session.isActive()) {
            log.info("🤖 AI MONITOR: Session found for [{}] but inactive. Activating contextually...", slot.getSubject());
            session.setActive(true);
            session = sessionRepository.save(session);
        }

        // 3. 10-Minute Check-in Enforcement
        if (timeIsAfterThreshold(startTime, now, 10)) {
            enforceAttendance(session, slot);
        }

        // 🕐 4. 5-Minute Unauthorized Walkout Enforcement
        enforceWalkoutRules(session, slot);

        // 🤖 AI CONTINUOUS SILENT SCAN - Check for suspicious movement even for 'Present' students
        monitorStudentBehavior(session);
    }

    private ClassroomSession createAutomaticSession(Timetable slot, LocalDateTime now) {
        ClassroomSession session = new ClassroomSession();
        session.setTimetable(slot);
        session.setRoom(slot.getRoom());
        session.setFaculty(slot.getFaculty());
        session.setSection(slot.getSection());
        session.setSubject(slot.getSubject());
        session.setAutoGenerated(true);
        session.setActive(true);
        session.setIsExamDay(slot.getIsExamDay() != null ? slot.getIsExamDay() : false);
        session.setIsHoliday(slot.getIsHoliday() != null ? slot.getIsHoliday() : false);
        
        // Convert Timetable LocalTime to Instant for today
        ZoneId zone = ZoneId.systemDefault();
        Instant start = now.toLocalDate().atTime(slot.getStartTime()).atZone(zone).toInstant();
        Instant end = now.toLocalDate().atTime(slot.getEndTime()).atZone(zone).toInstant();
        
        // Handle midnight transition for late-night test slots
        if (end.isBefore(start)) {
            end = now.toLocalDate().plusDays(1).atTime(slot.getEndTime()).atZone(zone).toInstant();
        }
        
        session.setStartTime(start);
        session.setEndTime(end);
        
        // Sync Geofence from Room boundary
        if (slot.getRoom() != null) {
            session.setGeofencePolygon(slot.getRoom().getBoundaryPolygon());
        }
        
        ClassroomSession savedSession = sessionRepository.save(session);
        
        // 🤖 AI BROADCAST: Notify all students in the section that class has started
        notifySectionOnSessionStart(savedSession);
        
        return savedSession;
    }

    private void notifySectionOnSessionStart(ClassroomSession session) {
        List<User> students = userRepository.findBySectionId(session.getSection().getId());
        log.info("🤖 AI BROADCAST: Sending session start prompts to {} students in section {}", 
                students.size(), session.getSection().getName());
        
        for (User student : students) {
            notificationService.sendSessionStartPrompt(student, session);
        }
    }

    private void monitorStudentBehavior(ClassroomSession session) {
        // AI scans all present students for suspicious behavior
        List<AttendanceRecord> presenceList = attendanceRepository.findBySessionIdOrderByRecordedAtDesc(session.getId());
        
        for (AttendanceRecord record : presenceList) {
            if ("PRESENT".equalsIgnoreCase(record.getStatus())) {
                boolean isSuspicious = spatialEngine.checkSuspiciousBehavior(record.getStudent().getId(), session.getId());
                
                if (isSuspicious) {
                    log.warn("🤖 AI ANALYTICS [SUSPICIOUS]: Student {} ({}) showing unauthorized movement.", 
                            record.getStudent().getName(), record.getStudent().getRegistrationNumber());
                    
                    // Log AI Security Alert for the Dashboard
                    SecurityAlert alert = new SecurityAlert();
                    alert.setUser(record.getStudent());
                    alert.setAlertType("SUSPICIOUS_MOVEMENT");
                    alert.setAlertMessage("AI detected suspicious movement vector toward boundary without Hall Pass.");
                    alert.setSeverity("MEDIUM");
                    alert.setConfidence(0.85);
                    alertRepository.save(alert);
                }
            }
        }
    }


    private boolean timeIsAfterThreshold(LocalTime startTime, LocalDateTime now, int minutes) {
        LocalTime threshold = startTime.plusMinutes(minutes);
        return now.toLocalTime().isAfter(threshold);
    }

    private void enforceAttendance(ClassroomSession session, Timetable slot) {
        // 🤖 AI EXAM MODE: Skip autonomous absence marking on Exam Days
        if (Boolean.TRUE.equals(session.getIsExamDay())) {
            log.info("🤖 AI MONITOR [EXAM MODE]: Session {} is an exam. Skipping autonomous marking.", session.getId());
            return;
        }

        List<User> students = userRepository.findBySectionId(slot.getSection().getId());

        for (User student : students) {
            // Skip if already marked Present or Late
            if (attendanceRepository.existsBySession_IdAndStudent_Id(session.getId(), student.getId())) {
                continue;
            }

            // 🤖 AI CONTEXTUAL BYPASS CHECKS
            if (shouldBypassEnforcement(student, session)) {
                continue;
            }

            // 4. Mark ABSENT and notify stakeholders
            markAbsent(student, session, slot);
        }
    }

    private boolean shouldBypassEnforcement(User student, ClassroomSession session) {
        // A. Digital Hall Pass Bypass
        String hallPassKey = "hallpass:" + session.getId() + ":" + student.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(hallPassKey))) {
            log.debug("🤖 AI MONITOR: Skipping student {} - Active Hall Pass", student.getName());
            return true;
        }

        // B. Sudden Room Change Bypass
        if (spatialEngine.isRoomTransitionInProgress(session.getSection().getId())) {
             log.info("🤖 AI MONITOR: Skipping student {} - Room Transition Grace Period Active", student.getName());
             return true;
        }

        // C. Network Glitch Bypass
        if (isGlobalNetworkGlitchDetected(session.getSection().getId())) {
            log.warn("🤖 AI MONITOR: Skipping student {} - Potential Network Glitch Detected for Section", student.getName());
            return true;
        }

        return false;
    }

    private void markAbsent(User student, ClassroomSession session, Timetable slot) {
        AttendanceRecord absentRecord = new AttendanceRecord();
        absentRecord.setStudent(student);
        absentRecord.setSession(session);
        absentRecord.setStatus("ABSENT");
        absentRecord.setRecordedAt(Instant.now());
        absentRecord.setNote("AI Automated Absence Marking (10m No-Show)");
        absentRecord.setAiDecision(true);
        absentRecord.setConfidence(1.0);
        
        attendanceRepository.save(absentRecord);

        // Notify stakeholders
        notificationService.sendAttendanceAlert(student, slot, "AUTOMATED_ABSENCE");

        // Log security/AI alert
        SecurityAlert alert = new SecurityAlert();
        alert.setUser(student);
        alert.setAlertType("AI_AUTO_ABSENT");
        alert.setAlertMessage("Student marked absent automatically for " + slot.getSubject() + " (10m threshold exceeded)");
        alert.setSeverity("LOW");
        alert.setConfidence(0.98);
        alertRepository.save(alert);
        
        log.info("🤖 AI MONITOR: Marked {} as ABSENT for {} session (AI Decision)", student.getName(), slot.getSubject());
    }


    private void enforceWalkoutRules(ClassroomSession session, Timetable slot) {
        // Find all students currently marked as WALK_OUT for this session
        List<AttendanceRecord> walkouts = attendanceRepository.findBySessionIdAndStatus(session.getId(), "WALK_OUT");

        for (AttendanceRecord record : walkouts) {
            User student = record.getStudent();
            String timerKey = "walkout_start:" + session.getId() + ":" + student.getId();
            String hallPassKey = "hallpass:" + session.getId() + ":" + student.getId();

            // 1. Check if we have a start time for this walkout
            String startTimeStr = redisTemplate.opsForValue().get(timerKey);
            if (startTimeStr == null) continue;

            Instant walkoutStart = Instant.parse(startTimeStr);
            long minutesOut = Duration.between(walkoutStart, Instant.now()).toMinutes();

            // 2. 5-Minute Rule Check
            if (minutesOut >= 5) {
                // 3. Check for Permission (Hall Pass)
                if (Boolean.FALSE.equals(redisTemplate.hasKey(hallPassKey))) {
                    log.warn("🤖 AI MONITOR [WALKOUT_EXPIRED]: Student {} has not returned after 5 minutes without permission. Revoking attendance.", 
                            student.getName());
                    
                    // Convert to ABSENT
                    record.setStatus("ABSENT");
                    record.setAiDecision(true);
                    record.setNote("AI Automated Absence: Unauthorized Walkout > 5 Minutes");
                    attendanceRepository.save(record);

                    // Notify parents and log alert
                    notificationService.sendAttendanceAlert(student, slot, "UNAUTHORIZED_WALKOUT");
                    
                    SecurityAlert alert = new SecurityAlert();
                    alert.setUser(student);
                    alert.setAlertType("WALKOUT_ABSENCE");
                    alert.setAlertMessage("Student marked ABSENT: Unauthorized walkout exceeded 5-minute security threshold.");
                    alert.setSeverity("HIGH");
                    alert.setConfidence(0.98);
                    alertRepository.save(alert);

                    // Clear the timer
                    redisTemplate.delete(timerKey);
                }
            }
        }
    }

    private boolean isGlobalNetworkGlitchDetected(UUID sectionId) {
        return learningOptimizer.isGlobalNetworkGlitch(sectionId);
    }
}
