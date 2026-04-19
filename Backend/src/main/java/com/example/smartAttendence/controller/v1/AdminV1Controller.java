package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.domain.User;
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
import com.example.smartAttendence.dto.v1.UserUpdateRequest;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.service.v1.AdminV1Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.example.smartAttendence.dto.v1.ImageCalibrationRequest;
import com.example.smartAttendence.dto.v1.CoordinateDTO;
import com.example.smartAttendence.service.VirtualCalibrationService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminV1Controller {

    private final AdminV1Service adminV1Service;
    private final ClassroomSessionV1Repository classroomSessionRepository;
    private final VirtualCalibrationService virtualCalibrationService;

    public AdminV1Controller(AdminV1Service adminV1Service, 
                             ClassroomSessionV1Repository classroomSessionRepository,
                             VirtualCalibrationService virtualCalibrationService) {
        this.adminV1Service = adminV1Service;
        this.classroomSessionRepository = classroomSessionRepository;
        this.virtualCalibrationService = virtualCalibrationService;
    }

    /**
     * 📊 Get Dashboard Statistics
     * GET /api/v1/admin/dashboard/stats
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<com.example.smartAttendence.dto.v1.DashboardStatsDTO> getDashboardStats() {
        try {
            return ResponseEntity.ok(adminV1Service.getDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. Device & Biometric Reset
     * POST /api/v1/admin/students/{registrationNumber}/reset-device
     */
    @PostMapping("/students/{registrationNumber}/reset-device")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStudentDevice(@PathVariable String registrationNumber) {
        try {
            User student = adminV1Service.resetStudentDevice(registrationNumber);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                        "message", "Device reset successfully for student",
                        "studentId", student.getId(),
                        "registrationNumber", student.getRegistrationNumber(),
                        "email", student.getEmail()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset device: " + e.getMessage()));
        }
    }

    /**
     * 3. Room Creation with Exact Boundaries
     * POST /api/v1/admin/rooms
     */
    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody RoomCreationRequest request) {
        try {
            Room room = adminV1Service.createRoom(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "message", "Room created successfully with exact boundaries",
                        "roomId", room.getId(),
                        "name", room.getName(),
                        "capacity", room.getCapacity(),
                        "building", room.getBuilding(),
                        "floor", room.getFloor(),
                        "boundarySet", room.getBoundaryPolygon() != null,
                        "boundaryType", getBoundaryType(room.getBoundaryPolygon())
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create room: " + e.getMessage()));
        }
    }

    /**
     * Get Room Details
     * GET /api/v1/admin/rooms/{id}
     */
    @GetMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable UUID id) {
        try {
            Room room = adminV1Service.getRoomById(id);
            return ResponseEntity.ok(Map.of(
                "message", "Room retrieved successfully",
                "room", Map.of(
                    "roomId", room.getId(),
                    "name", room.getName(),
                    "capacity", room.getCapacity(),
                    "building", room.getBuilding(),
                    "floor", room.getFloor(),
                    "description", room.getDescription() != null ? room.getDescription() : "",
                    "sensorUrl", room.getSensorUrl() != null ? room.getSensorUrl() : "",
                    "hasBoundary", room.getBoundaryPolygon() != null,
                    "boundaryType", getBoundaryType(room.getBoundaryPolygon())
                )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve room: " + e.getMessage()));
        }
    }

    /**
     * Update Room Details
     * PUT /api/v1/admin/rooms/{id}
     */
    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRoom(@PathVariable UUID id, @Valid @RequestBody RoomCreationRequest request) {
        try {
            Room room = adminV1Service.updateRoomDetails(id, request);
            return ResponseEntity.ok(Map.of(
                "message", "Room updated successfully",
                "roomId", room.getId(),
                "name", room.getName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update room: " + e.getMessage()));
        }
    }

    /**
     * Delete Room
     * DELETE /api/v1/admin/rooms/{id}
     */
    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteRoom(@PathVariable UUID id) {
        try {
            adminV1Service.deleteRoom(id);
            return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete room: " + e.getMessage()));
        }
    }

    /**
     * 3.1. Get All Rooms with Boundaries
     * GET /api/v1/admin/rooms
     */
    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> getAllRooms() {
        try {
            var rooms = adminV1Service.getAllRoomsWithBoundaries();
            
            return ResponseEntity.ok(Map.of(
                "message", "Rooms retrieved successfully",
                "totalRooms", rooms.size(),
                "rooms", rooms.stream().map(room -> Map.of(
                    "roomId", room.getId(),
                    "name", room.getName(),
                    "capacity", room.getCapacity(),
                    "building", room.getBuilding(),
                    "floor", room.getFloor(),
                    "isAvailable", classroomSessionRepository.findActiveSessionsInRoom(room.getId(), Instant.now()).isEmpty(),
                    "hasBoundary", room.getBoundaryPolygon() != null,
                    "boundaryType", getBoundaryType(room.getBoundaryPolygon()),
                    "description", room.getDescription() != null ? room.getDescription() : ""
                )).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve rooms: " + e.getMessage()));
        }
    }

    /**
     * 3.2. Update Room Boundary
     * PUT /api/v1/admin/rooms/{roomId}/boundary
     */
    @PutMapping("/rooms/{roomId}/boundary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRoomBoundary(
            @PathVariable UUID roomId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");
            
            if (coordinates == null || coordinates.size() < 4) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least 4 coordinate points required for polygon"));
            }
            
            Room updatedRoom = adminV1Service.updateRoomBoundary(roomId, coordinates);
            
            return ResponseEntity.ok(Map.of(
                "message", "Room boundary updated successfully",
                "roomId", updatedRoom.getId(),
                "name", updatedRoom.getName(),
                "boundarySet", true,
                "boundaryType", getBoundaryType(updatedRoom.getBoundaryPolygon())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update room boundary: " + e.getMessage()));
        }
    }

    /**
     * 3.2.1 Virtual Geofencing Calibration (The Calibration Bridge)
     * Maps 4 pinned 2D pixels to geographic 3D coordinates.
     * POST /api/v1/admin/rooms/calibrate-boundary
     */
    @PostMapping("/rooms/calibrate-boundary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> calibrateVirtualBoundary(
            @Valid @RequestBody ImageCalibrationRequest request) {
        try {
            List<CoordinateDTO> boundaryCoords = virtualCalibrationService.calculateBoundaryCoordinates(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Virtual calibration completed successfully",
                "coordinates", boundaryCoords,
                "metadata", Map.of(
                    "baseLat", request.getBaseLatitude(),
                    "baseLon", request.getBaseLongitude(),
                    "processedPoints", request.getPoints().size()
                )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Calibration failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calibrate boundary: " + e.getMessage()));
        }
    }

    /**
     * 3.3. Validate Room Boundary
     * POST /api/v1/admin/rooms/{roomId}/validate-boundary
     */
    @PostMapping("/rooms/{roomId}/validate-boundary")
    public ResponseEntity<Map<String, Object>> validateRoomBoundary(
            @PathVariable UUID roomId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");
            
            var validation = adminV1Service.validateRoomBoundary(roomId, coordinates);
            
            return ResponseEntity.ok(Map.of(
                "message", "Boundary validation completed",
                "roomId", roomId,
                "isValid", validation.isValid(),
                "area", validation.area(),
                "perimeter", validation.perimeter(),
                "warnings", validation.warnings(),
                "recommendations", validation.recommendations()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to validate boundary: " + e.getMessage()));
        }
    }

    /**
     * 3.4. Get Room Boundary GeoJSON
     * GET /api/v1/admin/rooms/{roomId}/boundary
     */
    @GetMapping("/rooms/{roomId}/boundary")
    public ResponseEntity<Map<String, Object>> getRoomBoundary(@PathVariable UUID roomId) {
        try {
            var boundary = adminV1Service.getRoomBoundaryAsGeoJSON(roomId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Room boundary retrieved successfully",
                "roomId", roomId,
                "geojson", boundary
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve room boundary: " + e.getMessage()));
        }
    }

    /**
     * Helper method to determine boundary type
     */
    private String getBoundaryType(org.locationtech.jts.geom.Polygon polygon) {
        if (polygon == null) return "NONE";
        
        // Simple analysis of polygon shape
        int numPoints = polygon.getCoordinates().length - 1; // Exclude closing point
        
        if (numPoints == 4) return "RECTANGLE";
        if (numPoints == 5) return "PENTAGON";
        if (numPoints > 8) return "COMPLEX";
        return "POLYGON";
    }

    /**
     * 5. Student Onboarding
     * POST /api/v1/admin/onboard/student
     */
    @PostMapping("/onboard/student")
    public ResponseEntity<OnboardingResponseDTO> onboardStudent(@Valid @RequestBody StudentOnboardingRequest request) {
        try {
            OnboardingResponseDTO response = adminV1Service.onboardStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new OnboardingResponseDTO(null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 6. Faculty Onboarding
     * POST /api/v1/admin/onboard/faculty
     */
    @PostMapping("/onboard/faculty")
    public ResponseEntity<OnboardingResponseDTO> onboardFaculty(@Valid @RequestBody FacultyOnboardingRequest request) {
        try {
            OnboardingResponseDTO response = adminV1Service.onboardFaculty(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new OnboardingResponseDTO(null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 7. Admin Onboarding
     * POST /api/v1/admin/onboard/admin
     */
    @PostMapping("/onboard/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<OnboardingResponseDTO> onboardAdmin(@Valid @RequestBody AdminOnboardingRequest request) {
        try {
            // Get the caller's role from SecurityContext
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean callerIsSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            String targetRole = request.role().trim().toUpperCase();

            // ADMIN cannot create ADMIN or SUPER_ADMIN accounts
            if (!callerIsSuperAdmin && (targetRole.equals("ADMIN") || targetRole.equals("SUPER_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(null);
            }

            OnboardingResponseDTO response = adminV1Service.onboardAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new OnboardingResponseDTO(null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 8. Get all departments for dropdown
     * GET /api/v1/admin/departments
     */
    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<List<DropdownDTO>> getAllDepartments() {
        try {
            List<DropdownDTO> departments = adminV1Service.getAllDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 9. Get sections by department for basic dropdown
     * GET /api/v1/admin/departments/{departmentId}/sections
     */
    @GetMapping("/departments/{departmentId}/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<List<DropdownDTO>> getDepartmentSections(@PathVariable UUID departmentId) {
        try {
            return ResponseEntity.ok(adminV1Service.getDepartmentSections(departmentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 9.1. Get sections by department for cascading dropdown (Detailed)
     * GET /api/v1/admin/departments/{departmentId}/sections/details
     */
    @GetMapping("/departments/{departmentId}/sections/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentSectionsDetailed(@PathVariable UUID departmentId) {
        try {
            return ResponseEntity.ok(adminV1Service.getDepartmentSectionsDetailed(departmentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get faculty for a specific department
     * GET /api/v1/admin/departments/{departmentId}/faculty
     */
    @GetMapping("/departments/{departmentId}/faculty")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentFaculty(@PathVariable UUID departmentId) {
        try {
            return ResponseEntity.ok(adminV1Service.getDepartmentFaculty(departmentId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get students for a specific section
     * GET /api/v1/admin/sections/{sectionId}/students
     */
    @GetMapping("/sections/{sectionId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public ResponseEntity<List<Map<String, Object>>> getSectionStudents(@PathVariable UUID sectionId) {
        try {
            return ResponseEntity.ok(adminV1Service.getSectionStudents(sectionId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== MASTER DATA CREATION ENDPOINTS ==========

    /**
     * Create a new department
     * POST /api/v1/admin/departments
     */
    @PostMapping("/departments")

    public ResponseEntity<?> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        try {
            com.example.smartAttendence.entity.Department department = adminV1Service.createDepartment(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "message", "Department created successfully",
                        "departmentId", department.getId(),
                        "name", department.getName(),
                        "code", department.getCode(),
                        "isActive", department.getIsActive()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create department: " + e.getMessage()));
        }
    }

    /**
     * Update an existing department
     * PUT /api/v1/admin/departments/{id}
     */
    @PutMapping("/departments/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentCreateRequest request) {
        try {
            com.example.smartAttendence.entity.Department department = adminV1Service.updateDepartment(id, request);
            return ResponseEntity.ok(Map.of(
                "message", "Department updated successfully",
                "departmentId", department.getId(),
                "name", department.getName(),
                "code", department.getCode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update department: " + e.getMessage()));
        }
    }

    /**
     * Delete a department
     * DELETE /api/v1/admin/departments/{id}
     */
    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable UUID id) {
        try {
            adminV1Service.deleteDepartment(id);
            return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete department: " + e.getMessage()));
        }
    }

    /**
     * Create a new section
     * POST /api/v1/admin/sections
     */
    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createSection(@Valid @RequestBody SectionCreateRequest request) {
        try {
            com.example.smartAttendence.entity.Section section = adminV1Service.createSection(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "message", "Section created successfully",
                        "sectionId", section.getId(),
                        "name", section.getName(),
                        "departmentId", section.getDepartment().getId(),
                        "departmentName", section.getDepartment().getName(),
                        "program", section.getProgram(),
                        "capacity", section.getCapacity(),
                        "isActive", section.getIsActive()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create section: " + e.getMessage()));
        }
    }

    /**
     * Update an existing section
     * PUT /api/v1/admin/sections/{id}
     */
    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateSection(@PathVariable UUID id, @Valid @RequestBody SectionCreateRequest request) {
        try {
            com.example.smartAttendence.entity.Section section = adminV1Service.updateSection(id, request);
            return ResponseEntity.ok(Map.of(
                "message", "Section updated successfully",
                "sectionId", section.getId(),
                "name", section.getName(),
                "program", section.getProgram()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update section: " + e.getMessage()));
        }
    }

    /**
     * Delete a section
     * DELETE /api/v1/admin/sections/{id}
     */
    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteSection(@PathVariable UUID id) {
        try {
            adminV1Service.deleteSection(id);
            return ResponseEntity.ok(Map.of("message", "Section deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete section: " + e.getMessage()));
        }
    }

    /**
     * Bulk promote students to new section and academic year
     * PUT /api/v1/admin/students/promote
     */
    @PutMapping("/students/promote")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> promoteStudents(@Valid @RequestBody BulkPromotionRequest request) {
        try {
            int promotedCount = adminV1Service.promoteStudents(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Students promoted successfully",
                "promotedCount", promotedCount,
                "targetSectionId", request.targetSectionId(),
                "autoIncrementSemester", request.autoIncrementSemester(),
                "studentIds", request.studentIds()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to promote students: " + e.getMessage()));
        }
    }

    /**
     * Update student status
     * PUT /api/v1/admin/students/{studentId}/status
     */
    @PutMapping("/students/{studentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        try {
            User updatedUser = adminV1Service.updateUserStatus(studentId, request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Student status updated successfully",
                "studentId", updatedUser.getId(),
                "studentName", updatedUser.getName(),
                "studentEmail", updatedUser.getEmail(),
                "oldStatus", "Previous status",
                "newStatus", updatedUser.getStatus(),
                "reason", request.reason(),
                "updatedAt", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update student status: " + e.getMessage()));
        }
    }

    /**
     * Transfer faculty with security lockdown
     * PUT /api/v1/admin/faculty/{facultyId}/transfer
     */
    @PutMapping("/faculty/{facultyId}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> transferFaculty(@PathVariable UUID facultyId) {
        try {
            User transferredFaculty = adminV1Service.transferFaculty(facultyId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Faculty transferred successfully",
                "facultyId", transferredFaculty.getId(),
                "facultyName", transferredFaculty.getName(),
                "facultyEmail", transferredFaculty.getEmail(),
                "newStatus", transferredFaculty.getStatus(),
                "accessRevoked", true,
                "transferredAt", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to transfer faculty: " + e.getMessage()));
        }
    }

    /**
     * Get all users with optional department filtering
     * GET /api/v1/admin/users
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String department) {
        try {
            Map<String, Object> result = adminV1Service.getAllUsersWithDepartmentInfo(department);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve users: " + e.getMessage()));
        }
    }

    /**
     * Update an existing user's details
     * PUT /api/v1/admin/users/{userId}
     */
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            User updatedUser = adminV1Service.updateUser(userId, request);
            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "userId", updatedUser.getId(),
                "name", updatedUser.getName(),
                "email", updatedUser.getEmail(),
                "role", updatedUser.getRole(),
                "status", updatedUser.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }

    /**
     * Get activity logs for a specific user
     * GET /api/v1/admin/users/{userId}/activity
     */
    @GetMapping("/users/{userId}/activity")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> getUserActivity(@PathVariable UUID userId) {
        try {
            var activity = adminV1Service.getUserActivity(userId);
            return ResponseEntity.ok(Map.of(
                "message", "User activity retrieved successfully",
                "userId", userId,
                "activity", activity
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user activity: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable UUID userId) {
        try {
            Map<String, Object> result = adminV1Service.getUserDetails(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user details: " + e.getMessage()));
        }
    }
    @PutMapping("/faculty/{facultyId}/resign")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> resignFaculty(
            @PathVariable UUID facultyId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "Faculty resignation");
            User resignedFaculty = adminV1Service.resignFaculty(facultyId, reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "Faculty resignation processed successfully",
                "facultyId", resignedFaculty.getId(),
                "facultyName", resignedFaculty.getName(),
                "facultyEmail", resignedFaculty.getEmail(),
                "newStatus", resignedFaculty.getStatus(),
                "accessRevoked", true,
                "reason", reason,
                "resignedAt", java.time.Instant.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process faculty resignation: " + e.getMessage()));
        }
    }

    // 🕐 BREAK TIME MANAGEMENT ENDPOINTS

    /**
     * 🕐 Configure Lunch Break for Timetable
     * PUT /api/v1/admin/timetables/{timetableId}/lunch-break
     */
    @PutMapping("/timetables/{timetableId}/lunch-break")
    public ResponseEntity<Map<String, Object>> configureLunchBreak(
            @PathVariable UUID timetableId,
            @RequestBody Map<String, Object> request) {
        try {
            java.time.LocalTime startTime = java.time.LocalTime.parse((String) request.get("startTime"));
            java.time.LocalTime endTime = java.time.LocalTime.parse((String) request.get("endTime"));
            Integer toleranceMinutes = (Integer) request.getOrDefault("toleranceMinutes", 10);
            
            var updatedTimetable = adminV1Service.configureLunchBreak(timetableId, startTime, endTime, toleranceMinutes);
            
            return ResponseEntity.ok(Map.of(
                "message", "Lunch break configured successfully",
                "timetableId", updatedTimetable.getId(),
                "lunchBreakStart", updatedTimetable.getLunchBreakStart(),
                "lunchBreakEnd", updatedTimetable.getLunchBreakEnd(),
                "breakToleranceMinutes", updatedTimetable.getBreakToleranceMinutes(),
                "configuredAt", java.time.Instant.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to configure lunch break: " + e.getMessage()));
        }
    }

    /**
     * 🕐 Configure Short Break for Timetable
     * PUT /api/v1/admin/timetables/{timetableId}/short-break
     */
    @PutMapping("/timetables/{timetableId}/short-break")
    public ResponseEntity<Map<String, Object>> configureShortBreak(
            @PathVariable UUID timetableId,
            @RequestBody Map<String, Object> request) {
        try {
            java.time.LocalTime startTime = java.time.LocalTime.parse((String) request.get("startTime"));
            java.time.LocalTime endTime = java.time.LocalTime.parse((String) request.get("endTime"));
            Integer toleranceMinutes = (Integer) request.getOrDefault("toleranceMinutes", 10);
            
            var updatedTimetable = adminV1Service.configureShortBreak(timetableId, startTime, endTime, toleranceMinutes);
            
            return ResponseEntity.ok(Map.of(
                "message", "Short break configured successfully",
                "timetableId", updatedTimetable.getId(),
                "shortBreakStart", updatedTimetable.getShortBreakStart(),
                "shortBreakEnd", updatedTimetable.getShortBreakEnd(),
                "breakToleranceMinutes", updatedTimetable.getBreakToleranceMinutes(),
                "configuredAt", java.time.Instant.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to configure short break: " + e.getMessage()));
        }
    }

    /**
     * 🕐 Get Break Status for Current Time
     * GET /api/v1/admin/timetables/{timetableId}/break-status
     */
    @GetMapping("/timetables/{timetableId}/break-status")
    public ResponseEntity<Map<String, Object>> getBreakStatus(@PathVariable UUID timetableId) {
        try {
            java.time.LocalTime currentTime = java.time.LocalTime.now();
            var breakStatus = adminV1Service.getBreakStatus(timetableId, currentTime);
            
            return ResponseEntity.ok(Map.of(
                "message", "Break status retrieved successfully",
                "timetableId", timetableId,
                "currentTime", currentTime,
                "isLunchBreak", breakStatus.get("isLunchBreak"),
                "isShortBreak", breakStatus.get("isShortBreak"),
                "isAnyBreak", breakStatus.get("isAnyBreak"),
                "walkOutThresholdMinutes", breakStatus.get("walkOutThresholdMinutes"),
                "nextBreakType", breakStatus.get("nextBreakType"),
                "nextBreakStart", breakStatus.get("nextBreakStart"),
                "minutesToNextBreak", breakStatus.get("minutesToNextBreak")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get break status: " + e.getMessage()));
        }
    }

    /**
     * 🕐 Disable All Breaks for Timetable
     * DELETE /api/v1/admin/timetables/{timetableId}/breaks
     */
    @DeleteMapping("/timetables/{timetableId}/breaks")
    public ResponseEntity<Map<String, Object>> disableAllBreaks(@PathVariable UUID timetableId) {
        try {
            var updatedTimetable = adminV1Service.disableAllBreaks(timetableId);
            
            return ResponseEntity.ok(Map.of(
                "message", "All breaks disabled successfully",
                "timetableId", updatedTimetable.getId(),
                "hasLunchBreak", updatedTimetable.getHasLunchBreak(),
                "hasShortBreak", updatedTimetable.getHasShortBreak(),
                "disabledAt", java.time.Instant.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to disable breaks: " + e.getMessage()));
        }
    }

    /**
     * 🗓️ Manage Academic Calendar Day
     * POST /api/v1/admin/calendar/day
     */
    @PostMapping("/calendar/day")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> setCalendarDay(@RequestBody Map<String, Object> request) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse((String) request.get("date"));
            com.example.smartAttendence.entity.AcademicCalendar.DayType type = 
                com.example.smartAttendence.entity.AcademicCalendar.DayType.valueOf((String) request.get("type").toString().toUpperCase());
            String description = (String) request.get("description");
            
            var entry = adminV1Service.setCalendarDay(date, type, description);
            
            return ResponseEntity.ok(Map.of(
                "message", "Calendar entry updated successfully",
                "date", entry.getDate(),
                "dayType", entry.getDayType(),
                "description", entry.getDescription()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update calendar: " + e.getMessage()));
        }
    }
}
