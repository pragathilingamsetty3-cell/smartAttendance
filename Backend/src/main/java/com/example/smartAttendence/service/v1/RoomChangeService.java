package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.QRRoomChangeRequest;
import com.example.smartAttendence.dto.v1.RoomChangeRequest;
import com.example.smartAttendence.dto.v1.WeeklyRoomSwapConfig;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.RoomChangeTransition;
import com.example.smartAttendence.entity.WeeklyRoomSwap;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.entity.CRLRAssignment;
import com.example.smartAttendence.repository.RoomChangeTransitionRepository;
import com.example.smartAttendence.repository.WeeklyRoomSwapRepository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.RoomRepository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.CRLRAssignmentRepository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.service.EmailService;
import com.example.smartAttendence.service.v1.NotificationService;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RoomChangeService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoomChangeService.class);
    
    private final RoomChangeTransitionRepository transitionRepository;
    private final WeeklyRoomSwapRepository weeklySwapRepository;
    private final ClassroomSessionV1Repository sessionRepository;
    private final UserV1Repository userRepository;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final CRLRAssignmentRepository crLrAssignmentRepository;
    private final TimetableRepository timetableRepository;
    private final SharedUtilityService sharedUtilityService;
    private static final int GRACE_PERIOD_MINUTES = 15;
    
    public RoomChangeService(
            RoomChangeTransitionRepository transitionRepository,
            WeeklyRoomSwapRepository weeklySwapRepository,
            ClassroomSessionV1Repository sessionRepository,
            UserV1Repository userRepository,
            RoomRepository roomRepository,
            SectionRepository sectionRepository,
            NotificationService notificationService,
            EmailService emailService,
            CRLRAssignmentRepository crLrAssignmentRepository,
            TimetableRepository timetableRepository,
            SharedUtilityService sharedUtilityService) {
        this.transitionRepository = transitionRepository;
        this.weeklySwapRepository = weeklySwapRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.sectionRepository = sectionRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.crLrAssignmentRepository = crLrAssignmentRepository;
        this.timetableRepository = timetableRepository;
        this.sharedUtilityService = sharedUtilityService;
    }
    
    /**
     * Handle QR-based room change (CR/LR/Faculty)
     */
    public RoomChangeTransition handleQRRoomChange(QRRoomChangeRequest request) {
        // Validate scanner user
        User scanner = userRepository.findById(request.scannerUserId())
            .orElseThrow(() -> new IllegalArgumentException("Scanner user not found"));
        
        // Validate room
        Room newRoom = roomRepository.findById(request.qrRoomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        
        // Determine section based on user role and validate authorization
        UUID sectionId = determineAndValidateSection(request, scanner);
        
        // Find current session for this section
        ClassroomSession session = findCurrentSessionForSection(sectionId);
        
        // Create transition record
        RoomChangeTransition transition = new RoomChangeTransition(
            session, scanner, session.getRoom().getId(), newRoom.getId(),
            RoomChangeTransition.RoomChangeType.SUDDEN_CHANGE, request.reason()
        );
        transition.setGracePeriodMinutes(15);
        
        // Store old room for logging
        Room oldRoom = session.getRoom();
        
        // Update session room and preserve exact admin-set boundary
        session.setRoom(newRoom);
        
        // Validate and set the admin-set exact boundary
        org.locationtech.jts.geom.Polygon adminBoundary = newRoom.getBoundaryPolygon();
        if (adminBoundary == null) {
            throw new IllegalStateException("New room has no boundary polygon set by admin: " + newRoom.getName());
        }
        
        if (!adminBoundary.isValid()) {
            throw new IllegalStateException("New room has invalid boundary polygon: " + newRoom.getName());
        }
        
        // Use exact copy of admin boundary
        session.setGeofencePolygon((org.locationtech.jts.geom.Polygon) adminBoundary.copy());
        
        // Log the boundary change for audit
        logger.info("Room change: Session {} moved from {} to {} with exact boundary ({} points)", 
                   session.getId(), oldRoom.getName(), newRoom.getName(), 
                   adminBoundary.getCoordinates().length - 1);
        
        // Save changes
        transition = transitionRepository.save(transition);
        sessionRepository.save(session);
        
        // Send notifications
        sendRoomChangeNotifications(transition, session, request);
        
        return transition;
    }
    
    /**
     * Handle pre-planned room change
     */
    public RoomChangeTransition handlePrePlannedRoomChange(RoomChangeRequest request, String email) {
        User requestedBy = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        // Validate and find session
        ClassroomSession session = findSessionBySectionAndTime(request.sectionId(), request.scheduledTime());
        
        // Validate new room
        Room newRoom = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new IllegalArgumentException("New room not found"));
        
        // Create transition record
        RoomChangeTransition transition = new RoomChangeTransition(
            session, requestedBy, session.getRoom().getId(), newRoom.getId(),
            RoomChangeTransition.RoomChangeType.PRE_PLANNED, request.reason()
        );
        transition.setTransitionStartTime(request.scheduledTime());
        transition.setTransitionEndTime(request.scheduledTime().plusSeconds(15 * 60L));
        
        // Update session room and preserve exact admin-set boundary
        session.setRoom(newRoom);
        
        // Validate and set the admin-set exact boundary
        org.locationtech.jts.geom.Polygon adminBoundary = newRoom.getBoundaryPolygon();
        if (adminBoundary == null) {
            throw new IllegalStateException("New room has no boundary polygon set by admin: " + newRoom.getName());
        }
        
        if (!adminBoundary.isValid()) {
            throw new IllegalStateException("New room has invalid boundary polygon: " + newRoom.getName());
        }
        
        // Use exact copy of admin boundary
        session.setGeofencePolygon((org.locationtech.jts.geom.Polygon) adminBoundary.copy());
        
        // Save changes
        transition = transitionRepository.save(transition);
        sessionRepository.save(session);
        
        // Schedule notifications
        schedulePrePlannedNotifications(transition, session, request);
        
        return transition;
    }
    
    /**
     * Handle weekly room swap
     */
    public List<RoomChangeTransition> handleWeeklyRoomSwap(UUID swapConfigId, String email) {
        User requestedBy = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        WeeklyRoomSwap swapConfig = weeklySwapRepository.findById(swapConfigId)
            .orElseThrow(() -> new IllegalArgumentException("Weekly swap config not found"));
        
        // Find current session for the timetable
        ClassroomSession session = findCurrentSessionForTimetable(swapConfig.getTimetable().getId());
        
        if (session == null) {
            throw new IllegalArgumentException("No active session found for timetable: " + swapConfig.getTimetable().getId());
        }
        
        // Create transition for room swap
        RoomChangeTransition transition = new RoomChangeTransition(
            session, requestedBy, swapConfig.getOriginalRoom().getId(), swapConfig.getNewRoom().getId(),
            RoomChangeTransition.RoomChangeType.WEEKLY_SWAP, swapConfig.getReason() != null ? swapConfig.getReason() : "Weekly room swap"
        );
        
        // Update session with new room and boundary
        session.setRoom(swapConfig.getNewRoom());
        org.locationtech.jts.geom.Polygon newBoundary = swapConfig.getNewRoom().getBoundaryPolygon();
        if (newBoundary == null || !newBoundary.isValid()) {
            throw new IllegalStateException("New room has invalid boundary polygon: " + swapConfig.getNewRoom().getName());
        }
        session.setGeofencePolygon((org.locationtech.jts.geom.Polygon) newBoundary.copy());
        
        // Save changes
        transition = transitionRepository.save(transition);
        sessionRepository.save(session);
        
        // Send notifications
        sendWeeklySwapNotifications(transition, swapConfig);
        
        return List.of(transition);
    }
    
    /**
     * Find current session for timetable
     */
    private ClassroomSession findCurrentSessionForTimetable(UUID timetableId) {
        Instant now = Instant.now();
        Instant startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        
        // Use existing method to find active session for today
        java.util.Optional<ClassroomSession> sessionOpt = sessionRepository.findByTimetableIdAndDateRange(timetableId, startOfDay, endOfDay);

        
        return sessionOpt
            .filter(session -> session.isActive() && session.getStartTime().isBefore(now) && session.getEndTime().isAfter(now))
            .orElse(null);
    }
    
    /**
     * Create weekly room swap configuration
     */
    public WeeklyRoomSwap createWeeklySwapConfig(WeeklyRoomSwapConfig config, String email) {
        User createdBy = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        // Validate entities
        Section section1 = sharedUtilityService.validateAndGetSection(config.section1Id());
        Section section2 = sharedUtilityService.validateAndGetSection(config.section2Id());
        Room room1 = roomRepository.findById(config.room1Id())
            .orElseThrow(() -> new IllegalArgumentException("Room 1 not found"));
        Room room2 = roomRepository.findById(config.room2Id())
            .orElseThrow(() -> new IllegalArgumentException("Room 2 not found"));
        
        // Find a timetable for these sections (simplified - create a basic timetable)
        java.time.LocalDate swapDate = config.nextSwapTime() != null ? 
            config.nextSwapTime().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : 
            java.time.LocalDate.now();
        
        // For simplicity, we'll create a basic timetable object
        // In a real implementation, you would find the actual timetable for this section
        com.example.smartAttendence.entity.Timetable timetable = new com.example.smartAttendence.entity.Timetable();
        timetable.setId(UUID.randomUUID()); // Temporary ID
        timetable.setSubject("Room Swap");
        timetable.setRoom(room1);
        timetable.setFaculty(createdBy);
        timetable.setDayOfWeek(java.time.DayOfWeek.from(swapDate));
        timetable.setStartTime(java.time.LocalTime.of(9, 0)); // Default 9 AM
        timetable.setEndTime(java.time.LocalTime.of(10, 0)); // Default 10 AM
        
        // Check for existing swaps
        if (weeklySwapRepository.existsByOriginalRoomIdAndNewRoomIdAndSwapDate(room1.getId(), room2.getId(), swapDate)) {
            throw new IllegalArgumentException("Weekly swap already exists for these rooms on this date");
        }
        
        // Create swap config
        WeeklyRoomSwap swap = new WeeklyRoomSwap();
        swap.setOriginalRoom(room1);
        swap.setNewRoom(room2);
        swap.setTimetable(timetable);
        swap.setSwapDate(swapDate);
        swap.setReason(config.description());
        swap.setApprovedBy(createdBy);
        swap.setApprovedAt(Instant.now());
        
        return weeklySwapRepository.save(swap);
    }
    
    /**
     * Check if student is in grace period for room transition
     */
    public boolean isStudentInGracePeriod(UUID studentId, UUID sessionId) {
        Instant now = Instant.now();
        List<RoomChangeTransition> activeTransitions = 
            transitionRepository.findActiveTransitionsForSession(sessionId, now);
        
        return !activeTransitions.isEmpty();
    }
    
    /**
     * Get active transitions for all sessions
     */
    public List<RoomChangeTransition> getAllActiveTransitions() {
        return transitionRepository.findAllActiveTransitions(Instant.now());
    }
    
    private UUID determineAndValidateSection(QRRoomChangeRequest request, User scanner) {
        // Faculty can change any room they're teaching
        if (scanner.getRole() == com.example.smartAttendence.enums.Role.FACULTY) {
            if (request.sectionId() == null) {
                // Find which class faculty is currently teaching
                return findFacultyCurrentSection(scanner);
            }
            return request.sectionId();
        }
        
        // CR and LR can change rooms for their assigned sections
        if (scanner.getRole() == com.example.smartAttendence.enums.Role.CR || scanner.getRole() == com.example.smartAttendence.enums.Role.LR) {
            // Get the section(s) this CR/LR is assigned to
            List<UUID> assignedSections = getAssignedSectionsForCR_LR(scanner);
            if (assignedSections.isEmpty()) {
                throw new IllegalArgumentException("CR/LR is not assigned to any section");
            }
            
            // If section is specified, validate it's one of their assigned sections
            if (request.sectionId() != null) {
                if (!assignedSections.contains(request.sectionId())) {
                    throw new IllegalArgumentException("CR/LR is not assigned to this section");
                }
                return request.sectionId();
            }
            
            // For single section assignment, return that section
            if (assignedSections.size() == 1) {
                return assignedSections.get(0);
            }
            
            // For multiple sections (combined classes), return the first active one
            return findActiveSectionForCR_LR(scanner, assignedSections);
        }
        
        throw new IllegalArgumentException("Unauthorized role for room change: " + scanner.getRole());
    }
    
    private UUID findFacultyCurrentSection(User faculty) {
        Instant now = Instant.now();
        DayOfWeek dayOfWeek = DayOfWeek.from(now.atZone(ZoneId.systemDefault()));
        LocalTime currentTime = LocalTime.from(now.atZone(ZoneId.systemDefault()));
        
        // Find current timetable entries for this faculty
        List<com.example.smartAttendence.entity.Timetable> currentTimetables = 
            timetableRepository.findCurrentSessionForFaculty(faculty.getId(), dayOfWeek, currentTime);
        
        if (currentTimetables.isEmpty()) {
            throw new IllegalArgumentException("Faculty is not teaching any class at this time");
        }
        
        // Handle combined classes - return the first section
        return currentTimetables.get(0).getSection().getId();
    }
    
    private List<UUID> getAssignedSectionsForCR_LR(User user) {
        List<CRLRAssignment> assignments = crLrAssignmentRepository.findByUserIdAndActiveTrue(user.getId());
        return assignments.stream()
                .map(assignment -> assignment.getSection().getId())
                .toList();
    }
    
    private UUID findActiveSectionForCR_LR(User user, List<UUID> assignedSections) {
        Instant now = Instant.now();
        DayOfWeek dayOfWeek = DayOfWeek.from(now.atZone(ZoneId.systemDefault()));
        LocalTime currentTime = LocalTime.from(now.atZone(ZoneId.systemDefault()));
        
        // Check which assigned section has an active class right now
        for (UUID sectionId : assignedSections) {
            List<com.example.smartAttendence.entity.Timetable> currentTimetables = 
                timetableRepository.findCurrentSessionForSection(sectionId, dayOfWeek, currentTime);
            
            if (!currentTimetables.isEmpty()) {
                return sectionId;
            }
        }
        
        // If no active class, return the first assigned section
        return assignedSections.get(0);
    }
    
    private ClassroomSession findCurrentSessionForSection(UUID sectionId) {
        Instant now = Instant.now();
        DayOfWeek dayOfWeek = DayOfWeek.from(now.atZone(ZoneId.systemDefault()));
        LocalTime currentTime = LocalTime.from(now.atZone(ZoneId.systemDefault()));
        
        // Find current timetable entry for this section
        List<com.example.smartAttendence.entity.Timetable> currentTimetables = 
            timetableRepository.findCurrentSessionForSection(sectionId, dayOfWeek, currentTime);
        
        if (currentTimetables.isEmpty()) {
            throw new IllegalArgumentException("No active session found for section at this time");
        }
        
        // Find the actual classroom session for the first timetable
        Instant startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        java.util.Optional<ClassroomSession> sessionOpt = sessionRepository.findByTimetableIdAndDateRange(
                currentTimetables.get(0).getId(), 
                startOfDay, 
                endOfDay
        );


        
        return sessionOpt
            .filter(session -> session.isActive())
            .orElseThrow(() -> new IllegalArgumentException("No active classroom session found for timetable"));
    }
    private ClassroomSession findSessionBySectionAndTime(UUID sectionId, Instant scheduledTime) {
        DayOfWeek dayOfWeek = DayOfWeek.from(scheduledTime.atZone(ZoneId.systemDefault()));
        LocalTime startTime = LocalTime.from(scheduledTime.atZone(ZoneId.systemDefault()));
        
        // Find timetable entry for this section at scheduled time
        List<com.example.smartAttendence.entity.Timetable> scheduledTimetables = 
            timetableRepository.findSessionForSectionAtTime(sectionId, dayOfWeek, startTime);
        
        if (scheduledTimetables.isEmpty()) {
            throw new IllegalArgumentException("No session scheduled for section at specified time");
        }
        
        com.example.smartAttendence.entity.Timetable timetable = scheduledTimetables.get(0);
        
        Instant startOfDay = scheduledTime.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = scheduledTime.atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Find or create corresponding classroom session
        return sessionRepository.findByTimetableIdAndDateRange(timetable.getId(), startOfDay, endOfDay)
            .orElseGet(() -> createClassroomSessionForTimetable(timetable, scheduledTime));

    }
    
    private ClassroomSession createClassroomSessionForTimetable(com.example.smartAttendence.entity.Timetable timetable, Instant scheduledTime) {
        ClassroomSession session = new ClassroomSession();
        session.setTimetable(timetable);
        session.setRoom(timetable.getRoom());
        session.setFaculty(timetable.getFaculty());
        session.setStartTime(scheduledTime);
        session.setEndTime(scheduledTime.plusSeconds(
            java.time.Duration.between(timetable.getStartTime(), timetable.getEndTime()).getSeconds()
        ));
        
        // Validate and set exact admin-set boundary
        org.locationtech.jts.geom.Polygon adminBoundary = timetable.getRoom().getBoundaryPolygon();
        if (adminBoundary == null) {
            throw new IllegalStateException("Room has no boundary polygon set by admin: " + timetable.getRoom().getName());
        }
        
        if (!adminBoundary.isValid()) {
            throw new IllegalStateException("Room has invalid boundary polygon: " + timetable.getRoom().getName());
        }
        
        // Use exact copy of admin boundary
        session.setGeofencePolygon((org.locationtech.jts.geom.Polygon) adminBoundary.copy());
        session.setActive(true);
        session.setAutoGenerated(true);
        
        return sessionRepository.save(session);
    }
    
    private void sendRoomChangeNotifications(RoomChangeTransition transition, ClassroomSession session, QRRoomChangeRequest request) {
        if (request.notifyStudents()) {
            notificationService.sendEmergencyRoomChangeNotification(
                session, transition.getOriginalRoomId(), transition.getNewRoomId(), transition.getReason()
            );
        }
        
        if (request.notifyFaculty()) {
            emailService.sendEmergencyChangeEmail(session, null);
        }
    }
    
    private void schedulePrePlannedNotifications(RoomChangeTransition transition, ClassroomSession session, RoomChangeRequest request) {
        // TODO: Implement scheduled notifications using a task scheduler
        // This would send notifications at appropriate times before the scheduled change
    }
    
    private void sendWeeklySwapNotifications(RoomChangeTransition transition, WeeklyRoomSwap swapConfig) {
        // Send notifications about the room swap
        notificationService.sendEmergencyRoomChangeNotification(
            transition.getSession(),
            swapConfig.getOriginalRoom().getId(),
            swapConfig.getNewRoom().getId(),
            swapConfig.getReason() != null ? swapConfig.getReason() : "Weekly room swap"
        );
        
        // Log the notification
        logger.info("Weekly room swap notification sent for swap on {} from {} to {}", 
            swapConfig.getSwapDate(), 
            swapConfig.getOriginalRoom().getName(), 
            swapConfig.getNewRoom().getName());
    }
}
