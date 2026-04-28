package com.example.smartAttendence.dto.v1;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🎓 STUDENT DASHBOARD STATISTICS DTO
 * Optimized for the Student Dashboard frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardStatsDTO {
    private double overallAttendance;
    private int attendedClasses;
    private int totalClasses;
    
    // Day-wise attendance trend (e.g., "2024-04-01" -> 100.0)
    private Map<String, Double> attendanceTrend;
    
    // Today's classes
    private List<TimetableResponseDTO> todayClasses;
    
    // Current active session (if student is currently in a class)
    private TimetableResponseDTO activeSession;
    
    // Whether student has already marked attendance for the active session
    private Boolean attendanceMarked;
    
    // Recent hall pass status
    private HallPassStatusDTO recentHallPass;
    
    // Profile summary
    private String departmentName;
    private String sectionName;
    private Integer semester;
    private String registrationNumber;
    private UUID sectionId;
    
    // AI Insights for Student
    private double aiVerificationConfidence; // 0.0 - 1.0 (Accuracy Score)
    
    // 🔍 Diagnostics (Optional, for debugging)
    private Map<String, Object> debugInfo;
}
