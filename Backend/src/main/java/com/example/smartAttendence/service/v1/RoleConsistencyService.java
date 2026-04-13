package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.security.RoleValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 🔐 PRODUCTION-GRADE ROLE CONSISTENCY SERVICE
 * 
 * This service ensures role consistency across all real-time operations:
 * - User registration/onboarding
 * - Role updates and assignments
 * - Database cleanup and validation
 * - Real-time API access control
 * 
 * Prevents 403 Forbidden errors by maintaining role consistency
 */
@Service
@Transactional
@Slf4j
public class RoleConsistencyService {

    private final UserV1Repository userV1Repository;
    private final RoleValidationService roleValidationService;

    public RoleConsistencyService(UserV1Repository userV1Repository, 
                                  RoleValidationService roleValidationService) {
        this.userV1Repository = userV1Repository;
        this.roleValidationService = roleValidationService;
    }

    /**
     * 🔐 Validates and fixes user role during onboarding
     * Ensures new users have proper roles from day 1
     */
    public User validateUserRoleOnOnboarding(User user) {
        if (user == null || user.getRole() == null) {
            throw new IllegalArgumentException("User and role cannot be null");
        }

        // Validate role consistency
        if (!roleValidationService.isRoleConsistent(user)) {
            throw new IllegalArgumentException("Invalid role assigned: " + user.getRole());
        }

        // Ensure proper firstLogin and isTemporaryPassword flags
        if (user.getRole() == Role.SUPER_ADMIN) {
            user.setFirstLogin(false);
            user.setIsTemporaryPassword(false);
        } else {
            user.setFirstLogin(true);
            user.setIsTemporaryPassword(true);
        }

        return userV1Repository.save(user);
    }

    /**
     * 🔐 Updates user role with validation
     * Used for role changes, promotions, and assignments
     */
    public User updateUserRole(UUID userId, String newRole) {
        User user = userV1Repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate and convert role
        Role validatedRole = roleValidationService.validateAndConvertRole(newRole);
        
        // Update role
        user.setRole(validatedRole);
        
        // Update login flags based on role
        if (validatedRole == Role.SUPER_ADMIN) {
            user.setFirstLogin(false);
            user.setIsTemporaryPassword(false);
        }

        return userV1Repository.save(user);
    }

    /**
     * 🔐 Bulk role validation and fix
 * Fixes inconsistent roles across the database
     */
    public int fixInconsistentRoles() {
        List<User> allUsers = userV1Repository.findAll();
        int fixedCount = 0;

        for (User user : allUsers) {
            if (!roleValidationService.isRoleConsistent(user)) {
                // Try to fix the role
                try {
                    String currentRole = user.getRole().toString();
                    Role fixedRole = roleValidationService.validateAndConvertRole(currentRole);
                    user.setRole(fixedRole);
                    userV1Repository.save(user);
                    fixedCount++;
                } catch (IllegalArgumentException e) {
                    log.warn("❌ IllegalArgumentException: {}", e.getMessage());
                    // If role cannot be fixed, set to STUDENT as default
                    user.setRole(Role.STUDENT);
                    userV1Repository.save(user);
                    fixedCount++;
                }
            }
        }

        return fixedCount;
    }

    /**
     * 🔐 Validates role during CRLR assignments
     * Ensures CR/LR roles are properly assigned
     */
    public User validateCRLRRoleAssignment(UUID userId, String roleType) {
        User user = userV1Repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate base role is STUDENT
        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only students can be assigned CR/LR roles");
        }

        // Update role based on assignment
        Role newRole = switch (roleType.toUpperCase()) {
            case "CR" -> Role.CR;
            case "LR" -> Role.LR;
            default -> throw new IllegalArgumentException("Invalid role type: " + roleType);
        };

        user.setRole(newRole);
        return userV1Repository.save(user);
    }

    /**
     * 🔐 Resets user role back to STUDENT
     * Used when CR/LR assignments are removed
     */
    public User resetStudentRole(UUID userId) {
        User user = userV1Repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == Role.CR || user.getRole() == Role.LR) {
            user.setRole(Role.STUDENT);
            return userV1Repository.save(user);
        }

        return user; // No change needed
    }

    /**
     * 🔐 Validates admin user creation
     * Ensures admin users have proper configuration
     */
    public User validateAdminCreation(User adminUser, String roleString) {
        // Validate role
        Role adminRole = roleValidationService.validateAndConvertRole(roleString);
        
        if (adminRole != Role.ADMIN && adminRole != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Invalid admin role: " + roleString);
        }

        adminUser.setRole(adminRole);
        adminUser.setFirstLogin(false);
        adminUser.setIsTemporaryPassword(false);

        return userV1Repository.save(adminUser);
    }

    /**
     * 🔐 Real-time role validation for API access
     * Validates user role before API execution
     */
    public boolean validateApiAccess(UUID userId, String endpoint) {
        User user = userV1Repository.findById(userId).orElse(null);
        return roleValidationService.validateEndpointAccess(user, endpoint);
    }

    /**
     * 🔐 Cleanup stale admin user
     * Removes and recreates admin user with proper configuration
     */
    public User cleanupAndRecreateAdmin(String email, String password) {
        // Delete existing admin user
        userV1Repository.findByEmail(email).ifPresent(existing -> {
            log.info("🗑️ Deleting stale admin user: {}", email);
            userV1Repository.delete(existing);
        });

        // Create new admin user with proper configuration
        User newAdmin = new User();
        newAdmin.setName("Super Admin");
        newAdmin.setEmail(email);
        newAdmin.setRegistrationNumber("SA001");
        // Password should be encoded by the calling service
        newAdmin.setPassword(password);
        newAdmin.setRole(Role.SUPER_ADMIN);
        newAdmin.setFirstLogin(false);
        newAdmin.setIsTemporaryPassword(false);

        return userV1Repository.save(newAdmin);
    }

    /**
     * 🔐 Database health check for role consistency
     * Reports any role inconsistencies in the database
     */
    public RoleConsistencyReport checkRoleConsistency() {
        List<User> allUsers = userV1Repository.findAll();
        int totalUsers = allUsers.size();
        int consistentUsers = 0;
        int inconsistentUsers = 0;

        for (User user : allUsers) {
            if (roleValidationService.isRoleConsistent(user)) {
                consistentUsers++;
            } else {
                inconsistentUsers++;
            }
        }

        return new RoleConsistencyReport(totalUsers, consistentUsers, inconsistentUsers);
    }

    /**
     * 🔐 Role consistency report
     */
    public record RoleConsistencyReport(int totalUsers, int consistentUsers, int inconsistentUsers) {
        public boolean isHealthy() {
            return inconsistentUsers == 0;
        }

        public double getConsistencyPercentage() {
            return totalUsers > 0 ? (double) consistentUsers / totalUsers * 100 : 0;
        }
    }
}
