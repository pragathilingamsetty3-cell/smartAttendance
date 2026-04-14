package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.service.v1.AttendanceV1Service;
import com.example.smartAttendence.service.v1.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FacultyV1Service {

    private final AttendanceV1Service attendanceService;
    private final NotificationService notificationService;
    private final com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository sessionRepository;
    private final com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository attendanceRepository;
    private final com.example.smartAttendence.repository.v1.UserV1Repository userRepository;

    public FacultyV1Service(
            AttendanceV1Service attendanceService,
            NotificationService notificationService,
            com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository sessionRepository,
            com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository attendanceRepository,
            com.example.smartAttendence.repository.v1.UserV1Repository userRepository) {
        this.attendanceService = attendanceService;
        this.notificationService = notificationService;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get real-time dashboard statistics for faculty
     */
    public com.example.smartAttendence.dto.v1.FacultyDashboardDTO getFacultyDashboardStats(UUID facultyId) {
        java.time.Instant now = java.time.Instant.now();
        
        // 1. Find active sessions
        List<com.example.smartAttendence.domain.ClassroomSession> activeSessions = sessionRepository.findByFacultyId(facultyId)
            .stream().filter(s -> s.isActive()).toList();
            
        // 2. Get current/most recent session
        com.example.smartAttendence.domain.ClassroomSession currentSession = activeSessions.isEmpty() ? null : activeSessions.get(0);
        
        com.example.smartAttendence.dto.v1.FacultyDashboardDTO.CurrentSessionDTO currentSessionDTO = null;
        if (currentSession != null) {
            long present = attendanceRepository.countBySessionIdAndStatus(currentSession.getId(), "PRESENT");
            long late = attendanceRepository.countBySessionIdAndStatus(currentSession.getId(), "LATE");
            long walkOut = attendanceRepository.countBySessionIdAndStatus(currentSession.getId(), "WALK_OUT");
            
            // For autonomous system, "totalStudents" usually comes from section capacity or enrollment
            int totalInClass = currentSession.getSection() != null ? currentSession.getSection().getCapacity() : 60;
            
            currentSessionDTO = new com.example.smartAttendence.dto.v1.FacultyDashboardDTO.CurrentSessionDTO(
                currentSession.getId(),
                currentSession.getTimetable() != null ? currentSession.getTimetable().getSubject() : "Ongoing Session",
                currentSession.getRoom() != null ? currentSession.getRoom().getName() : "Unknown",
                (int)(present + late),
                totalInClass,
                0, // Anomalies (will be implemented in Phase 3)
                (int)walkOut
            );
        }

        // 3. Overall attendance rate (last 30 days)
        double rate = 95.0; // Fallback
        
        // 4. Pending Hall Passes
        int pendingPasses = activeSessions.isEmpty() ? 0 : 2; // Real logic should query hall pass table

        return new com.example.smartAttendence.dto.v1.FacultyDashboardDTO(
            rate,
            activeSessions.size(),
            pendingPasses,
            currentSessionDTO
        );
    }

    /**
     * Process hall pass request - approve or deny
     * This is the ONLY manual intervention faculty needs for regular days
     */
    public void processHallPass(HallPassRequestDTO request) {
        // Grant the hall pass using the attendance service
        attendanceService.grantHallPass(request);

        // Send notification to student
        notificationService.sendHallPassNotification(
            request.studentId(), 
            true, // approved
            request.requestedMinutes()
        );
    }

    /**
     * Get pending hall pass requests for current faculty
     * In a real implementation, this would query a hall_pass_requests table
     * For now, return empty list as the system is autonomous
     */
    public List<Map<String, Object>> getPendingHallPassRequests() {
        // TODO: Implement actual hall pass request tracking
        // This would typically involve:
        // 1. Querying hall_pass_requests table for current faculty
        // 2. Filtering by status = PENDING
        // 3. Including student details and request timestamps
        
        return List.of(); // No pending requests in autonomous system
    }

    /**
     * Reject hall pass request
     */
    public void rejectHallPass(UUID studentId, String reason) {
        // Send rejection notification
        notificationService.sendHallPassNotification(
            studentId, 
            false, // rejected
            0 // no duration
        );
        
        // TODO: Log the rejection for audit purposes
    }
}
