package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.CRLRAssignmentRequest;
import com.example.smartAttendence.entity.CRLRAssignment;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.repository.CRLRAssignmentRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.service.EmailService;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class CRLRAssignmentService {
    
    private final CRLRAssignmentRepository assignmentRepository;
    private final UserV1Repository userRepository;
    private final SectionRepository sectionRepository;
    private final EmailService emailService;
    private final SharedUtilityService sharedUtilityService;
    
    public CRLRAssignmentService(
            CRLRAssignmentRepository assignmentRepository,
            UserV1Repository userRepository,
            SectionRepository sectionRepository,
            EmailService emailService,
            SharedUtilityService sharedUtilityService) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.emailService = emailService;
        this.sharedUtilityService = sharedUtilityService;
    }
    
    /**
     * Assign CR/LR role to a student (Faculty Coordinator only)
     * Supports single or multiple section assignments for combined classes
     */
    public CRLRAssignment assignCRLRRole(CRLRAssignmentRequest request, String email) {
        User coordinator = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        // Validate coordinator is faculty
        if (coordinator.getRole() != com.example.smartAttendence.enums.Role.FACULTY) {
            throw new IllegalArgumentException("Only faculty coordinators can assign CR/LR roles");
        }
        
        // Validate student
        User student = userRepository.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        if (student.getRole() != com.example.smartAttendence.enums.Role.STUDENT) {
            throw new IllegalArgumentException("Only students can be assigned CR/LR roles. Found role: " + student.getRole());
        }
        
        // Validate section
        Section section = sharedUtilityService.validateAndGetSection(request.sectionId());
        
        // Check if student already has an active assignment for this role type
        List<CRLRAssignment> existingAssignments = assignmentRepository.findByUserIdAndActiveTrue(student.getId());
        boolean hasExistingRole = existingAssignments.stream()
            .anyMatch(assignment -> assignment.getRoleType() == CRLRAssignment.RoleType.valueOf(request.roleType().name()));
        
        if (hasExistingRole) {
            throw new IllegalArgumentException("Student already has an active " + request.roleType() + " assignment");
        }
        
        // Note: Allow multiple CR/LR assignments for different sections (combined classes)
        // But prevent duplicate assignments for the same section and role
        
        // Check if already assigned to this section with same role
        boolean hasSectionRole = assignmentRepository.existsByUserIdAndSectionIdAndRoleTypeAndActiveTrue(
            request.studentId(), request.sectionId(), CRLRAssignment.RoleType.valueOf(request.roleType().name())
        );
        
        if (hasSectionRole) {
            throw new IllegalArgumentException("Student is already assigned as " + request.roleType() + " for this section");
        }
        
        // Determine academic metadata (defaults from section if not provided)
        String academicYear = request.academicYear() != null ? request.academicYear() : section.getAcademicYear();
        String semester = request.semester() != null ? request.semester() : section.getSemester();

        // Create assignment
        CRLRAssignment assignment = new CRLRAssignment(
            student, section, 
            CRLRAssignment.RoleType.valueOf(request.roleType().name()),
            academicYear, semester, 
            coordinator.getId()
        );
        assignment.setNotes(request.notes());
        
        // Save assignment
        assignment = assignmentRepository.save(assignment);
        
        // Update user role if this is their first CR/LR assignment
        updateUserRoleForMultipleAssignments(student, request.roleType());
        
        // Send notifications
        sendAssignmentNotifications(assignment, coordinator);
        
        return assignment;
    }
    
    /**
     * Revoke CR/LR role from a student (Faculty Coordinator only)
     */
    public CRLRAssignment revokeCRLRRole(UUID assignmentId, String reason, String email) {
        User coordinator = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        CRLRAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        
        if (!assignment.getActive()) {
            throw new IllegalArgumentException("Assignment is already inactive");
        }
        
        // Revoke assignment
        assignment.revokeAssignment(coordinator.getId(), reason);
        assignment = assignmentRepository.save(assignment);
        
        // Update user role back to STUDENT
        User student = assignment.getUser();
        student.setRole(com.example.smartAttendence.enums.Role.STUDENT);
        userRepository.save(student);
        
        // Send notifications
        sendRevocationNotifications(assignment, coordinator, reason);
        
        return assignment;
    }
    
    /**
     * Get all CR/LR assignments for a section
     */
    public List<CRLRAssignment> getSectionAssignments(UUID sectionId) {
        return assignmentRepository.findActiveAssignmentsForSection(sectionId);
    }
    
    /**
     * Get CR/LR assignment history for a student
     */
    public List<CRLRAssignment> getStudentAssignmentHistory(UUID studentId) {
        return assignmentRepository.findByUserIdAndActiveTrue(studentId);
    }
    
    /**
     * Get all assignments managed by a coordinator
     */
    public List<CRLRAssignment> getCoordinatorAssignments(String email) {
        User coordinator = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        
        // TODO: Implement query to find assignments created by specific coordinator
        return List.of(); // Placeholder
    }
    
    /**
     * Update user role based on CR/LR assignment (supports multiple assignments)
     */
    private void updateUserRoleForMultipleAssignments(User student, CRLRAssignmentRequest.RoleType roleType) {
        // Check if this is the first CR/LR assignment
        List<CRLRAssignment> existingAssignments = assignmentRepository.findByUserIdAndActiveTrue(student.getId());
        
        if (existingAssignments.size() == 1) {
            // First assignment - update user role
            switch (roleType) {
                case CR -> student.setRole(com.example.smartAttendence.enums.Role.CR);
                case LR -> student.setRole(com.example.smartAttendence.enums.Role.LR);
            }
            userRepository.save(student);
        }
        // If multiple assignments, keep current role (CR takes precedence over LR if both exist)
    }
    
    /**
     * Update user role based on CR/LR assignment (legacy method)
     */
    private void updateUserRole(User student, CRLRAssignmentRequest.RoleType roleType) {
        updateUserRoleForMultipleAssignments(student, roleType);
    }
    
    /**
     * Send assignment notifications
     */
    private void sendAssignmentNotifications(CRLRAssignment assignment, User coordinator) {
        // Send email to student
        String subject = String.format("Congratulations! You have been appointed as %s", 
            assignment.getRoleType().name());
        String body = String.format(
            "Dear %s,\n\n" +
            "You have been appointed as %s for %s section.\n\n" +
            "📋 Assignment Details:\n" +
            "• Role: %s\n" +
            "• Section: %s\n" +
            "• Academic Year: %s\n" +
            "• Semester: %s\n" +
            "• Assigned by: %s\n" +
            "• Assigned on: %s\n\n" +
            "🔧 Your Responsibilities:\n" +
            "• Manage room changes for your section during emergencies\n" +
            "• Coordinate with faculty for classroom logistics\n" +
            "• Assist in maintaining discipline and attendance\n\n" +
            "You can now use the Smart Attendance app to make room changes for your section.\n\n" +
            "Best regards,\n" +
            "Smart Attendance Team",
            assignment.getUser().getName(),
            assignment.getRoleType().name(),
            assignment.getSection().getName(),
            assignment.getRoleType().name(),
            assignment.getSection().getName(),
            assignment.getAcademicYear(),
            assignment.getSemester(),
            coordinator.getName(),
            assignment.getAssignedAt()
        );
        
        // Send email
        try {
            emailService.sendSimpleEmail(assignment.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            // Log error but don't throw - assignment is still valid
            log.debug("Failed to send assignment notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send revocation notifications
     */
    private void sendRevocationNotifications(CRLRAssignment assignment, User coordinator, String reason) {
        String subject = String.format("CR/LR Role Revoked - %s", assignment.getRoleType().name());
        String body = String.format(
            "Dear %s,\n\n" +
            "Your %s role for %s section has been revoked.\n\n" +
            "📋 Revocation Details:\n" +
            "• Role: %s\n" +
            "• Section: %s\n" +
            "• Reason: %s\n" +
            "• Revoked by: %s\n" +
            "• Revoked on: %s\n\n" +
            "Your access to room change features has been removed.\n\n" +
            "Best regards,\n" +
            "Smart Attendance Team",
            assignment.getUser().getName(),
            assignment.getRoleType().name(),
            assignment.getSection().getName(),
            assignment.getRoleType().name(),
            assignment.getSection().getName(),
            reason,
            coordinator.getName(),
            assignment.getRevokedAt()
        );
        
        // Send email
        try {
            emailService.sendSimpleEmail(assignment.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            // Log error but don't throw - revocation is still valid
            log.debug("Failed to send revocation notification: {}", e.getMessage());
        }
    }
}
