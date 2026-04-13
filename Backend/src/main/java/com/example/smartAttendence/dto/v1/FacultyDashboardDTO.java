package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record FacultyDashboardDTO(
    double attendanceRate,
    int activeSessions,
    int pendingHallPasses,
    CurrentSessionDTO currentSession
) {
    public record CurrentSessionDTO(
        UUID sessionId,
        String courseName,
        String roomName,
        int presentCount,
        int totalStudents,
        int anomalies,
        int outOfRoom
    ) {}
}
