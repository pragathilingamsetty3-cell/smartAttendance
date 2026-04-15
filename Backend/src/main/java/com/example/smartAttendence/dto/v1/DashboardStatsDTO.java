package com.example.smartAttendence.dto.v1;

/**
 * 📊 DASHBOARD STATISTICS DTO
 * Matches the frontend requirements for the system overview dashboard.
 */
public record DashboardStatsDTO(
    long totalUsers,
    long totalStudents,
    long activeSessions,
    long activeToday,
    long anomalies,
    double attendanceRate,
    long totalScheduledToday,
    long verifiedCount
) {}
