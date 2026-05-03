package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.StudentDashboardStatsDTO;
import com.example.smartAttendence.dto.v1.TimetableResponseDTO;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.entity.Department;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.DepartmentRepository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class StudentV1Service {

    private static final java.time.ZoneId IST = java.time.ZoneId.of("Asia/Kolkata");
    private final UserV1Repository userV1Repository;
    private final AttendanceRecordV1Repository attendanceRecordRepository;
    private final TimetableRepository timetableRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final ClassroomSessionV1Repository classroomSessionRepository;
    private final FacultyHallPassService facultyHallPassService;

    /**
     * Get student dashboard statistics - CACHED for high speed
     */
    
    public StudentDashboardStatsDTO getStudentDashboardStats(UUID studentId) {
        User student = userV1Repository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(IST);
        
        // 1. Calculate Attendance Metrics
        List<AttendanceRecord> allRecordsRaw = attendanceRecordRepository.findByStudentIdAndRecordedAtAfter(studentId, Instant.EPOCH);
        
        // 🚀 NUMERATOR: Attended sessions (PRESENT/LATE)
        long attendedAllTime = allRecordsRaw.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .count();
        
        java.time.Instant startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay(IST).toInstant();
        long attendedThisMonth = allRecordsRaw.stream()
                .filter(r -> r.getRecordedAt().isAfter(startOfMonth))
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .count();

        // 🚀 DENOMINATOR: Total sessions conducted for the SECTION
        UUID sectionId = student.getSectionId();
        long totalSessionsForSection = 0;
        long totalSessionsThisMonth = 0;
        
        if (sectionId != null) {
            totalSessionsForSection = classroomSessionRepository.countBySectionIdAndActiveFalse(sectionId);
            totalSessionsThisMonth = classroomSessionRepository.countBySectionIdAndActiveFalseAndStartTimeAfter(sectionId, startOfMonth);
        } else {
            // Fallback to record count if section is missing (should not happen with our self-healing)
            totalSessionsForSection = allRecordsRaw.stream().filter(r -> r.getSession() != null && !r.getSession().isActive()).count();
            totalSessionsThisMonth = allRecordsRaw.stream().filter(r -> r.getRecordedAt().isAfter(startOfMonth) && r.getSession() != null && !r.getSession().isActive()).count();
        }
        
        double overallAttendance = totalSessionsForSection == 0 ? 0.0 : (double) attendedAllTime * 100.0 / totalSessionsForSection;
        
        // 🚀 OPTIMIZATION: Only use today's records for the dashboard "Marked" check
        java.time.Instant startOfToday = now.toLocalDate().atStartOfDay(IST).toInstant();
        List<AttendanceRecord> todayRecords = allRecordsRaw.stream()
                .filter(r -> r.getRecordedAt().isAfter(startOfToday))
                .collect(Collectors.toList());
        
        // 2. Fetch Today's Classes (IST)
        DayOfWeek today = now.getDayOfWeek();
        List<Timetable> todayTimetable = new ArrayList<>();
        Map<String, Object> debugInfo = new HashMap<>();
        
        java.time.LocalDate todayDate = now.toLocalDate();
        log.info("🔍 DASHBOARD DEBUG: Fetching classes for Student: {} (Reg: {}), Section: {}, Day: {}", 
                student.getName(), student.getRegistrationNumber(), 
                student.getSection() != null ? student.getSection().getName() : "NULL", 
                today);

        if (student.getSection() != null || student.getSectionId() != null) {
            sectionId = student.getSectionId();
            
            List<Timetable> allForSection = timetableRepository.findBySectionId(sectionId);
            log.info("🔍 DASHBOARD DEBUG: Total classes in DB for section {}: {}", sectionId, allForSection.size());
            
            List<Timetable> todayRaw = timetableRepository.findBySectionAndDayOfWeek(sectionId, today);
            log.info("🔍 DASHBOARD DEBUG: Raw classes for today ({}): {}", today, todayRaw.size());

            // 🛠️ BRUTE-FORCE SELF-HEALING: Search GLOBALLY for any section with this name that has classes
            if (allForSection.isEmpty()) {
                String sectionName = student.getSection() != null ? student.getSection().getName() : null;
                log.warn("🚨 GLOBAL SELF-HEALING: No classes for ID {}. Searching by name '{}' globally...", sectionId, sectionName);
                
                if (sectionName != null) {
                    List<Timetable> foundByName = timetableRepository.findBySectionName(sectionName);
                    if (!foundByName.isEmpty()) {
                        Section correctSection = foundByName.get(0).getSection();
                        log.info("✅ SUCCESS: Found {} classes in Section ID {}. REPAIRING PROFILE...", foundByName.size(), correctSection.getId());
                        
                        student.setSection(correctSection);
                        student.setSectionId(correctSection.getId());
                        userV1Repository.save(student);
                        
                        sectionId = correctSection.getId();
                        allForSection = foundByName;
                        todayRaw = timetableRepository.findBySectionAndDayOfWeek(sectionId, today);
                    }
                }
            }

            todayTimetable = todayRaw.stream()
                    .filter(t -> {
                        // 🛠️ RELAXED FILTERING: If dates are null, allow it.
                        boolean startOk = t.getStartDate() == null || !todayDate.isBefore(t.getStartDate());
                        boolean endOk = t.getEndDate() == null || !todayDate.isAfter(t.getEndDate());
                        
                        // 🔍 LOG EVERY FILTERING DECISION
                        if (!startOk || !endOk) {
                            log.warn("🚨 DASHBOARD FILTER: Class {} ({}) filtered out. Start: {}, End: {}, Today: {}", 
                                    t.getSubject(), t.getDayOfWeek(), t.getStartDate(), t.getEndDate(), todayDate);
                        }
                        return startOk && endOk;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("sectionId", sectionId);
            debug.put("today", today.toString());
            debug.put("totalSectionClasses", allForSection.size());
            debug.put("allSubjectsForSection", allForSection.stream()
                    .map(t -> t.getSubject() + " [" + t.getDayOfWeek() + "]")
                    .distinct()
                    .collect(Collectors.toList()));
            debug.put("todayRawClasses", todayRaw.size());
            debug.put("todayFilteredClasses", todayTimetable.size());
            debug.put("serverTime", now.toString());
            debug.put("serverDate", todayDate.toString());
            
            debugInfo = debug; // Set to the local map
        } else {
            log.warn("⚠️ DASHBOARD WARN: Student {} has NO SECTION ID assigned in their profile!", student.getName());
        }

        List<TimetableResponseDTO> todayClasses = todayTimetable.stream()
                .map(this::mapToTimetableResponse)
                .collect(Collectors.toList());

        // 3. Find Active Session
        LocalTime nowTime = now.toLocalTime();
        Timetable activeSessionEntity = todayTimetable.stream()
                .filter(t -> {
                    LocalTime start = t.getStartTime();
                    LocalTime end = t.getEndTime();
                    return !nowTime.isBefore(start) && !nowTime.isAfter(end);
                })
                .max(Comparator.comparing(Timetable::getStartTime)) // 🚀 FIX: If overlapping, pick the one that started latest
                .orElse(null);

        TimetableResponseDTO activeSession = mapToTimetableResponse(activeSessionEntity);
        
        // 3b. Check if student already marked attendance for the active session today
        boolean attendanceMarked = false;
        if (activeSessionEntity != null) {
            // Check for PRESENT/LATE record specifically for this active session's timetable
            final UUID activeTimetableId = activeSessionEntity.getId();
            attendanceMarked = todayRecords.stream()
                    .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()) || "WALK_OUT".equals(r.getStatus()))
                    .anyMatch(r -> r.getSession() != null 
                            && r.getSession().getTimetable() != null
                            && activeTimetableId.equals(r.getSession().getTimetable().getId()));
            log.info("📋 ATTENDANCE CHECK: Student {} attendance marked for active session (timetable={}) = {}", 
                    student.getName(), activeTimetableId, attendanceMarked);
        }

        // 4. Attendance Trend (Last 7 Days)
        Map<String, Double> attendanceTrend = calculateAttendanceTrend(studentId, now);

        // 5. Recent Hall Pass (Latest status)
        var recentHallPass = facultyHallPassService.getLatestHallPassForStudent(studentId);

        // 6. Section and Department Info
        String deptName = "N/A";
        if (student.getDepartment() != null) {
            try {
                UUID deptId = UUID.fromString(student.getDepartment());
                deptName = departmentRepository.findById(deptId)
                        .map(Department::getName)
                        .orElse(student.getDepartment());
            } catch (Exception e) {
                deptName = student.getDepartment();
            }
        }
        
        String sectionName = student.getSection() != null ? student.getSection().getName() : "N/A";

        // 6. AI Insights for Student
        double avgConfidence = allRecordsRaw.stream()
                .filter(r -> r.getConfidence() != null)
                .mapToDouble(AttendanceRecord::getConfidence)
                .average()
                .orElse(92.0); // Baseline confidence for new students

        debugInfo.put("studentId", studentId);
        debugInfo.put("sectionId", student.getSectionId());
        debugInfo.put("hasSectionEntity", student.getSection() != null);
        debugInfo.put("todayClassesCount", todayClasses.size());
        debugInfo.put("totalTimetablesInDB", timetableRepository.count());
        debugInfo.put("rawTodayTimetableSize", todayTimetable.size());

        return StudentDashboardStatsDTO.builder()
                .overallAttendance(Math.round(overallAttendance * 10.0) / 10.0)
                .attendedClasses((int) attendedThisMonth)
                .totalClasses((int) totalSessionsThisMonth)
                .attendanceTrend(attendanceTrend)
                .todayClasses(todayClasses)
                .activeSession(activeSession)
                .attendanceMarked(attendanceMarked)
                .recentHallPass(recentHallPass)
                .departmentName(deptName)
                .sectionName(sectionName)
                .semester(student.getSemester() != null ? student.getSemester() : 1)
                .registrationNumber(student.getRegistrationNumber())
                .sectionId(student.getSectionId())
                .aiVerificationConfidence(avgConfidence)
                .debugInfo(debugInfo)
                .build();
    }

    private Map<String, Double> calculateAttendanceTrend(UUID studentId, java.time.ZonedDateTime now) {
        Map<String, Double> trend = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        Instant since = now.minusDays(7).toInstant();
        List<Object[]> results = attendanceRecordRepository.getAttendanceTrendForStudent(studentId, since);
        
        // Initialize with 0s for last 7 days
        for (int i = 6; i >= 0; i--) {
            trend.put(now.minusDays(i).format(formatter), 0.0);
        }
        
        // Populate with real data
        for (Object[] row : results) {
            if (row[0] != null && row[1] != null) {
                String day = row[0].toString();
                Double rate = ((Number) row[1]).doubleValue();
                trend.put(day, Math.round(rate * 10.0) / 10.0);
            }
        }
        
        return trend;
    }

    private TimetableResponseDTO mapToTimetableResponse(Timetable t) {
        if (t == null) return null;
        
        return TimetableResponseDTO.builder()
                .id(t.getId())
                .subject(t.getSubject())
                .dayOfWeek(t.getDayOfWeek() != null ? t.getDayOfWeek().name() : null)
                .startTime(t.getStartTime() != null ? t.getStartTime().toString() : null)
                .endTime(t.getEndTime() != null ? t.getEndTime().toString() : null)
                .isExamDay(t.getIsExamDay())
                .isAdhoc(t.getIsAdhoc())
                .startDate(t.getStartDate() != null ? t.getStartDate().toString() : null)
                .endDate(t.getEndDate() != null ? t.getEndDate().toString() : null)
                .description(t.getDescription())
                .room(t.getRoom() != null ? TimetableResponseDTO.RoomInfo.builder()
                        .id(t.getRoom().getId())
                        .name(t.getRoom().getName())
                        .building(t.getRoom().getBuilding())
                        .floor(t.getRoom().getFloor())
                        .build() : null)
                .faculty(t.getFaculty() != null ? TimetableResponseDTO.FacultyInfo.builder()
                        .id(t.getFaculty().getId())
                        .name(t.getFaculty().getName())
                        .email(t.getFaculty().getEmail())
                        .build() : null)
                .section(t.getSection() != null ? TimetableResponseDTO.SectionInfo.builder()
                        .id(t.getSection().getId())
                        .name(t.getSection().getName())
                        .build() : null)
                .build();
    }
}
