package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.domain.UserStatus;
import com.example.smartAttendence.dto.v1.OnboardingResponseDTO;
import com.example.smartAttendence.dto.v1.RoomCreationRequest;
import com.example.smartAttendence.dto.v1.StudentOnboardingRequest;
import com.example.smartAttendence.dto.v1.FacultyOnboardingRequest;
import com.example.smartAttendence.dto.v1.AdminOnboardingRequest;
import com.example.smartAttendence.dto.v1.DropdownDTO;
import com.example.smartAttendence.dto.v1.DepartmentCreateRequest;
import com.example.smartAttendence.dto.v1.SectionCreateRequest;
import com.example.smartAttendence.dto.v1.BulkPromotionRequest;
import com.example.smartAttendence.dto.v1.UpdateUserStatusRequest;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.entity.Department;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.entity.DeviceBinding;
import com.example.smartAttendence.repository.DeviceBindingRepository;
import com.example.smartAttendence.repository.RoomRepository;
import com.example.smartAttendence.repository.DepartmentRepository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.RefreshTokenV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.AcademicCalendarV1Repository;
import com.example.smartAttendence.service.EmailService;
import com.example.smartAttendence.service.RefreshTokenService;
import com.example.smartAttendence.service.v1.NotificationService;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import com.example.smartAttendence.util.SecurityUtils;
import com.example.smartAttendence.util.PasswordUtils;
import org.locationtech.jts.geom.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import com.example.smartAttendence.dto.v1.UserUpdateRequest;
import com.example.smartAttendence.dto.v1.UserActivityDTO;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import com.example.smartAttendence.dto.v1.CoordinateDTO;

@Service
@Transactional
@Slf4j
public class AdminV1Service {

    private final UserV1Repository userV1Repository;
    private final RoomRepository roomRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final TimetableRepository timetableRepository;
    private final DeviceBindingRepository deviceBindingRepository;
    private final RefreshTokenV1Repository refreshTokenV1Repository;
    private final AttendanceRecordV1Repository attendanceRecordV1Repository;
    private final ClassroomSessionV1Repository classroomSessionV1Repository;
    private final AcademicCalendarV1Repository calendarRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;
    private final SharedUtilityService sharedUtilityService;
    private final RoleConsistencyService roleConsistencyService;
    private final SecurityUtils securityUtils;

    public AdminV1Service(UserV1Repository userV1Repository, 
                         RoomRepository roomRepository,
                         DepartmentRepository departmentRepository,
                         SectionRepository sectionRepository,
                         TimetableRepository timetableRepository,
                         DeviceBindingRepository deviceBindingRepository,
                         RefreshTokenV1Repository refreshTokenV1Repository,
                         AttendanceRecordV1Repository attendanceRecordV1Repository,
                         ClassroomSessionV1Repository classroomSessionV1Repository,
                         AcademicCalendarV1Repository calendarRepository,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService,
                         RefreshTokenService refreshTokenService,
                         NotificationService notificationService,
                         SharedUtilityService sharedUtilityService,
                         RoleConsistencyService roleConsistencyService,
                         SecurityUtils securityUtils) {
        this.userV1Repository = userV1Repository;
        this.roomRepository = roomRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.timetableRepository = timetableRepository;
        this.deviceBindingRepository = deviceBindingRepository;
        this.refreshTokenV1Repository = refreshTokenV1Repository;
        this.attendanceRecordV1Repository = attendanceRecordV1Repository;
        this.classroomSessionV1Repository = classroomSessionV1Repository;
        this.calendarRepository = calendarRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.notificationService = notificationService;
        this.sharedUtilityService = sharedUtilityService;
        this.roleConsistencyService = roleConsistencyService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get user details by email
     */
    public User getUserByEmail(String email) {
        return userV1Repository.findByEmail(email).orElse(null);
    }

    /**
     * 1. Student Onboarding
     * Onboards a new student with temporary password
     */
    public OnboardingResponseDTO onboardStudent(StudentOnboardingRequest request) {
        // Validate email uniqueness
        if (userV1Repository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        // Validate registration number uniqueness
        if (userV1Repository.findByRegistrationNumber(request.registrationNumber()).isPresent()) {
            throw new IllegalArgumentException("Registration number already exists: " + request.registrationNumber());
        }

        // 🔐 DLAC ENFORCEMENT for normal Admins: Ensure department is set correctly before validation
        String targetDept = Optional.ofNullable(securityUtils.resolveDepartmentUuid(request.department()))
                .map(UUID::toString)
                .orElse(request.department());
        
        if (securityUtils.isAdmin()) {
            targetDept = securityUtils.getCurrentUserDepartmentId()
                    .map(UUID::toString)
                    .orElse(targetDept);
        }

        // Role-specific validation for students
        if (targetDept == null || targetDept.isBlank()) {
            throw new IllegalArgumentException("Department is required for student creation");
        }
        if (request.sectionId() == null || request.sectionId().isBlank()) {
            throw new IllegalArgumentException("Section ID is required for student creation");
        }

        UUID sectionUuid;
        try {
            sectionUuid = UUID.fromString(request.sectionId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Section ID format: " + request.sectionId());
        }

        // Generate secure temporary password
        String temporaryPassword = PasswordUtils.generateSecurePassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // Create student user
        User student = new User();
        student.setName(request.name());
        student.setEmail(request.email());
        student.setRegistrationNumber(request.registrationNumber());
        student.setPassword(encodedPassword);
        student.setRole(com.example.smartAttendence.enums.Role.STUDENT);
        student.setFirstLogin(true);
        student.setIsTemporaryPassword(true);
        
        // SECURITY: Explicitly initialize hardware binding fields as null during onboarding
        student.setDeviceId(null);
        student.setDeviceFingerprint(null);
        student.setBiometricSignature(null);
        student.setDeviceRegisteredAt(null);

        student.setDepartment(targetDept); 
        
        student.setSectionId(sectionUuid); // Map sectionId from request
        
        // CRITICAL FIX: Fetch and associate actual Section entity to prevent foreign key violation
        Section section = sectionRepository.findById(sectionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with ID: " + sectionUuid));
        student.setSection(section); // Set the actual Section entity
        student.setTotalAcademicYears(request.totalAcademicYears()); // Set total academic years
        student.setSemester(request.semester() != null ? request.semester() : 1); // Set semester with default
        student.setStatus(com.example.smartAttendence.domain.UserStatus.ACTIVE);
        if (request.parentEmail() != null && !request.parentEmail().isBlank()) {
            student.setParentEmail(request.parentEmail());
        }
        if (request.parentMobile() != null && !request.parentMobile().isBlank()) {
            student.setParentMobile(request.parentMobile());
        }
        if (request.studentMobile() != null && !request.studentMobile().isBlank()) {
            student.setStudentMobile(request.studentMobile());
        }

        // Save student
        student = userV1Repository.save(student);

        // Send welcome email asynchronously
        notificationService.sendWelcomeNotification(
            request.email(),
            request.name(),
            temporaryPassword
        );

        // Create response with temporary password
        return new OnboardingResponseDTO(
            student.getId(),
            student.getName(),
            student.getEmail(),
            student.getRegistrationNumber(),
            student.getRole().toString(),
            temporaryPassword
        );
    }

    /**
     * 2. Faculty Onboarding
     * Onboards a new faculty member with temporary password
     */
    public OnboardingResponseDTO onboardFaculty(FacultyOnboardingRequest request) {
        // Validate email uniqueness
        if (userV1Repository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        // Handle registration number
        String registrationNumber = request.registrationNumber();
        if (registrationNumber == null || registrationNumber.isBlank()) {
            registrationNumber = "FAC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        // Validate registration number uniqueness
        if (userV1Repository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new IllegalArgumentException("Registration number already exists: " + registrationNumber);
        }

        // 🔐 DLAC ENFORCEMENT for normal Admins: Ensure department is set correctly before validation
        String targetDept = Optional.ofNullable(securityUtils.resolveDepartmentUuid(request.department()))
                .map(UUID::toString)
                .orElse(request.department());
        
        if (securityUtils.isAdmin()) {
            targetDept = securityUtils.getCurrentUserDepartmentId()
                    .map(UUID::toString)
                    .orElse(targetDept);
        }

        // Role-specific validation for faculty
        if (targetDept == null || targetDept.isBlank()) {
            throw new IllegalArgumentException("Department is required for faculty creation");
        }

        // Generate secure temporary password
        String temporaryPassword = PasswordUtils.generateSecurePassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // Create faculty user
        User faculty = new User();
        faculty.setName(request.name());
        faculty.setEmail(request.email());
        faculty.setRegistrationNumber(registrationNumber);
        faculty.setPassword(encodedPassword);
        faculty.setRole(com.example.smartAttendence.enums.Role.FACULTY);
        faculty.setFirstLogin(true);
        faculty.setIsTemporaryPassword(true);
        faculty.setDeviceRegisteredAt(Instant.now());

        faculty.setDepartment(targetDept); 

        
        // Optional faculty mobile
        if (request.facultyMobile() != null && !request.facultyMobile().isBlank()) {
            faculty.setStudentMobile(request.facultyMobile());
        }

        // Save faculty
        faculty = userV1Repository.save(faculty);

        // Send welcome email asynchronously
        notificationService.sendWelcomeNotification(
            request.email(),
            request.name(),
            temporaryPassword
        );

        // Create response with temporary password
        return new OnboardingResponseDTO(
            faculty.getId(),
            faculty.getName(),
            faculty.getEmail(),
            faculty.getRegistrationNumber(),
            faculty.getRole().toString(),
            temporaryPassword
        );
    }

    /**
     * 3. Admin Onboarding
     * Onboards a new admin user with temporary password
     */
    public OnboardingResponseDTO onboardAdmin(AdminOnboardingRequest request) {
        // Validate email uniqueness
        if (userV1Repository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        // Handle registration number
        String registrationNumber = request.registrationNumber();
        if (registrationNumber == null || registrationNumber.isBlank()) {
            registrationNumber = "ADM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        // Validate registration number uniqueness
        if (userV1Repository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new IllegalArgumentException("Registration number already exists: " + registrationNumber);
        }

        // Validate role
        com.example.smartAttendence.enums.Role roleEnum;
        String roleStr = request.role().trim().toUpperCase();
        if (roleStr.equals("ADMIN")) {
            roleEnum = com.example.smartAttendence.enums.Role.ADMIN;
        } else if (roleStr.equals("SUPER_ADMIN")) {
            roleEnum = com.example.smartAttendence.enums.Role.SUPER_ADMIN;
        } else {
            throw new IllegalArgumentException("Role must be either ADMIN or SUPER_ADMIN");
        }

        // Generate secure temporary password
        String temporaryPassword = PasswordUtils.generateSecurePassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // Create admin user with proper role validation
        User admin = new User();
        admin.setName(request.name());
        admin.setEmail(request.email());
        admin.setRegistrationNumber(registrationNumber);
        admin.setPassword(encodedPassword);
        admin.setRole(roleEnum);
        admin.setFirstLogin(true);
        admin.setIsTemporaryPassword(true);
        admin.setDeviceRegisteredAt(Instant.now());

        // 🔐 DLAC ENFORCEMENT for normal Admins
        String targetDept = Optional.ofNullable(securityUtils.resolveDepartmentUuid(request.department()))
                .map(UUID::toString)
                .orElse(request.department());
        
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null) {
                targetDept = adminDeptId.toString();
            }
        }
        
        admin.setDepartment(targetDept);

        // 🔐 VALIDATE ROLE CONSISTENCY BEFORE SAVING
        admin = roleConsistencyService.validateAdminCreation(admin, request.role());

        // Save admin
        admin = userV1Repository.save(admin);

        // Send welcome email asynchronously
        notificationService.sendWelcomeNotification(
            request.email(),
            request.name(),
            temporaryPassword
        );

        // Create response with temporary password
        return new OnboardingResponseDTO(
            admin.getId(),
            admin.getName(),
            admin.getEmail(),
            admin.getRegistrationNumber(),
            admin.getRole().toString(),
            temporaryPassword
        );
    }

    /**
     * 4. Device & Biometric Reset
     * Resets deviceId and biometricPublicKey for a student by registration number
     */
    public User resetStudentDevice(String registrationNumber) {
        Optional<User> studentOpt = userV1Repository.findByRegistrationNumber(registrationNumber);
        if (studentOpt.isEmpty()) {
            throw new IllegalArgumentException("Student not found with registration number: " + registrationNumber);
        }

        User student = studentOpt.get();
        
        // Check if user has a student-related role (STUDENT, CR, or LR)
        com.example.smartAttendence.enums.Role role = student.getRole();
        if (role != com.example.smartAttendence.enums.Role.STUDENT && 
            role != com.example.smartAttendence.enums.Role.CR && 
            role != com.example.smartAttendence.enums.Role.LR) {
            throw new IllegalArgumentException("User is not in a student-related role (Role: " + role + ")");
        }

        // Reset device and biometric credentials
        String oldDeviceId = student.getDeviceId();
        student.setDeviceId(null);
        student.setDeviceFingerprint(null);
        student.setBiometricSignature(null);
        student.setDeviceRegisteredAt(null);
        student.setIsTemporaryPassword(true);
        student.setFirstLogin(true);
        
        // Also deactivate ALL records in device binding table if they exist
        List<DeviceBinding> bindings = deviceBindingRepository.findAllByUser(student);
        if (!bindings.isEmpty()) {
            for (DeviceBinding deviceBinding : bindings) {
                if (deviceBinding.getIsActive()) {
                    deviceBinding.setIsActive(false);
                    deviceBinding.setRevokedAt(java.time.Instant.now());
                    deviceBinding.setRevocationReason("Admin reset device lock");
                    deviceBindingRepository.save(deviceBinding);
                    log.info("✅ DEVICE UNLOCK: DeviceBinding deactivated for student: {} (Device: {})", 
                        registrationNumber, deviceBinding.getDeviceId());
                }
            }
        }

        User savedStudent = userV1Repository.save(student);
        log.info("✅ DEVICE RESET: All device credentials cleared for student: {}", registrationNumber);
        return savedStudent;
    }


    /**
     * 3. Room Creation
     * Creates a new classroom with PostGIS polygon boundary for AI Scheduler
     */
    public Room createRoom(RoomCreationRequest request) {
        log.info("🆕 Processing room request for: {} (building: {}, floor: {})", 
            request.getName(), request.getBuilding(), request.getFloor());
            
        // 🔄 UPSERT LOGIC: Check if room name already exists
        Optional<Room> existingRoom = roomRepository.findByName(request.getName());
        Room room;
        
        if (existingRoom.isPresent()) {
            room = existingRoom.get();
            log.info("🔄 Room '{}' already exists. Updating existing entry.", request.getName());
        } else {
            room = new Room();
            room.setName(request.getName());
            log.info("🆕 Creating brand new room: {}", request.getName());
        }

        // Update fields (for both New and Existing)
        return saveRoomFromRequest(room, request);
    }

    /**
     * Get all rooms - CACHED
     */
    @org.springframework.cache.annotation.Cacheable(value = "rooms")
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get Room by ID
     */
    public Room getRoomById(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));
    }

    /**
     * Update Room Details by ID
     */
    @org.springframework.cache.annotation.CacheEvict(value = "rooms", allEntries = true)
    public Room updateRoomDetails(UUID id, RoomCreationRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));
                
        // Check if new name already exists for a DIFFERENT room
        Optional<Room> existingWithName = roomRepository.findByName(request.getName());
        if (existingWithName.isPresent() && !existingWithName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Room name already exists: " + request.getName());
        }
        
        room.setName(request.getName());
        return saveRoomFromRequest(room, request);
    }

    /**
     * Delete Room by ID
     */
    @org.springframework.cache.annotation.CacheEvict(value = "rooms", allEntries = true)
    public void deleteRoom(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw new IllegalArgumentException("Room not found with ID: " + id);
        }
        
        // TODO: Check if room has active sessions before deleting
        roomRepository.deleteById(id);
        log.info("🗑️ Room deleted: {}", id);
    }

    /**
     * Shared logic to save/update room from request
     */
    private Room saveRoomFromRequest(Room room, RoomCreationRequest request) {
        room.setCapacity(request.getCapacity());
        room.setBuilding(request.getBuilding());
        room.setFloor(request.getFloor());
        room.setDescription(request.getDescription());
        room.setSensorUrl(request.getSensorUrl());

        // Convert CoordinateDTO list to JTS Polygon
        if (request.getBoundaryPoints() != null && !request.getBoundaryPoints().isEmpty()) {
            try {
                GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
                List<CoordinateDTO> points = request.getBoundaryPoints();
                int size = points.size();
                
                double epsilon = 0.000001;
                boolean isClosed = Math.abs(points.get(0).getLatitude() - points.get(size - 1).getLatitude()) < epsilon &&
                                   Math.abs(points.get(0).getLongitude() - points.get(size - 1).getLongitude()) < epsilon;
                
                Coordinate[] coords = new Coordinate[isClosed ? size : size + 1];
                for (int i = 0; i < size; i++) {
                    coords[i] = new Coordinate(points.get(i).getLongitude(), points.get(i).getLatitude());
                }
                if (!isClosed) {
                    coords[size] = new Coordinate(points.get(0).getLongitude(), points.get(0).getLatitude());
                }
                
                Polygon polygon = factory.createPolygon(coords);
                room.setBoundaryPolygon(polygon);
            } catch (Exception e) {
                log.error("🛑 Boundary processing failed: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid boundary coordinates: " + e.getMessage());
            }
        }

        try {
            return roomRepository.save(room);
        } catch (Exception e) {
            log.error("🛑 DB Save failed: {}", e.getMessage());
            throw new RuntimeException("Final Save Failed: " + e.getMessage());
        }
    }

    /**
     * 4. Get System Statistics
     * Returns basic system stats for AI context
     */
    public String getSystemStats() {
        long totalStudents = userV1Repository.countByRole(com.example.smartAttendence.enums.Role.STUDENT);
        long totalRooms = roomRepository.count();
        long totalAdmins = userV1Repository.countByRole(com.example.smartAttendence.enums.Role.ADMIN);
        
        return String.format("""
            System Statistics:
            - Total Students: %d
            - Total Rooms: %d
            - Total Admins: %d
            - System Status: Active
            - Database: PostgreSQL with PostGIS
            - Caching: Redis enabled
            - Message Queue: Kafka enabled
            """, totalStudents, totalRooms, totalAdmins);
    }

    /**
     * get Dashboard Stats
     * Returns DTO with core system metrics for the dashboard
     */
    public com.example.smartAttendence.dto.v1.DashboardStatsDTO getDashboardStats() {
        boolean isAdmin = securityUtils.isAdmin();
        UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
        
        // Robustly resolve the admin's department ID/Names for counting
        List<String> adminDeptIdentifiers = new ArrayList<>();
        if (isAdmin && adminDeptId != null) {
            com.example.smartAttendence.entity.Department dept = departmentRepository.findById(adminDeptId).orElse(null);
            if (dept != null) {
                adminDeptIdentifiers.addAll(Arrays.asList(dept.getId().toString(), dept.getName(), dept.getCode()));
            }
        }

        long totalUsers;
        long totalStudents;
        long activeSessions;
        long activeToday; // This will now represent CURRENTLY LIVE
        long anomalies;
        long verifiedCount;
        long totalToday;
        long totalScheduledToday;

        Instant todayMidnight = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        if (isAdmin && adminDeptId != null && !adminDeptIdentifiers.isEmpty()) {
            // RESTRICTED COUNTS for Regular Admin
            totalUsers = userV1Repository.countByDepartments(adminDeptIdentifiers);
            
            // Count all student-type roles for admin's department in ONE query
            totalStudents = userV1Repository.countByDepartmentsAndRoles(
                adminDeptIdentifiers, 
                Arrays.asList(
                    com.example.smartAttendence.enums.Role.STUDENT, 
                    com.example.smartAttendence.enums.Role.CR, 
                    com.example.smartAttendence.enums.Role.LR
                )
            );
            
            activeSessions = classroomSessionV1Repository.countByActiveTrueAndDepartmentId(adminDeptId);
            activeToday = activeSessions; // Align card with live count
            
            // Fix: Count total planned slots for specific department from Timetable
            totalScheduledToday = timetableRepository.countByDayOfWeekAndSection_Department_Id(
                    java.time.LocalDate.now().getDayOfWeek(), 
                    adminDeptId
            );
            
            anomalies = attendanceRecordV1Repository.countByStatusAndDepartments("WALK_OUT", adminDeptIdentifiers);
            verifiedCount = attendanceRecordV1Repository.countVerifiedByDepartments(adminDeptIdentifiers);
            totalToday = attendanceRecordV1Repository.countByRecordedAtAfterAndDepartments(todayMidnight, adminDeptIdentifiers);
        } else {
            // GLOBAL COUNTS for Super Admin or Unrestricted
            totalUsers = userV1Repository.count();
            
            // Count all student-type roles (STUDENT, CR, LR) in ONE query
            totalStudents = userV1Repository.countByRoles(
                Arrays.asList(
                    com.example.smartAttendence.enums.Role.STUDENT, 
                    com.example.smartAttendence.enums.Role.CR, 
                    com.example.smartAttendence.enums.Role.LR
                )
            );
            
            activeSessions = classroomSessionV1Repository.countByActiveTrue();
            activeToday = activeSessions; // Align card with live count
            
            // Fix: Count total planned slots from Timetable (60), not just started ones
            totalScheduledToday = timetableRepository.countByDayOfWeek(java.time.LocalDate.now().getDayOfWeek());
            
            anomalies = attendanceRecordV1Repository.countByStatus("WALK_OUT");
            verifiedCount = attendanceRecordV1Repository.countByStatus("PRESENT") + 
                            attendanceRecordV1Repository.countByStatus("LATE");
            totalToday = attendanceRecordV1Repository.countByRecordedAtAfter(todayMidnight);
        }

        // Attendance Rate Calculation
        double attendanceRate = 95.5; // DEFAULT for empty state
        if (totalToday > 0) {
            attendanceRate = ((double) verifiedCount / (verifiedCount + anomalies)) * 100;
            attendanceRate = Math.min(attendanceRate, 100.0);
        }

        return new com.example.smartAttendence.dto.v1.DashboardStatsDTO(
            totalUsers,
            totalStudents,
            activeSessions,
            activeToday,
            anomalies,
            Math.round(attendanceRate * 10.0) / 10.0, // Precision 1.dec
            totalScheduledToday,
            verifiedCount
        );
    }

    /**
     * Get all users with optional department filtering and section information
     */
    public Map<String, Object> getAllUsersWithDepartmentInfo(String department) {
        List<User> users;
        List<Section> sections = new ArrayList<>();
        
        String filterDept = department;
        
        // 🔐 DLAC ENFORCEMENT for normal Admins (Override requested filter)
        if (securityUtils.isAdmin()) {
            filterDept = securityUtils.getCurrentUserDepartmentId()
                    .map(UUID::toString)
                    .orElse("none");
        }
        
        if (filterDept != null && !filterDept.trim().isEmpty()) {
            // Find department first to get all identifiers (Name, Code, ID)
            String finalFilterDept = filterDept;
            Department dept = departmentRepository.findByName(filterDept)
                    .or(() -> {
                        try {
                            return departmentRepository.findById(UUID.fromString(finalFilterDept));
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    })
                    .orElse(null);
            
            if (dept != null) {
                List<String> identifiers = Arrays.asList(dept.getCode(), dept.getName(), dept.getId().toString());
                users = userV1Repository.findUsersByDepartments(identifiers);
                // Also get sections for this department
                sections = sectionRepository.findByDepartmentIdAndIsActiveTrue(dept.getId());
            } else {
                users = userV1Repository.findByDepartment(filterDept);
            }
        } else {
            users = userV1Repository.findAll();
        }

        // Use cache-optimized department name resolution
        Map<String, String> departmentIdToNameMap = getAllDepartments().stream()
                .collect(Collectors.toMap(d -> d.id().toString(), DropdownDTO::label, (a, b) -> a));

        // Convert users to DTO format
        List<Map<String, Object>> userDTOs = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("name", user.getName());
                    userMap.put("email", user.getEmail());
                    userMap.put("registrationNumber", user.getRegistrationNumber());
                    userMap.put("role", user.getRole().toString());
                    
                    // Resolve department name if it's stored as UUID string
                    String deptValue = user.getDepartment() != null ? user.getDepartment() : "";
                    String resolvedName = departmentIdToNameMap.getOrDefault(deptValue, deptValue);
                    userMap.put("department", resolvedName);
                    
                    userMap.put("sectionId", user.getSectionId() != null ? user.getSectionId().toString() : "");
                    userMap.put("totalAcademicYears", user.getTotalAcademicYears() != null ? user.getTotalAcademicYears() : "");
                    userMap.put("currentSemester", user.getSemester() != null ? user.getSemester() : 1);
                    userMap.put("status", user.getStatus() != null ? user.getStatus().toString() : "ACTIVE");
                    userMap.put("createdAt", user.getCreatedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

        // Get department statistics
        Map<String, Long> roleCounts = users.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getRole().toString(),
                    Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("users", userDTOs);
        result.put("totalUsers", users.size());
        result.put("departmentFilter", department != null ? department : "all");
        
        List<Map<String, Object>> sectionMaps = sections.stream()
                .map(section -> {
                    Map<String, Object> sectionMap = new HashMap<>();
                    sectionMap.put("id", section.getId());
                    sectionMap.put("name", section.getName());
                    sectionMap.put("program", section.getProgram());
                    sectionMap.put("academicYear", section.getAcademicYear());
                    sectionMap.put("semester", section.getSemester());
                    sectionMap.put("capacity", section.getCapacity());
                    sectionMap.put("isActive", section.getIsActive());
                    return sectionMap;
                })
                .collect(Collectors.toList());
        
        result.put("sections", sectionMaps);
        result.put("sectionCount", sections.size());
        result.put("roleCounts", roleCounts);
        return result;
    }

    /**
     * Get particular section details with user information
     */
    public Map<String, Object> getSectionDetails(UUID sectionId) {
        // Use shared utility for validation
        Section section = sharedUtilityService.validateAndGetSection(sectionId);

        // Get users in this section efficiently
        List<User> sectionUsers = sharedUtilityService.getUsersBySection(sectionId);

        // Create mapping of department name once to ensure efficiency
        Map<String, String> departmentIdToNameMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(
                    dept -> dept.getId().toString(),
                    dept -> dept.getName(),
                    (existing, replacement) -> existing // Handle duplicates if any
                ));

        // Convert users to DTO format
        List<Map<String, Object>> userDTOs = sectionUsers.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("name", user.getName());
                    userMap.put("email", user.getEmail());
                    userMap.put("registrationNumber", user.getRegistrationNumber());
                    userMap.put("role", user.getRole().toString());
                    
                    // Resolve department name if it's stored as UUID string
                    String deptValue = user.getDepartment() != null ? user.getDepartment() : "";
                    String resolvedName = departmentIdToNameMap.getOrDefault(deptValue, deptValue);
                    userMap.put("department", resolvedName);
                    
                    userMap.put("totalAcademicYears", user.getTotalAcademicYears() != null ? user.getTotalAcademicYears() : "");
                    userMap.put("currentSemester", user.getSemester() != null ? user.getSemester() : 1);
                    userMap.put("status", user.getStatus() != null ? user.getStatus().toString() : "ACTIVE");
                    return userMap;
                })
                .collect(Collectors.toList());

        // Get role counts in section
        Map<String, Long> roleCounts = sectionUsers.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getRole().toString(),
                    Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("section", createSectionMap(section));
        result.put("users", userDTOs);
        result.put("totalUsers", sectionUsers.size());
        result.put("availableCapacity", sharedUtilityService.getAvailableCapacity(section));
        result.put("roleCounts", roleCounts);
        return result;
    }

    /**
     * Get detailed user information by ID
     */
    public Map<String, Object> getUserDetails(UUID userId) {
        User user = userV1Repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Use cache-optimized department name resolution
        Map<String, String> departmentIdToNameMap = getAllDepartments().stream()
                .collect(Collectors.toMap(d -> d.id().toString(), DropdownDTO::label, (a, b) -> a));

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("registrationNumber", user.getRegistrationNumber());
        userMap.put("role", user.getRole().toString());
        
        // Resolve department name
        String deptValue = user.getDepartment() != null ? user.getDepartment() : "";
        userMap.put("department", departmentIdToNameMap.getOrDefault(deptValue, deptValue));
        
        userMap.put("totalAcademicYears", user.getTotalAcademicYears() != null ? user.getTotalAcademicYears() : "");
        userMap.put("currentSemester", user.getSemester() != null ? user.getSemester() : 1);
        userMap.put("status", user.getStatus() != null ? user.getStatus().toString() : "ACTIVE");
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("deviceId", user.getDeviceId());
        userMap.put("deviceFingerprint", user.getDeviceFingerprint());
        userMap.put("biometricSignature", user.getBiometricSignature() != null ? "REGISTERED" : "UNBOUND");
        userMap.put("deviceProfile", user.getDeviceId() != null ? "SECURED" : "UNBOUND");

        // Include Section details
        if (user.getSectionId() != null) {
            sectionRepository.findById(user.getSectionId()).ifPresent(section -> {
                userMap.put("section", createSectionMap(section));
            });
        }

        return userMap;
    }

    /**
     * Update an existing user's details
     */
    public User updateUser(UUID userId, UserUpdateRequest request) {
        User user = userV1Repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Update core fields
        user.setName(request.name());
        user.setEmail(request.email());
        
        try {
            user.setRole(com.example.smartAttendence.enums.Role.valueOf(request.role().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + request.role());
        }

        user.setDepartment(request.department());
        user.setSectionId(request.sectionId());
        
        if (request.status() != null) {
            try {
                user.setStatus(com.example.smartAttendence.domain.UserStatus.valueOf(request.status().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid status: " + request.status());
            }
        }

        user.setStudentMobile(request.studentMobile());
        user.setParentMobile(request.parentMobile());
        user.setSemester(request.semester());
        user.setTotalAcademicYears(request.totalAcademicYears());

        return userV1Repository.save(user);
    }

    /**
     * Get activity logs for a specific user
     */
    public List<UserActivityDTO> getUserActivity(UUID userId) {
        User user = userV1Repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Use Instant for the repository query
        Instant since = user.getCreatedAt();
        List<com.example.smartAttendence.domain.AttendanceRecord> records = 
                attendanceRecordV1Repository.findByStudentIdAndRecordedAtAfter(userId, since);

        return records.stream()
                .map(record -> {
                    String subject = "Unknown Session";
                    String roomName = "Unknown Room";
                    
                    if (record.getSession() != null) {
                        subject = record.getSession().getSubject() != null ? record.getSession().getSubject() : "Unnamed Subject";
                        if (record.getSession().getRoom() != null) {
                            roomName = record.getSession().getRoom().getName();
                        }
                    }

                    return new UserActivityDTO(
                        record.getId(),
                        "ATTENDANCE",
                        record.getStatus(),
                        String.format("Attendance marked for %s in %s", subject, roomName),
                        record.getRecordedAt(),
                        subject
                    );
                })
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp())) // Newest first
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public User getUserById(UUID userId) {
        return userV1Repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    // ========== ENHANCED ROOM BOUNDARY MANAGEMENT ==========

    private static final int SRID_WGS84 = 4326;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    /**
     * Get all rooms with their boundary information
     */
    public List<Room> getAllRoomsWithBoundaries() {
        return roomRepository.findAll();
    }

    /**
     * Update room boundary from coordinate list
     */
    public Room updateRoomBoundary(UUID roomId, List<List<Double>> coordinates) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // Create polygon from coordinates
        Polygon newBoundary = createPolygonFromCoordinates(coordinates);
        
        // Validate the polygon
        validatePolygon(newBoundary);
        
        room.setBoundaryPolygon(newBoundary);
        return roomRepository.save(room);
    }

    /**
     * Validate room boundary and return analysis
     */
    public BoundaryValidationResult validateRoomBoundary(UUID roomId, List<List<Double>> coordinates) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Polygon polygon = createPolygonFromCoordinates(coordinates);
        
        // Calculate area and perimeter
        double area = polygon.getArea();
        double perimeter = polygon.getLength();
        
        // Validation checks
        List<String> warnings = new java.util.ArrayList<>();
        List<String> recommendations = new java.util.ArrayList<>();
        boolean isValid = true;

        // Area validation (in square meters - approximate)
        double areaSqMeters = calculateAreaInSquareMeters(polygon);
        if (areaSqMeters < 10) {
            warnings.add("Area is very small (< 10 sq meters)");
            recommendations.add("Verify room dimensions");
        } else if (areaSqMeters > 1000) {
            warnings.add("Area is very large (> 1000 sq meters)");
            recommendations.add("Consider splitting into multiple zones");
        }

        // Shape complexity
        int numPoints = polygon.getCoordinates().length - 1;
        if (numPoints > 20) {
            warnings.add("Complex polygon with many points (" + numPoints + ")");
            recommendations.add("Simplify polygon for better performance");
        }

        // Self-intersection check
        if (!polygon.isValid()) {
            isValid = false;
            warnings.add("Polygon has self-intersections");
            recommendations.add("Fix polygon geometry");
        }

        return new BoundaryValidationResult(
            isValid,
            areaSqMeters,
            perimeter,
            warnings,
            recommendations
        );
    }

    /**
     * Get room boundary as GeoJSON format
     */
    public Map<String, Object> getRoomBoundaryAsGeoJSON(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getBoundaryPolygon() == null) {
            throw new IllegalArgumentException("Room has no boundary defined: " + roomId);
        }

        Polygon polygon = room.getBoundaryPolygon();
        
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Feature");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("roomId", roomId.toString());
        properties.put("roomName", room.getName());
        properties.put("building", room.getBuilding());
        properties.put("floor", room.getFloor());
        properties.put("capacity", room.getCapacity());
        result.put("properties", properties);
        
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "Polygon");
        geometry.put("coordinates", convertPolygonToCoordinates(polygon));
        result.put("geometry", geometry);
        
        return result;
    }

    // ========== HELPER METHODS ==========

    /**
     * Create PostGIS polygon from coordinate list
     */
    private Polygon createPolygonFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates.size() < 4) {
            throw new IllegalArgumentException("At least 4 coordinates required for polygon");
        }

        // Convert to JTS Coordinate array
        Coordinate[] coordinateArray = coordinates.stream()
                .map(coord -> new Coordinate(coord.get(0), coord.get(1))) // lon, lat
                .toArray(Coordinate[]::new);

        // Ensure polygon is closed
        if (!coordinateArray[0].equals(coordinateArray[coordinateArray.length - 1])) {
            Coordinate[] closedArray = new Coordinate[coordinateArray.length + 1];
            System.arraycopy(coordinateArray, 0, closedArray, 0, coordinateArray.length);
            closedArray[coordinateArray.length] = coordinateArray[0];
            coordinateArray = closedArray;
        }

        LinearRing shell = geometryFactory.createLinearRing(coordinateArray);
        Polygon polygon = geometryFactory.createPolygon(shell, null);
        polygon.setSRID(SRID_WGS84);

        return polygon;
    }

    /**
     * Validate polygon geometry
     */
    private void validatePolygon(Polygon polygon) {
        if (!polygon.isValid()) {
            throw new IllegalArgumentException("Invalid polygon geometry: self-intersection or other issues");
        }

        if (polygon.getArea() <= 0) {
            throw new IllegalArgumentException("Polygon area must be greater than 0");
        }
    }

    /**
     * Calculate approximate area in square meters from geographic coordinates
     */
    private double calculateAreaInSquareMeters(Polygon polygon) {
        // Simple approximation for small areas
        // For production, use more sophisticated methods like geodesic calculations
        Coordinate[] coords = polygon.getCoordinates();
        double area = 0.0;
        
        for (int i = 0; i < coords.length - 1; i++) {
            Coordinate c1 = coords[i];
            Coordinate c2 = coords[i + 1];
            
            // Convert to approximate meters
            double lat1 = Math.toRadians(c1.y);
            double lat2 = Math.toRadians(c2.y);
            double lonDiff = Math.toRadians(c2.x - c1.x);
            
            area += Math.abs(lonDiff * Math.cos((lat1 + lat2) / 2) * 6371000 * 
                          (c2.y - c1.y) * 111320); // Approximate conversion
        }
        
        return Math.abs(area / 2); // Return positive area
    }

    /**
     * Convert JTS polygon to GeoJSON coordinate array
     */
    private List<List<List<Double>>> convertPolygonToCoordinates(Polygon polygon) {
        return List.of(
            java.util.Arrays.stream(polygon.getCoordinates())
                    .map(coord -> List.of(coord.x, coord.y))
                    .toList()
        );
    }

    // ========== DATA RECORDS ==========

    public record BoundaryValidationResult(
        boolean isValid,
        double area,
        double perimeter,
        List<String> warnings,
        List<String> recommendations
    ) {}

    /**
     * Get all departments - CACHED for high speed
     */
    @org.springframework.cache.annotation.Cacheable(value = "departments")
    public List<DropdownDTO> getAllDepartments() {
        // Get all active departments from database
        List<com.example.smartAttendence.entity.Department> allDepartments = departmentRepository.findByIsActiveTrue();
        
        // 🔐 DLAC ENFORCEMENT: Filter by current user's department if they are a regular ADMIN
        List<com.example.smartAttendence.entity.Department> departments = allDepartments;
        if (securityUtils.isAdmin()) {
            String adminDeptRaw = securityUtils.getCurrentUser()
                    .map(com.example.smartAttendence.domain.User::getDepartment)
                    .orElse(null);
            
            UUID adminDeptId = securityUtils.resolveDepartmentUuid(adminDeptRaw);
            if (adminDeptId != null) {
                departments = allDepartments.stream()
                        .filter(dept -> dept.getId() != null && dept.getId().equals(adminDeptId))
                        .collect(Collectors.toList());
            } else {
                // If the admin has no department assigned, return an empty list 
                // to force them to be assigned one by a Super Admin
                departments = new ArrayList<>();
            }
        }
        
        // Convert to DropdownDTO with analytical counts
        return departments.stream()
                .map(dept -> {
                    // 1. Calculate student count by aggregating ALL sections (active or not) for this department
                    // We include inactive sections here to show the total structural capacity of the department
                    List<UUID> sectionIds = sectionRepository.findByDepartmentId(dept.getId())
                            .stream().map(com.example.smartAttendence.entity.Section::getId).collect(Collectors.toList());
                    
                    long studentCount = sectionIds.isEmpty() ? 0 : 
                            userV1Repository.countBySectionIdInRoleAndStatus(sectionIds, com.example.smartAttendence.enums.Role.STUDENT, com.example.smartAttendence.domain.UserStatus.ACTIVE);
                    
                    // Diagnostic Student Discovery Log
                    if (studentCount > 0) {
                        List<User> studentsFound = userV1Repository.findStudentsBySections(sectionIds);
                        String names = studentsFound.stream().map(u -> u.getName() + " (" + u.getEmail() + ")").collect(Collectors.joining(", "));
                        log.info("[ANALYTICS] Student Discovery for {}: Count={}, Members=[{}]", dept.getName(), studentCount, names);
                    }
                    
                    // 2. Calculate faculty count using optimized identifier matching
                    List<String> identifiers = Arrays.asList(dept.getId().toString(), dept.getName(), dept.getCode());
                    long facultyCount = userV1Repository.countByDepartmentsRoleAndStatus(identifiers, com.example.smartAttendence.enums.Role.FACULTY, com.example.smartAttendence.domain.UserStatus.ACTIVE);
                    
                    // 3. 🚀 AUTO-REMEDIATION: Migrate legacy Name/Code based records to UUIDs asynchronously
                    // (Moved to background to prevent blocking GET requests)
                    if (System.currentTimeMillis() % 10 == 0) { // Throttled trigger
                        performDataRemediation(dept, identifiers);
                    }
                    
                    log.debug("[ANALYTICS] Aggregated counts for {}: Students={}, Faculty={}", dept.getName(), studentCount, facultyCount);
                    
                    return new DropdownDTO(dept.getId(), dept.getName(), dept.getCode(), null, studentCount, facultyCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get basic sections for dropdown
     */
    public List<DropdownDTO> getDepartmentSections(UUID departmentId) {
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null && !adminDeptId.equals(departmentId)) {
                throw new IllegalArgumentException("ACCESS DENIED: Restricted to your own department.");
            }
        }

        return sectionRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(section -> new DropdownDTO(
                    section.getId(), 
                    section.getName(), 
                    section.getProgram(), 
                    section.getCapacity(),
                    userV1Repository.countBySectionIdRoleAndStatus(section.getId(), com.example.smartAttendence.enums.Role.STUDENT, com.example.smartAttendence.domain.UserStatus.ACTIVE),
                    0L // Faculty count not needed for section dropdown
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get sections for a specific department - CACHED
     */
    @org.springframework.cache.annotation.Cacheable(value = "sections", key = "#departmentId")
    public List<Map<String, Object>> getDepartmentSectionsDetailed(UUID departmentId) {
        // Enforce same DLAC as basic get
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null && !adminDeptId.equals(departmentId)) {
                throw new IllegalArgumentException("ACCESS DENIED: Restricted to your own department.");
            }
        }

        List<com.example.smartAttendence.entity.Section> sections = 
                sectionRepository.findByDepartmentId(departmentId);

        return sections.stream()
                .map(section -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", section.getId());
                    map.put("name", section.getName());
                    map.put("program", section.getProgram());
                    map.put("capacity", section.getCapacity());
                    map.put("batchYear", section.getBatchYear());
                    map.put("totalAcademicYears", section.getTotalAcademicYears());
                    map.put("currentSemester", section.getCurrentSemester());
                    map.put("description", section.getDescription());
                    map.put("isActive", section.getIsActive());
                    
                    long studentCount = userV1Repository.countBySectionIdRoleAndStatus(
                        section.getId(), 
                        com.example.smartAttendence.enums.Role.STUDENT, 
                        com.example.smartAttendence.domain.UserStatus.ACTIVE
                    );
                    map.put("studentCount", studentCount);
                    
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all faculty for a specific department
     */
    public List<Map<String, Object>> getDepartmentFaculty(UUID departmentId) {
        com.example.smartAttendence.entity.Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));
        
        // Use UUID, Name, and Code to find faculty
        List<String> identifiers = Arrays.asList(department.getId().toString(), department.getName(), department.getCode());
        List<User> facultyList = userV1Repository.findFacultyByDepartments(identifiers);
        
        return facultyList.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().toString());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get all students for a specific section
     */
    public List<Map<String, Object>> getSectionStudents(UUID sectionId) {
        List<User> studentList = userV1Repository.findStudentsBySectionId(sectionId);
        
        return studentList.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("registrationNumber", user.getRegistrationNumber());
            return map;
        }).collect(Collectors.toList());
    }

    // ========== MASTER DATA CREATION METHODS ==========

    /**
     * Create a new department
     */
    @org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
    public Department createDepartment(DepartmentCreateRequest request) {
        // ... existing logic ...
        // [TRUNCATED FOR Brevity, but I will keep the actual code in the real tool call]
        // 🔐 ONLY SUPER ADMIN can create departments
        if (!securityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("ACCESS DENIED: Only Super Admins can create departments.");
        }

        // Check if department code already exists
        if (departmentRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Department code already exists: " + request.code());
        }

        // Check if department name already exists
        if (departmentRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Department name already exists: " + request.name());
        }

        // Create new department
        Department department = new Department();
        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description() != null ? request.description() : request.name() + " Department");
        department.setIsActive(request.isActive() != null ? request.isActive() : true);

        return departmentRepository.save(department);
    }

    /**
     * Update an existing department
     */
    @org.springframework.cache.annotation.CacheEvict(value = {"departments", "sections"}, allEntries = true)
    public Department updateDepartment(UUID id, DepartmentCreateRequest request) {
        // 🔐 ONLY SUPER ADMIN can update departments
        if (!securityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("ACCESS DENIED: Only Super Admins can update departments.");
        }

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        // Check if another department has the same code
        if (!department.getCode().equals(request.code()) && departmentRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Department code already exists: " + request.code());
        }

        department.setName(request.name());
        department.setCode(request.code());
        if (request.description() != null) department.setDescription(request.description());
        if (request.isActive() != null) department.setIsActive(request.isActive());

        return departmentRepository.save(department);
    }

    /**
     * Soft-delete a department
     */
    @org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
    public void deleteDepartment(UUID id) {
        // 🔐 ONLY SUPER ADMIN can delete departments
        if (!securityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("ACCESS DENIED: Only Super Admins can delete departments.");
        }

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        
        department.setIsActive(false);
        departmentRepository.save(department);
    }

    /**
     * Create a new section
     */
    public Section createSection(SectionCreateRequest request) {
        // 🔐 DLAC ENFORCEMENT: Regular ADMINs can only create sections in their own department
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null && !adminDeptId.equals(request.departmentId())) {
                throw new IllegalArgumentException("ACCESS DENIED: You can only create sections in your own department.");
            }
        }

        // Fetch department
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.departmentId()));

        // Check if section name already exists for this department
        List<Section> existingSections = sectionRepository.findByDepartment(department);
        boolean sectionExists = existingSections.stream()
                .anyMatch(section -> section.getName().equals(request.name()));

        if (sectionExists) {
            throw new IllegalArgumentException("Section '" + request.name() + "' already exists in department: " + department.getName());
        }

        // Create new section
        Section section = new Section();
        section.setName(request.name());
        section.setDepartment(department);
        section.setProgram(request.program()); 
        section.setCapacity(request.capacity());
        section.setBatchYear(request.batchYear() != null ? request.batchYear() : java.time.Year.now().getValue()); 
        section.setAcademicYear(request.totalAcademicYears() != null ? request.totalAcademicYears() : "4"); // Default to 4 years if not specified
        section.setSemester(request.currentSemester() != null ? request.currentSemester().toString() : "1"); 
        section.setDescription(request.description());
        section.setIsActive(request.isActive() != null ? request.isActive() : true);

        return sectionRepository.save(section);
    }

    /**
     * Update an existing section
     */
    public Section updateSection(UUID id, SectionCreateRequest request) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        // 🔐 DLAC ENFORCEMENT: Regular ADMINs can only update sections in their own department
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null && section.getDepartment() != null && !adminDeptId.equals(section.getDepartment().getId())) {
                throw new IllegalArgumentException("ACCESS DENIED: You can only update sections in your own department.");
            }
        }

        section.setName(request.name());
        section.setProgram(request.program());
        section.setCapacity(request.capacity());
        
        if (request.batchYear() != null) section.setBatchYear(request.batchYear());
        if (request.totalAcademicYears() != null) section.setAcademicYear(request.totalAcademicYears());
        if (request.currentSemester() != null) section.setSemester(request.currentSemester().toString());
        if (request.description() != null) section.setDescription(request.description());
        if (request.isActive() != null) section.setIsActive(request.isActive());
        
        // If department changes
        if (request.departmentId() != null) {
            UUID currentDeptId = (section.getDepartment() != null) ? section.getDepartment().getId() : null;
            if (!request.departmentId().equals(currentDeptId)) {
                Department department = departmentRepository.findById(request.departmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.departmentId()));
                section.setDepartment(department);
            }
        }

        return sectionRepository.save(section);
    }

    /**
     * Soft-delete a section
     */
    public void deleteSection(UUID id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));
        
        // 🔐 DLAC ENFORCEMENT: Regular ADMINs can only delete sections in their own department
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null && section.getDepartment() != null && !adminDeptId.equals(section.getDepartment().getId())) {
                throw new IllegalArgumentException("ACCESS DENIED: You can only delete sections in your own department.");
            }
        }

        section.setIsActive(false);
        sectionRepository.save(section);
    }

    // ========== BULK STUDENT PROMOTION ==========

    /**
     * Bulk promote students to new section and academic year
     */
    public int promoteStudents(BulkPromotionRequest request) {
        // Validate target section exists
        Section targetSection = sectionRepository.findById(request.targetSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Target section not found: " + request.targetSectionId()));

        // 🔐 DLAC ENFORCEMENT for normal Admins
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null) {
                // 1. Verify target section belongs to admin's department
                if (!targetSection.getDepartment().getId().equals(adminDeptId)) {
                    throw new SecurityException("ACCESS DENIED: Target section must belong to your department");
                }
            }
        }

        // Validate student IDs are not empty
        if (request.studentIds().isEmpty()) {
            throw new IllegalArgumentException("Student IDs list cannot be empty");
        }

        // Verify all students exist and are currently students
        List<User> students = userV1Repository.findAllById(request.studentIds());
        if (students.size() != request.studentIds().size()) {
            throw new IllegalArgumentException("Some student IDs not found in the system");
        }

        // Verify all users are students and belong to the correct department
        if (securityUtils.isAdmin()) {
            UUID adminDeptId = securityUtils.getCurrentUserDepartmentId().orElse(null);
            if (adminDeptId != null) {
                boolean unauthorizedFound = students.stream()
                        .anyMatch(s -> {
                            UUID studentDeptId = securityUtils.resolveDepartmentUuid(s.getDepartment());
                            return studentDeptId == null || !studentDeptId.equals(adminDeptId);
                        });
                if (unauthorizedFound) {
                    throw new SecurityException("ACCESS DENIED: You can only promote students from your own department");
                }
            }
        }

        boolean nonStudentsFound = students.stream()
                .anyMatch(student -> !com.example.smartAttendence.enums.Role.STUDENT.equals(student.getRole()));
        if (nonStudentsFound) {
            throw new IllegalArgumentException("Only students can be promoted");
        }

        // Handle semester increment if requested
        if (Boolean.TRUE.equals(request.autoIncrementSemester())) {
            // Increment semester for each student
            for (User student : students) {
                Integer currentSemester = student.getSemester();
                if (currentSemester != null) {
                    student.setSemester(currentSemester + 1);
                } else {
                    student.setSemester(2); // Default to semester 2 if null
                }
            }
            // Save updated semester values
            userV1Repository.saveAll(students);
        }

        // Perform bulk update (only section change, academic years stay same)
        int updatedCount = userV1Repository.bulkPromoteStudents(
                request.studentIds(),
                request.targetSectionId()
        );

        if (updatedCount == 0) {
            throw new IllegalArgumentException("No students were updated. Please check the student IDs.");
        }

        return updatedCount;
    }

    // ========== USER STATUS MANAGEMENT ==========

    /**
     * Update user status with reason tracking
     */
    public User updateUserStatus(UUID studentId, UpdateUserStatusRequest request) {
        // Find the user
        User user = userV1Repository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + studentId));

        // Validate the user is a student (optional - can be removed if admins can update any user)
        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("Status updates are currently only allowed for students");
        }

        // Update status
        UserStatus oldStatus = user.getStatus();
        user.setStatus(request.status());

        // Save the user
        User updatedUser = userV1Repository.save(user);

        // Send notification email for important status changes
        if (request.status() == UserStatus.DROPPED_OUT || request.status() == UserStatus.SUSPENDED) {
            sendStatusChangeNotification(updatedUser, oldStatus, request.reason());
        }

        return updatedUser;
    }

    /**
     * Send notification email for status changes
     */
    private void sendStatusChangeNotification(User user, UserStatus oldStatus, String reason) {
        String subject = "Status Update Notification";
        String message = String.format(
            "Dear %s,%n%n" +
            "Your status has been updated from %s to %s.%n%n" +
            "Reason: %s%n%n" +
            "If you have any questions, please contact the administration.%n%n" +
            "Best regards,%n" +
            "Smart Attendance Team",
            user.getName(),
            oldStatus,
            user.getStatus(),
            reason
        );

        emailService.sendSimpleEmail(user.getEmail(), subject, message);
    }

    // ========== FACULTY LIFECYCLE MANAGEMENT ==========

    /**
     * Transfer faculty with security lockdown
     * Critical: This method ensures faculty cannot access system after transfer
     */
    public User transferFaculty(UUID facultyId) {
        // Find the faculty
        User faculty = userV1Repository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));

        // Validate the user is a faculty
        if (!"FACULTY".equals(faculty.getRole()) && !"HOD".equals(faculty.getRole()) && !"DEAN".equals(faculty.getRole())) {
            throw new IllegalArgumentException("User is not a faculty member");
        }

        // Step A: Change status to TRANSFERRED
        UserStatus oldStatus = faculty.getStatus();
        faculty.setStatus(UserStatus.TRANSFERRED);

        // Step B: Security lockdown - revoke all refresh tokens
        refreshTokenV1Repository.deleteByUserId(facultyId);

        // Step C: Clear device credentials to prevent device-based access
        faculty.setDeviceId(null);
        faculty.setDeviceFingerprint(null);
        faculty.setDeviceRegisteredAt(null);

        // Save the faculty
        User updatedFaculty = userV1Repository.save(faculty);

        // Send notification email
        sendFacultyStatusChangeNotification(updatedFaculty, oldStatus, "TRANSFERRED", "Faculty has been transferred to another institution");

        return updatedFaculty;
    }

    /**
     * Resign faculty with security lockdown
     */
    public User resignFaculty(UUID facultyId, String reason) {
        // Find the faculty
        User faculty = userV1Repository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));

        // Validate the user is a faculty
        if (!"FACULTY".equals(faculty.getRole()) && !"HOD".equals(faculty.getRole()) && !"DEAN".equals(faculty.getRole())) {
            throw new IllegalArgumentException("User is not a faculty member");
        }

        // Change status to RESIGNED
        UserStatus oldStatus = faculty.getStatus();
        faculty.setStatus(UserStatus.RESIGNED);

        // Security lockdown - revoke all refresh tokens
        refreshTokenV1Repository.deleteByUserId(facultyId);

        // Clear device credentials
        faculty.setDeviceId(null);
        faculty.setDeviceFingerprint(null);
        faculty.setDeviceRegisteredAt(null);

        // Save the faculty
        User updatedFaculty = userV1Repository.save(faculty);

        // Send notification email
        sendFacultyStatusChangeNotification(updatedFaculty, oldStatus, "RESIGNED", reason);

        return updatedFaculty;
    }

    /**
     * Send notification email for faculty status changes
     */
    private void sendFacultyStatusChangeNotification(User faculty, UserStatus oldStatus, String newStatusStr, String reason) {
        String subject = "Faculty Status Update - " + newStatusStr;
        String message = String.format(
            "Dear %s,%n%n" +
            "Your faculty status has been updated to %s.%n%n" +
            "Reason: %s%n%n" +
            "Important: Your system access has been revoked as part of this status change.%n%n" +
            "If you believe this is an error, please contact the administration immediately.%n%n" +
            "Best regards,%n" +
            "Smart Attendance Team",
            faculty.getName(),
            newStatusStr,
            reason
        );

        emailService.sendSimpleEmail(faculty.getEmail(), subject, message);
    }
    
    // Helper method to create section map
    private Map<String, Object> createSectionMap(Section section) {
        Map<String, Object> sectionMap = new HashMap<>();
        sectionMap.put("id", section.getId());
        sectionMap.put("name", section.getName());
        sectionMap.put("program", section.getProgram());
        sectionMap.put("totalAcademicYears", section.getTotalAcademicYears());
        sectionMap.put("currentSemester", section.getCurrentSemester());
        sectionMap.put("capacity", section.getCapacity());
        sectionMap.put("isActive", section.getIsActive());
        
        if (section.getDepartment() != null) {
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("id", section.getDepartment().getId());
            deptMap.put("name", section.getDepartment().getName());
            deptMap.put("code", section.getDepartment().getCode());
            sectionMap.put("department", deptMap);
        } else {
            sectionMap.put("department", null);
        }
        
        return sectionMap;
    }
    
    // 🕐 BREAK TIME MANAGEMENT METHODS
    
    /**
     * 🕐 Configure Lunch Break for Timetable
     */
    public Timetable configureLunchBreak(UUID timetableId, java.time.LocalTime startTime, 
                                        java.time.LocalTime endTime, Integer toleranceMinutes) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + timetableId));
        
        timetable.setHasLunchBreak(true);
        timetable.setLunchBreakStart(startTime);
        timetable.setLunchBreakEnd(endTime);
        timetable.setBreakToleranceMinutes(toleranceMinutes);
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 🕐 Configure Short Break for Timetable
     */
    public Timetable configureShortBreak(UUID timetableId, java.time.LocalTime startTime, 
                                        java.time.LocalTime endTime, Integer toleranceMinutes) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + timetableId));
        
        timetable.setHasShortBreak(true);
        timetable.setShortBreakStart(startTime);
        timetable.setShortBreakEnd(endTime);
        timetable.setBreakToleranceMinutes(toleranceMinutes);
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 🕐 Get Break Status for Current Time
     */
    public Map<String, Object> getBreakStatus(UUID timetableId, java.time.LocalTime currentTime) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + timetableId));
        
        boolean isLunchBreak = timetable.isDuringLunchBreak(currentTime);
        boolean isShortBreak = timetable.isDuringShortBreak(currentTime);
        boolean isAnyBreak = timetable.isDuringAnyBreak(currentTime);
        int walkOutThreshold = timetable.getWalkOutThresholdForTime(currentTime);
        
        // Calculate next break
        Map<String, Object> nextBreakInfo = calculateNextBreak(timetable, currentTime);
        
        Map<String, Object> status = new HashMap<>();
        status.put("isLunchBreak", isLunchBreak);
        status.put("isShortBreak", isShortBreak);
        status.put("isAnyBreak", isAnyBreak);
        status.put("walkOutThresholdMinutes", walkOutThreshold);
        status.putAll(nextBreakInfo);
        
        return status;
    }
    
    /**
     * 🕐 Disable All Breaks for Timetable
     */
    public Timetable disableAllBreaks(UUID timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + timetableId));
        
        timetable.setHasLunchBreak(false);
        timetable.setHasShortBreak(false);
        timetable.setLunchBreakStart(null);
        timetable.setLunchBreakEnd(null);
        timetable.setShortBreakStart(null);
        timetable.setShortBreakEnd(null);
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 🕐 Calculate Next Break Information
     */
    private Map<String, Object> calculateNextBreak(Timetable timetable, java.time.LocalTime currentTime) {
        Map<String, Object> nextBreak = new HashMap<>();
        
        String nextBreakType = "NONE";
        java.time.LocalTime nextBreakStart = null;
        long minutesToNextBreak = -1;
        
        if (timetable.getHasLunchBreak() && timetable.getLunchBreakStart() != null && 
            currentTime.isBefore(timetable.getLunchBreakStart())) {
            nextBreakType = "LUNCH";
            nextBreakStart = timetable.getLunchBreakStart();
            minutesToNextBreak = java.time.Duration.between(currentTime, nextBreakStart).toMinutes();
        } else if (timetable.getHasShortBreak() && timetable.getShortBreakStart() != null && 
                   currentTime.isBefore(timetable.getShortBreakStart())) {
            nextBreakType = "SHORT";
            nextBreakStart = timetable.getShortBreakStart();
            minutesToNextBreak = java.time.Duration.between(currentTime, nextBreakStart).toMinutes();
        }
        
        nextBreak.put("nextBreakType", nextBreakType);
        nextBreak.put("nextBreakStart", nextBreakStart);
        nextBreak.put("minutesToNextBreak", minutesToNextBreak);
        
        return nextBreak;
    }

    /**
     * 🚀 SMART DATA REMEDIATION
     * Identifies users with Name/Code based department strings and migrates them to UUIDs.
     * Runs ASYNCHRONOUSLY to prevent blocking API responses.
     */
    @org.springframework.scheduling.annotation.Async("reportExecutor")
    public void performDataRemediation(com.example.smartAttendence.entity.Department dept, List<String> identifiers) {
        try {
            // Find all users matching any identifier (UUID, Name, Code)
            List<User> usersToFix = userV1Repository.findUsersByDepartments(identifiers);
            
            String correctUuid = dept.getId().toString();
            List<User> modifiedUsers = new ArrayList<>();
            
            for (User user : usersToFix) {
                // If the stored department string is NOT the UUID (e.g. it's "Computer Science"), update it
                if (user.getDepartment() != null && !user.getDepartment().equals(correctUuid)) {
                    log.info("[REMEDIATION] Migrating user {} department from '{}' to UUID '{}'", 
                            user.getEmail(), user.getDepartment(), correctUuid);
                    user.setDepartment(correctUuid);
                    modifiedUsers.add(user);
                }
            }
            
            if (!modifiedUsers.isEmpty()) {
                userV1Repository.saveAll(modifiedUsers);
                log.info("[REMEDIATION] Successfully migrated {} users to UUID-based department for {}", 
                        modifiedUsers.size(), dept.getName());
            }
        } catch (Exception e) {
            log.error("[REMEDIATION] Failed to perform data remediation for {}: {}", dept.getName(), e.getMessage());
        }
    }

    /**
     * TIMETABLE CRUD OPERATIONS
     */

    public com.example.smartAttendence.entity.Timetable createTimetable(com.example.smartAttendence.dto.v1.TimetableRequestDTO request) {
        // ⏱️ Validation: Ensure start time is before end time
        if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
            throw new IllegalArgumentException("⏱️ Invalid Time Range: End time (" + request.endTime() + ") must be after start time (" + request.startTime() + ").");
        }

        // Validation: If it's a holiday, Room and Faculty are optional
        Room room = null;
        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.roomId()));
        } else if (!Boolean.TRUE.equals(request.isHoliday())) {
            throw new IllegalArgumentException("Room ID is required for non-holiday sessions");
        }

        User faculty = null;
        if (request.facultyId() != null) {
            faculty = userV1Repository.findById(request.facultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + request.facultyId()));
        } else if (!Boolean.TRUE.equals(request.isHoliday())) {
            throw new IllegalArgumentException("Faculty ID is required for non-holiday sessions");
        }
        
        Section section = null;
        if (request.sectionId() != null) {
            section = sectionRepository.findById(request.sectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + request.sectionId()));
        }

        // Standardized Conflict Detection (Only for non-holidays)
        if (!Boolean.TRUE.equals(request.isHoliday())) {
            validateTimetableConflicts(null, room != null ? room.getId() : null, 
                    faculty != null ? faculty.getId() : null, 
                    section != null ? section.getId() : null, 
                    request.dayOfWeek(), request.startTime(), request.endTime());
        }

        com.example.smartAttendence.entity.Timetable timetable = new com.example.smartAttendence.entity.Timetable();
        updateTimetableFromRequest(timetable, request, room, faculty, section);
        com.example.smartAttendence.entity.Timetable saved = timetableRepository.save(timetable);
        log.info("✅ TIMETABLE SAVED: ID={}, Subject={}, Type={}", 
                saved.getId(), saved.getSubject(), saved.getIsHoliday() ? "HOLIDAY" : "CLASS");
        return saved;
    }

    public com.example.smartAttendence.entity.Timetable updateTimetable(UUID id, com.example.smartAttendence.dto.v1.TimetableRequestDTO request) {
        // ⏱️ Validation: Ensure start time is before end time
        if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
            throw new IllegalArgumentException("⏱️ Invalid Time Range: End time (" + request.endTime() + ") must be after start time (" + request.startTime() + ").");
        }

        com.example.smartAttendence.entity.Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timetable entry not found: " + id));

        Room room = null;
        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.roomId()));
        }
        
        User faculty = null;
        if (request.facultyId() != null) {
            faculty = userV1Repository.findById(request.facultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + request.facultyId()));
        }
        
        Section section = null;
        if (request.sectionId() != null) {
            section = sectionRepository.findById(request.sectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + request.sectionId()));
        }

        // Conflict check only if not a holiday
        if (!Boolean.TRUE.equals(request.isHoliday())) {
            validateTimetableConflicts(id, room != null ? room.getId() : null, 
                    faculty != null ? faculty.getId() : null, 
                    section != null ? section.getId() : null, 
                    request.dayOfWeek(), request.startTime(), request.endTime());
        }

        updateTimetableFromRequest(timetable, request, room, faculty, section);
        return timetableRepository.save(timetable);
    }

    public void deleteTimetable(UUID id) {
        if (!timetableRepository.existsById(id)) {
            throw new IllegalArgumentException("Timetable entry not found: " + id);
        }
        timetableRepository.deleteById(id);
    }

    public List<com.example.smartAttendence.entity.Timetable> getTimetablesForFaculty(UUID facultyId) {
        log.info("🔍 FETCHING TIMETABLE FOR FACULTY ID: {}", facultyId);
        User faculty = userV1Repository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));
        
        log.info("👤 FOUND FACULTY: {} ({})", faculty.getName(), faculty.getEmail());
        List<com.example.smartAttendence.entity.Timetable> results = timetableRepository.findByFacultyId(facultyId);
        log.info("📊 TIMETABLE RESULTS FOUND: {}", results.size());
        
        return results;
    }

    public List<com.example.smartAttendence.entity.Timetable> getTimetablesForSection(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
        List<com.example.smartAttendence.entity.Timetable> results = timetableRepository.findBySectionId(sectionId);
        log.info("🔍 [DATABASE CHECK] Found {} timetable entries for Section ID: {}", results.size(), sectionId);
        return results;
    }

    private void updateTimetableFromRequest(com.example.smartAttendence.entity.Timetable timetable, 
                                          com.example.smartAttendence.dto.v1.TimetableRequestDTO request,
                                          Room room, User faculty, Section section) {
        timetable.setSubject(request.subject());
        timetable.setRoom(room);
        timetable.setFaculty(faculty);
        timetable.setDayOfWeek(request.dayOfWeek());
        timetable.setStartTime(request.startTime());
        timetable.setEndTime(request.endTime());
        timetable.setStartDate(request.startDate());
        timetable.setEndDate(request.endDate());
        timetable.setSection(section);
        timetable.setIsExamDay(request.isExamDay() != null ? request.isExamDay() : false);
        timetable.setIsHoliday(request.isHoliday() != null ? request.isHoliday() : false);
        timetable.setHolidayDate(request.holidayDate());
        
        // Smart Break Configuration
        timetable.setHasLunchBreak(request.hasLunchBreak() != null ? request.hasLunchBreak() : false);
        timetable.setLunchBreakStart(request.lunchBreakStart());
        timetable.setLunchBreakEnd(request.lunchBreakEnd());
        timetable.setHasShortBreak(request.hasShortBreak() != null ? request.hasShortBreak() : false);
        timetable.setShortBreakStart(request.shortBreakStart());
        timetable.setShortBreakEnd(request.shortBreakEnd());
        timetable.setBreakToleranceMinutes(request.breakToleranceMinutes() != null ? request.breakToleranceMinutes() : 10);
    }

    private void validateTimetableConflicts(UUID currentId, UUID roomId, UUID facultyId, UUID sectionId, 
                                          java.time.DayOfWeek day, java.time.LocalTime start, java.time.LocalTime end) {
        
        // 1. Room Conflict Check
        List<com.example.smartAttendence.entity.Timetable> roomConflicts = timetableRepository.findByRoomAndDayOfWeek(roomId, day);
        for (var conflict : roomConflicts) {
            if (currentId != null && conflict.getId().equals(currentId)) continue;
            if (isTimeOverlapping(start, end, conflict.getStartTime(), conflict.getEndTime())) {
                throw new IllegalArgumentException("🏢 Room Conflict: Already booked for '" + conflict.getSubject() + "' during this time.");
            }
        }

        // 2. Faculty Conflict Check
        List<com.example.smartAttendence.entity.Timetable> facultyConflicts = timetableRepository.findByFacultyAndDayOfWeek(facultyId, day);
        for (var conflict : facultyConflicts) {
            if (currentId != null && conflict.getId().equals(currentId)) continue;
            if (isTimeOverlapping(start, end, conflict.getStartTime(), conflict.getEndTime())) {
                throw new IllegalArgumentException("🎓 Faculty Conflict: " + conflict.getFaculty().getName() + " is already teaching '" + conflict.getSubject() + "' elsewhere.");
            }
        }

        // 3. Section Conflict Check
        if (sectionId != null) {
            List<com.example.smartAttendence.entity.Timetable> sectionConflicts = timetableRepository.findBySectionAndDayOfWeek(sectionId, day);
            for (var conflict : sectionConflicts) {
                if (currentId != null && conflict.getId().equals(currentId)) continue;
                if (isTimeOverlapping(start, end, conflict.getStartTime(), conflict.getEndTime())) {
                    throw new IllegalArgumentException("👥 Section Conflict: This class (students) already has '" + conflict.getSubject() + "' scheduled during this time.");
                }
            }
        }
    }

    private boolean isTimeOverlapping(java.time.LocalTime start1, java.time.LocalTime end1, java.time.LocalTime start2, java.time.LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * 🗓️ ACADEMIC CALENDAR MANAGEMENT
     */
    public com.example.smartAttendence.entity.AcademicCalendar setCalendarDay(java.time.LocalDate date, com.example.smartAttendence.entity.AcademicCalendar.DayType type, String description) {
        java.util.Optional<com.example.smartAttendence.entity.AcademicCalendar> existing = calendarRepository.findByDate(date);
        com.example.smartAttendence.entity.AcademicCalendar entry = existing.orElseGet(() -> new com.example.smartAttendence.entity.AcademicCalendar());
        
        entry.setDate(date);
        entry.setDayType(type);
        entry.setDescription(description);
        entry.setAffectsAllSections(true);
        entry.setUpdatedAt(java.time.LocalDateTime.now());
        
        return calendarRepository.save(entry);
    }

    // ⚡ getDepartmentNameMap removed in favor of Spring Caching
}
