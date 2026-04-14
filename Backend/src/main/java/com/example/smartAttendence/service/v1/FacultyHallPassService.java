package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.dto.v1.HallPassApprovalRequest;
import com.example.smartAttendence.dto.v1.HallPassDenialRequest;
import com.example.smartAttendence.dto.v1.HallPassStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FacultyHallPassService {

    // In-memory storage for demo - in production, this would be a database table
    private final List<HallPassStatusDTO> hallPassRequests = new ArrayList<>();

    public FacultyHallPassService() {
        seedInitialData();
    }

    private void seedInitialData() {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        // Add a pending request
        hallPassRequests.add(new HallPassStatusDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            sessionId,
            "Aravind Kumar",
            "Medical Emergency",
            15,
            "PENDING",
            Instant.now().minusSeconds(300),
            null,
            null,
            "Urgent washroom visit"
        ));

        // Add a history item (Approved)
        hallPassRequests.add(new HallPassStatusDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            sessionId,
            "Sneha Reddy",
            "Water break",
            10,
            "APPROVED",
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(3540),
            "FACULTY",
            "Approved for 10 mins"
        ));

        // Add another history item (Denied)
        hallPassRequests.add(new HallPassStatusDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            sessionId,
            "Rohan Sharma",
            "Personal work",
            20,
            "DENIED",
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(7140),
            "FACULTY",
            "Important lecture in progress"
        ));
    }

    /**
     * Student creates hall pass request
     */
    public void createHallPassRequest(HallPassRequestDTO request) {
        HallPassStatusDTO newRequest = new HallPassStatusDTO(
            UUID.randomUUID(), // requestId
            request.studentId(),
            request.sessionId(),
            "Student Name", // Would fetch from user service
            request.reason(),
            request.requestedMinutes(),
            "PENDING",
            Instant.now(),
            null, // processedAt
            null, // processedBy
            request.studentNotes()
        );
        
        hallPassRequests.add(newRequest);
    }

    /**
     * Faculty approves hall pass request
     */
    public void approveHallPass(HallPassApprovalRequest request) {
        // Find the pending request
        HallPassStatusDTO pendingRequest = hallPassRequests.stream()
                .filter(hr -> hr.studentId().equals(request.studentId()) && 
                               hr.sessionId().equals(request.sessionId()) && 
                               "PENDING".equals(hr.status()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending hall pass request found"));

        // Update the request
        HallPassStatusDTO approvedRequest = new HallPassStatusDTO(
            pendingRequest.requestId(),
            request.studentId(),
            request.sessionId(),
            pendingRequest.studentName(),
            pendingRequest.reason(),
            request.approvedMinutes(),
            "APPROVED",
            Instant.now(),
            Instant.now(),
            "FACULTY",
            request.facultyNotes()
        );
        
        // Update in list (in production, this would be database update)
        hallPassRequests.remove(pendingRequest);
        hallPassRequests.add(approvedRequest);
    }

    /**
     * Faculty denies hall pass request
     */
    public void denyHallPass(HallPassDenialRequest request) {
        // Find the pending request
        HallPassStatusDTO pendingRequest = hallPassRequests.stream()
                .filter(hr -> hr.studentId().equals(request.studentId()) && 
                               hr.sessionId().equals(request.sessionId()) && 
                               "PENDING".equals(hr.status()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending hall pass request found"));

        // Update the request
        HallPassStatusDTO deniedRequest = new HallPassStatusDTO(
            pendingRequest.requestId(),
            request.studentId(),
            request.sessionId(),
            pendingRequest.studentName(),
            pendingRequest.reason(),
            0, // no approved minutes
            "DENIED",
            Instant.now(),
            Instant.now(),
            "FACULTY",
            request.facultyNotes()
        );
        
        // Update in list (in production, this would be database update)
        hallPassRequests.remove(pendingRequest);
        hallPassRequests.add(deniedRequest);
    }

    /**
     * Get pending hall pass requests for faculty
     */
    public List<HallPassStatusDTO> getPendingHallPassRequests() {
        return hallPassRequests.stream()
                .filter(hr -> "PENDING".equals(hr.status()))
                .toList();
    }

    /**
     * Get hall pass history for a session
     */
    public List<HallPassStatusDTO> getHallPassHistory(UUID sessionId) {
        return hallPassRequests.stream()
                .filter(hr -> hr.sessionId().equals(sessionId))
                .filter(hr -> !"PENDING".equals(hr.status()))
                .toList();
    }

    /**
     * Get latest hall pass request for a student (regardless of status)
     */
    public HallPassStatusDTO getLatestHallPassForStudent(UUID studentId) {
        return hallPassRequests.stream()
                .filter(hr -> hr.studentId().equals(studentId))
                .sorted((a, b) -> b.requestedAt().compareTo(a.requestedAt()))
                .findFirst()
                .orElse(null);
    }
}
