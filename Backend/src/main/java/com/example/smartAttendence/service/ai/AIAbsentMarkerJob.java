package com.example.smartAttendence.service.ai;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartAttendence.enums.Role;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AIAbsentMarkerJob {

    private static final Logger logger = LoggerFactory.getLogger(AIAbsentMarkerJob.class);
    
    // 10 minute grace period
    private static final int GRACE_PERIOD_MINUTES = 10;

    private final ClassroomSessionV1Repository sessionRepository;
    private final AttendanceRecordV1Repository attendanceRepository;
    private final UserV1Repository userRepository;
    private final EmailService emailService;

    @Autowired
    public AIAbsentMarkerJob(
            ClassroomSessionV1Repository sessionRepository,
            AttendanceRecordV1Repository attendanceRepository,
            UserV1Repository userRepository,
            EmailService emailService) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Runs every 5 minutes to find sessions that ended strictly before the grace period threshold.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processAutoAbsentMarking() {
        logger.info("🤖 AI Auto-Absent Marker: Scanning for ended sessions to finalize attendance...");
        
        Instant thresholdTime = Instant.now().minus(GRACE_PERIOD_MINUTES, ChronoUnit.MINUTES);
        
        // Find sessions that ended before our threshold but are still marked active
        // Or find sessions that just became inactive but we haven't processed yet.
        // Actually, the system might keep them 'active=true' if the faculty didn't manually end them.
        // Let's rely on the scheduled endTime.
        
        // Ideally we'd have an 'attendanceProcessed' flag on ClassroomSession.
        // For now, we will query sessions where endTime < thresholdTime and active = true
        // Then we mark them as active = false to signify we processed them.
        
        List<ClassroomSession> sessionsToProcess = sessionRepository.findByActiveTrue()
                .stream()
                .filter(s -> s.getEndTime() != null && s.getEndTime().isBefore(thresholdTime))
                .collect(Collectors.toList());

        for (ClassroomSession session : sessionsToProcess) {
            processSession(session);
            // Mark session as inactive so we don't process it again
            session.setActive(false);
            sessionRepository.save(session);
            logger.info("✅ Finished processing auto-absent for session: {}", session.getId());
        }
    }

    private void processSession(ClassroomSession session) {
        if (session.getSection() == null) {
            logger.warn("Session {} has no section assigned. Skipping.", session.getId());
            return;
        }

        // Get all students in this section
        List<User> expectedStudents = userRepository.findBySectionIdAndRole(session.getSection().getId(), Role.STUDENT);
        
        // Get all recorded attendance for this session
        List<AttendanceRecord> records = attendanceRepository.findBySessionIdOrderByRecordedAtAsc(session.getId());
        Set<java.util.UUID> attendedStudentIds = records.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .map(r -> r.getStudent().getId())
                .collect(Collectors.toSet());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("Asia/Kolkata"));
        String dateStr = formatter.format(session.getStartTime());
        String subjectName = session.getSubject() != null ? session.getSubject() : "Class";

        int absentCount = 0;

        for (User student : expectedStudents) {
            if (!attendedStudentIds.contains(student.getId())) {
                // Check if they already have an ABSENT record (e.g. from Walk-out watcher)
                Optional<AttendanceRecord> existingRecord = attendanceRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(student.getId(), session.getId());
                
                if (existingRecord.isEmpty()) {
                    // Create new ABSENT record
                    AttendanceRecord record = new AttendanceRecord();
                    record.setStudent(student);
                    record.setSession(session);
                    record.setStatus("ABSENT");
                    record.setRecordedAt(Instant.now());
                    record.setNote("Auto-marked absent: Did not verify attendance during session.");
                    record.setAiDecision(true);
                    record.setConfidence(1.0);
                    
                    attendanceRepository.save(record);
                    
                    // Trigger email notification
                    emailService.sendAbsentNotification(student.getEmail(), student.getName(), subjectName, dateStr);
                    absentCount++;
                } else if (!"ABSENT".equals(existingRecord.get().getStatus())) {
                     // They have a record but it's not PRESENT/LATE/ABSENT. Force absent.
                     AttendanceRecord rec = existingRecord.get();
                     rec.setStatus("ABSENT");
                     rec.setNote("Auto-marked absent: Did not verify attendance properly.");
                     rec.setRecordedAt(Instant.now());
                     attendanceRepository.save(rec);
                     
                     emailService.sendAbsentNotification(student.getEmail(), student.getName(), subjectName, dateStr);
                     absentCount++;
                }
            }
        }
        
        logger.info("🤖 Session {} auto-processing complete. Marked {} students absent out of {}.", session.getId(), absentCount, expectedStudents.size());
    }
}
