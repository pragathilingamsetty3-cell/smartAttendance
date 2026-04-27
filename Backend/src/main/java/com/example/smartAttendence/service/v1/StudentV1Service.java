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

    /**
     * Get student dashboard statistics - CACHED for high speed
     */
    
    public StudentDashboardStatsDTO getStudentDashboardStats(UUID studentId) {
        User student = userV1Repository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(IST);
        
        // 1. Calculate Attendance Metrics (Overall)
        List<AttendanceRecord> allRecords = attendanceRecordRepository.findByStudentIdAndRecordedAtAfter(studentId, Instant.EPOCH);
        
        long attendedCount = allRecords.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .count();
        
        double overallAttendance = allRecords.isEmpty() ? 0.0 : (double) attendedCount * 100.0 / allRecords.size();
        
        // 2. Fetch Today's Classes (IST)
        DayOfWeek today = now.getDayOfWeek();
        List<Timetable> todayTimetable = new ArrayList<>();
        
        log.info("🔍 DASHBOARD DEBUG: Fetching classes for Student: {} (Reg: {}), Section: {}, Day: {}", 
                student.getName(), student.getRegistrationNumber(), 
                student.getSection() != null ? student.getSection().getName() : "NULL", 
                today);

        if (student.getSection() != null || student.getSectionId() != null) {
            UUID sectionId = student.getSection() != null ? student.getSection().getId() : student.getSectionId();
            java.time.LocalDate todayDate = now.toLocalDate();
            
            log.info("🔍 DASHBOARD DEBUG: Searching Timetable for Section ID: {} on Day: {}", sectionId, today);
            
            todayTimetable = timetableRepository.findBySectionAndDayOfWeek(sectionId, today)
                    .stream()
                    .filter(t -> (t.getStartDate() == null || !todayDate.isBefore(t.getStartDate())) && 
                                 (t.getEndDate() == null || !todayDate.isAfter(t.getEndDate())))
                    .collect(Collectors.toList());
            
            log.info("🔍 DASHBOARD DEBUG: Found {} classes for today in database.", todayTimetable.size());
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

        // 6. AI Insights for Student
        double avgConfidence = allRecords.stream()
                .filter(r -> r.getConfidence() != null)
                .mapToDouble(AttendanceRecord::getConfidence)
                .average()
                .orElse(92.0); // Baseline confidence for new students

        return StudentDashboardStatsDTO.builder()
                .overallAttendance(Math.round(overallAttendance * 10.0) / 10.0)
                .attendedClasses((int) attendedCount)
                .totalClasses(allRecords.size())
                .attendanceTrend(attendanceTrend)
                .todayClasses(todayClasses)
                .activeSession(activeSession)
                .recentHallPass(recentHallPass)
                .departmentName(deptName)
                .sectionName(sectionName)
                .semester(student.getSemester())
                .registrationNumber(student.getRegistrationNumber())
                .aiVerificationConfidence(Math.round(avgConfidence * 10.0) / 10.0)
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
