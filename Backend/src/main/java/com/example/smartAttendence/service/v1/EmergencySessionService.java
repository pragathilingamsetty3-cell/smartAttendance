package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.EmergencySessionChangeRequest;
import com.example.smartAttendence.dto.v1.SubstituteClaimRequest;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.EmergencySessionChange;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.EmergencySessionChangeRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.RoomRepository;
import com.example.smartAttendence.service.v1.NotificationService;
import com.example.smartAttendence.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Service
@Transactional
public class EmergencySessionService {
    
    private final EmergencySessionChangeRepository emergencyChangeRepository;
    private final ClassroomSessionV1Repository sessionRepository;
    private final UserV1Repository userRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    public EmergencySessionService(
            EmergencySessionChangeRepository emergencyChangeRepository,
            ClassroomSessionV1Repository sessionRepository,
            UserV1Repository userRepository,
            RoomRepository roomRepository,
            NotificationService notificationService,
            EmailService emailService) {
        this.emergencyChangeRepository = emergencyChangeRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }
    
    /**
     * Handle emergency session changes (faculty, room, time, cancellation)
     */
    public EmergencySessionChange handleEmergencyChange(EmergencySessionChangeRequest request, String email) {
        User changedBy = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        // Validate session exists
        ClassroomSession session = sessionRepository.findById(request.sessionId())
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.sessionId()));
        
        // Create emergency change record
        EmergencySessionChange change = new EmergencySessionChange();
        change.setSession(session);
        change.setChangedBy(changedBy);
        change.setChangeType(com.example.smartAttendence.entity.EmergencySessionChange.EmergencyChangeType.valueOf(request.changeType().name()));
        change.setReason(request.reason());
        change.setAdminNotes(request.adminNotes());
        change.setEmergencyOverride(true); // All emergency changes are overrides
        change.setNotifyStudents(request.notifyStudents() != null ? request.notifyStudents() : true);
        change.setNotifyParents(request.notifyParents() != null ? request.notifyParents() : false);
        
        // Store original values
        change.setOriginalFacultyId(session.getFaculty().getId());
        change.setOriginalRoomId(session.getRoom().getId());
        change.setOriginalStartTime(session.getStartTime());
        change.setOriginalEndTime(session.getEndTime());
        
        // Apply changes based on type
        switch (request.changeType()) {
            case FACULTY_SUBSTITUTION -> handleFacultySubstitution(session, request, change);
            case ROOM_CHANGE -> handleRoomChange(session, request, change);
            case TIME_CHANGE -> handleTimeChange(session, request, change);
            case CANCELLATION -> handleCancellation(session, change);
            default -> throw new IllegalArgumentException("Unsupported change type: " + request.changeType());
        }
        
        // Save change record
        change = emergencyChangeRepository.save(change);
        
        // Update session
        sessionRepository.save(session);
        
        // Send notifications
        sendEmergencyNotifications(change, session);
        
        return change;
    }
    
    /**
     * Handle substitute faculty claim protocol
     */
    public EmergencySessionClaim handleSubstituteClaim(SubstituteClaimRequest request, String email) {
        User substituteFaculty = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        ClassroomSession session = sessionRepository.findById(request.sessionId())
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.sessionId()));
        
        // Validate substitute faculty
        if (substituteFaculty.getRole() != com.example.smartAttendence.enums.Role.FACULTY && 
            substituteFaculty.getRole() != com.example.smartAttendence.enums.Role.ADMIN) {
            throw new IllegalArgumentException("Only faculty or admin can claim sessions");
        }
        
        // Create emergency change record
        EmergencySessionChange change = new EmergencySessionChange();
        change.setSession(session);
        change.setChangedBy(substituteFaculty);
        change.setChangeType(EmergencySessionChange.EmergencyChangeType.FACULTY_SUBSTITUTION);
        change.setReason(request.substitutionReason());
        change.setEmergencyOverride(request.emergencyOverride() != null ? request.emergencyOverride() : false);
        change.setNotifyStudents(request.notifyStudents() != null ? request.notifyStudents() : true);
        change.setNotifyParents(request.notifyDepartment() != null ? request.notifyDepartment() : false);
        
        // Store original faculty
        change.setOriginalFacultyId(session.getFaculty().getId());
        change.setNewFacultyId(request.substituteFacultyId());
        
        // Update session faculty
        User newFaculty = userRepository.findById(request.substituteFacultyId())
            .orElseThrow(() -> new IllegalArgumentException("Substitute faculty not found"));
        session.setFaculty(newFaculty);
        
        // Handle room change if specified
        if (request.newRoomId() != null) {
            change.setOriginalRoomId(session.getRoom().getId());
            change.setNewRoomId(request.newRoomId());
            
            Room newRoom = roomRepository.findById(request.newRoomId())
                .orElseThrow(() -> new IllegalArgumentException("New room not found"));
            session.setRoom(newRoom);
        }
        
        // Save changes
        change = emergencyChangeRepository.save(change);
        sessionRepository.save(session);
        
        // Send notifications
        sendSubstituteNotifications(change, session, request);
        
        return new EmergencySessionClaim(change, session);
    }
    
    /**
     * Get emergency changes for a session
     */
    public List<EmergencySessionChange> getSessionChanges(UUID sessionId) {
        return emergencyChangeRepository.findBySessionIdOrderByChangeTimestampDesc(sessionId);
    }
    
    /**
     * Get recent emergency overrides
     */
    public List<EmergencySessionChange> getRecentEmergencyOverrides(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return emergencyChangeRepository.findEmergencyOverridesSince(since);
    }
    
    private void handleFacultySubstitution(ClassroomSession session, EmergencySessionChangeRequest request, EmergencySessionChange change) {
        User newFaculty = userRepository.findById(request.newFacultyId())
            .orElseThrow(() -> new IllegalArgumentException("New faculty not found"));
        
        change.setNewFacultyId(request.newFacultyId());
        session.setFaculty(newFaculty);
    }
    
    private void handleRoomChange(ClassroomSession session, EmergencySessionChangeRequest request, EmergencySessionChange change) {
        Room newRoom = roomRepository.findById(request.newRoomId())
            .orElseThrow(() -> new IllegalArgumentException("New room not found"));
        
        change.setNewRoomId(request.newRoomId());
        session.setRoom(newRoom);
        
        // Update geofence to new room location
        session.setGeofencePolygon(newRoom.getBoundaryPolygon());
    }
    
    private void handleTimeChange(ClassroomSession session, EmergencySessionChangeRequest request, EmergencySessionChange change) {
        if (request.newStartTime() != null) {
            change.setNewStartTime(request.newStartTime());
            session.setStartTime(request.newStartTime());
        }
        
        if (request.newEndTime() != null) {
            change.setNewEndTime(request.newEndTime());
            session.setEndTime(request.newEndTime());
        }
    }
    
    private void handleCancellation(ClassroomSession session, EmergencySessionChange change) {
        session.setActive(false);
    }
    
    private void sendEmergencyNotifications(EmergencySessionChange change, ClassroomSession session) {
        if (change.getNotifyStudents()) {
            // Send push notifications to all enrolled students
            notificationService.sendEmergencyChangeNotification(session, change);
        }
        
        if (change.getNotifyParents()) {
            // Send email notifications to parents
            emailService.sendEmergencyChangeEmail(session, change);
        }
    }
    
    private void sendSubstituteNotifications(EmergencySessionChange change, ClassroomSession session, SubstituteClaimRequest request) {
        if (request.notifyStudents() != null && request.notifyStudents()) {
            notificationService.sendSubstituteNotification(session, change);
        }
        
        if (request.notifyDepartment() != null && request.notifyDepartment()) {
            emailService.sendSubstituteNotification(session, change);
        }
    }
    
    public record EmergencySessionClaim(
        EmergencySessionChange change,
        ClassroomSession updatedSession
    ) {}
}
