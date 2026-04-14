package com.example.smartAttendence.scheduler;

import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.entity.AcademicCalendar;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.v1.AcademicCalendarV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.service.v1.NotificationService;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AutonomousSessionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousSessionScheduler.class);
    private static final int SESSION_START_BUFFER_MINUTES = 5;
    private static final int SESSION_END_BUFFER_MINUTES = 1;
    private static final int SRID_WGS84 = 4326;

    private final TimetableRepository timetableRepository;
    private final AcademicCalendarV1Repository academicCalendarRepository;
    private final ClassroomSessionV1Repository sessionRepository;
    private final UserV1Repository userRepository;
    private final NotificationService notificationService;
    private final SharedUtilityService sharedUtilityService;
    private final GeometryFactory geometryFactory;

    public AutonomousSessionScheduler(
            TimetableRepository timetableRepository,
            AcademicCalendarV1Repository academicCalendarRepository,
            ClassroomSessionV1Repository sessionRepository,
            UserV1Repository userRepository,
            NotificationService notificationService,
            SharedUtilityService sharedUtilityService
    ) {
        this.timetableRepository = timetableRepository;
        this.academicCalendarRepository = academicCalendarRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.sharedUtilityService = sharedUtilityService;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processTimetableSessions() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();
        LocalDate currentDate = now.toLocalDate();

        AcademicCalendar todayCalendar = academicCalendarRepository.findByDate(currentDate).orElse(null);
        if (todayCalendar != null && todayCalendar.getDayType() == AcademicCalendar.DayType.HOLIDAY) {
            logger.info("⏸️ AI MONITOR [RESTING]: Today is a holiday. Skipping session creation.");
            return;
        }

        List<Timetable> activeTimetables = timetableRepository.findByDayOfWeekAndStartTimeBeforeAndEndTimeAfter(
                currentDay, currentTime.plusMinutes(SESSION_START_BUFFER_MINUTES), currentTime.minusMinutes(SESSION_END_BUFFER_MINUTES)
        );

        for (Timetable timetable : activeTimetables) {
            try {
                processTimetableEntry(timetable, currentDate, currentTime);
            } catch (Exception e) {
                logger.error("❌ Error deploying session for timetable {}: {}", timetable.getId(), e.getMessage());
            }
        }
    }

    private void processTimetableEntry(Timetable timetable, LocalDate today, LocalTime nowTime) {
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        boolean exists = sessionRepository.existsByTimetableIdAndDateRangeV1(timetable.getId(), startOfDay, endOfDay);
        if (exists) return;

        LocalDateTime sessionStart = today.atTime(timetable.getStartTime());
        LocalDateTime sessionEnd = today.atTime(timetable.getEndTime());

        logger.info("🚀 AI ACTION: Deploying session for Subject: '{}' in Room: '{}'", 
                timetable.getSubject(), timetable.getRoom().getName());
        
        ClassroomSession session = createSessionFromTimetable(timetable, sessionStart, sessionEnd);
        sessionRepository.save(session);
        
        if (!timetable.getIsExamDay()) {
            sendSessionStartNotifications(session, timetable);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void sendClassReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(5);
        
        List<Timetable> reminders = timetableRepository.findByDayOfWeekAndStartTime(
                reminderTime.getDayOfWeek(), reminderTime.toLocalTime()
        );

        for (Timetable timetable : reminders) {
            if (!timetable.getIsExamDay()) {
                sendClassReminderNotifications(timetable);
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void autoEndExpiredSessions() {
        Instant now = Instant.now();
        Instant cutoffTime = now.minus(SESSION_END_BUFFER_MINUTES, ChronoUnit.MINUTES);
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        // 1. End sessions that logically timed out
        List<ClassroomSession> expired = sessionRepository.findByEndTimeBeforeAndActiveTrue(cutoffTime);
        
        // 2. End "Zombie" sessions that started yesterday or earlier but are still marked active
        List<ClassroomSession> zombies = sessionRepository.findByActiveTrue().stream()
                .filter(session -> session.getStartTime().isBefore(startOfToday))
                .collect(java.util.stream.Collectors.toList());

        expired.addAll(zombies);
        
        for (ClassroomSession session : expired.stream().distinct().collect(java.util.stream.Collectors.toList())) {
            session.setActive(false);
            sessionRepository.save(session);
            logger.info("🧹 AI CLEANUP: Auto-ended expired/zombie session: {}", session.getId());
        }
    }

    private ClassroomSession createSessionFromTimetable(Timetable timetable, LocalDateTime start, LocalDateTime end) {
        ClassroomSession session = new ClassroomSession();
        session.setTimetable(timetable);
        session.setSubject(timetable.getSubject());
        session.setRoom(timetable.getRoom());
        session.setFaculty(timetable.getFaculty());
        session.setSection(timetable.getSection());
        session.setStartTime(start.atZone(ZoneId.systemDefault()).toInstant());
        session.setEndTime(end.atZone(ZoneId.systemDefault()).toInstant());
        session.setActive(true);
        session.setIsExamDay(timetable.getIsExamDay());

        if (timetable.getRoom().getBoundaryPolygon() != null) {
            session.setGeofencePolygon((Polygon) timetable.getRoom().getBoundaryPolygon().copy());
        }
        return session;
    }

    private void sendSessionStartNotifications(ClassroomSession session, Timetable timetable) {
        if (timetable.getSection() == null) return;
        
        List<User> students = sharedUtilityService.getStudentsBySection(timetable.getSection().getId());
        for (User student : students) {
            try {
                notificationService.sendClassStartNotification(
                    student.getId(), session.getSubject(), session.getRoom().getName(),
                    LocalDateTime.ofInstant(session.getStartTime(), ZoneId.systemDefault())
                );
            } catch (Exception e) {
                logger.error("Failed notification to student {}: {}", student.getId(), e.getMessage());
            }
        }
    }

    private void sendClassReminderNotifications(Timetable timetable) {
        if (timetable.getSection() == null) return;
        
        List<User> students = userRepository.findBySectionAndRole(timetable.getSection(), com.example.smartAttendence.enums.Role.STUDENT);
        for (User student : students) {
            try {
                notificationService.sendClassReminderNotification(
                    student.getId(), timetable.getSubject(), timetable.getRoom().getName(), timetable.getStartTime()
                );
            } catch (Exception e) {
                logger.error("Failed reminder to student {}: {}", student.getId(), e.getMessage());
            }
        }
    }
}
