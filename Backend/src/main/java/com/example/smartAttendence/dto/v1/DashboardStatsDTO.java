package com.example.smartAttendence.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 📊 DASHBOARD STATISTICS DTO
 * Matches the frontend requirements for the system overview dashboard.
 */
@Schema(description = "System-wide statistics for the admin dashboard overview")
public record DashboardStatsDTO(
    @Schema(description = "Total number of users (Admins, Faculty, Students)", example = "1500")
    long totalUsers,

    @Schema(description = "Total number of student entities", example = "1200")
    long totalStudents,

    @Schema(description = "Number of classroom sessions currently active", example = "42")
    long activeSessions,

    @Schema(description = "Number of classroom sessions conducted today", example = "85")
    long activeToday,

    @Schema(description = "Count of high-priority security anomalies or walk-out violations detected", example = "3")
    long anomalies,

    @Schema(description = "Average system-wide attendance rate (0-100)", example = "94.5")
    double attendanceRate,

    @Schema(description = "Total number of classroom sessions scheduled or started today", example = "101")
    long totalScheduledToday,

    @Schema(description = "Number of successfully verified biometric/geofenced attendance markers", example = "1050")
    long verifiedCount
) {}
