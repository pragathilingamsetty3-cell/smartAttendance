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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final FacultyHallPassService facultyHallPassService;
    private final com.example.smartAttendence.service.ai.AILearningOptimizer learningOptimizer;

    /**
     * Get student dashboard statistics - CACHED for high speed
     */
    @org.springframework.cache.annotation.Cacheable(value = "dashboardStats", key = "#studentId")
    public StudentDashboardStatsDTO getStudentDashboardStats(UUID studentId) {
        User student = userV1Repository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(IST);
        
        // 1. Calculate Attendance Metrics
        Instant thirtyDaysAgo = now.minusDays(30).toInstant();
        List<AttendanceRecord> recentRecords = attendanceRecordRepository.findByStudentIdAndRecordedAtAfter(studentId, thirtyDaysAgo);
        
        long verifiedCount = recentRecords.stream()
                .filter(r -> "PRESENT".equalsIgnoreCase(r.getStatus()) || "LATE".equalsIgnoreCase(r.getStatus()))
                .count();
        long walkOutCount = recentRecords.stream()
                .filter(r -> "WALK_OUT".equalsIgnoreCase(r.getStatus()))
                .count();
        long totalCount = recentRecords.size();

        double attendanceRate = 100.0;
        if (totalCount > 0) {
            attendanceRate = ((double) verifiedCount / totalCount) * 100.0;
        }

        // 2. Fetch Today's Classes
        DayOfWeek today = now.getDayOfWeek();
        List<Timetable> todayTimetable = new ArrayList<>();
        if (student.getSection() != null) {
            todayTimetable = timetableRepository.findBySectionAndDayOfWeek(student.getSection().getId(), today);
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
                .findFirst()
                .orElse(null);

        TimetableResponseDTO activeSession = mapToTimetableResponse(activeSessionEntity);

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

        return StudentDashboardStatsDTO.builder()
                .overallAttendance(Math.round(attendanceRate * 10.0) / 10.0)
                .attendedClasses((int) verifiedCount)
                .totalClasses((int) totalCount)
                .attendanceTrend(attendanceTrend)
                .todayClasses(todayClasses)
                .activeSession(activeSession)
                .recentHallPass(recentHallPass)
                .departmentName(deptName)
                .sectionName(sectionName)
                .semester(student.getSemester())
                .registrationNumber(student.getRegistrationNumber())
                .aiVerificationConfidence(learningOptimizer.getStudentAccuracy(studentId))
                .build();
    }

    private Map<String, Double> calculateAttendanceTrend(UUID studentId, java.time.ZonedDateTime now) {
        Map<String, Double> trend = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.toLocalDate().minusDays(i);
            String dateStr = date.format(formatter);
            
            Instant startOfDay = date.atStartOfDay(IST).toInstant();
            Instant endOfDay = date.plusDays(1).atStartOfDay(IST).toInstant();
            
            // This is a simplified calculation for the trend
            // In a real system, you'd have a more optimized query
            trend.put(dateStr, 100.0); // Placeholder for now to avoid heavy queries in demo
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
