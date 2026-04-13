package com.example.smartAttendence.security;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔐 PRODUCTION-GRADE ROLE VALIDATION SERVICE
 * 
 * This service ensures consistent role handling across the entire application
 * and prevents 403 Forbidden errors by standardizing role format.
 * 
 * Key Features:
 * - Consistent ROLE_ prefix handling
 * - Real-time role validation
 * - Authority conversion utilities
 * - Role-based access control helpers
 */
@Service
public class RoleValidationService {

    /**
     * 🔐 Converts User Role enum to Spring Security Authority with ROLE_ prefix
     * This ensures consistency with CustomUserDetailsService
     */
    public List<GrantedAuthority> getAuthorities(Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toString()));
    }

    /**
     * 🔐 Converts User object to Spring Security Authorities
     * Used for real-time role validation
     */
    public List<GrantedAuthority> getAuthorities(User user) {
        return getAuthorities(user.getRole());
    }

    /**
     * 🔐 Validates if user has the required role for API access
     * This prevents 403 errors in real-time scenarios
     */
    public boolean hasRole(User user, String requiredRole) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        // Ensure both roles have ROLE_ prefix for comparison
        String userRole = ensureRolePrefix(user.getRole().toString());
        String requiredRoleWithPrefix = ensureRolePrefix(requiredRole);

        return userRole.equals(requiredRoleWithPrefix);
    }

    /**
     * 🔐 Validates if user has any of the required roles
     * Used for endpoints with multiple allowed roles
     */
    public boolean hasAnyRole(User user, String... requiredRoles) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        String userRole = ensureRolePrefix(user.getRole().toString());
        
        for (String requiredRole : requiredRoles) {
            if (userRole.equals(ensureRolePrefix(requiredRole))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 🔐 Ensures role has ROLE_ prefix for Spring Security compatibility
     * This is the core fix for the 403 Forbidden issue
     */
    public String ensureRolePrefix(String role) {
        if (role == null || role.trim().isEmpty()) {
            return role;
        }
        
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    /**
     * 🔐 Converts enum role to string with ROLE_ prefix
     * Used throughout the application for consistency
     */
    public String getRoleString(Role role) {
        return role != null ? "ROLE_" + role.toString() : null;
    }

    /**
     * 🔐 Validates role enum and converts to proper format
     * Prevents invalid role assignments in real-time
     */
    public Role validateAndConvertRole(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        // Remove ROLE_ prefix if present for enum conversion
        String cleanRole = roleString.startsWith("ROLE_") ? 
            roleString.substring(5) : roleString;

        try {
            return Role.valueOf(cleanRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleString + 
                ". Valid roles: " + java.util.Arrays.toString(Role.values()));
        }
    }

    /**
     * 🔐 Real-time role consistency check
     * Ensures user role is properly set for Spring Security
     */
    public boolean isRoleConsistent(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        // Check if role enum is valid
        try {
            Role.valueOf(user.getRole().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 🔐 Fixes user role if inconsistent
     * Used for data cleanup and real-time fixes
     */
    public User fixUserRole(User user, String correctRole) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Role validatedRole = validateAndConvertRole(correctRole);
        user.setRole(validatedRole);
        
        return user;
    }

    /**
     * 🔐 Gets all available roles with ROLE_ prefix
     * Used for API documentation and validation
     */
    public List<String> getAllRolesWithPrefix() {
        return java.util.Arrays.stream(Role.values())
            .map(role -> "ROLE_" + role.toString())
            .collect(Collectors.toList());
    }

    /**
     * 🔐 Validates API endpoint role requirements
     * Ensures SecurityConfig and CustomUserDetailsService are aligned
     */
    public boolean validateEndpointAccess(User user, String endpoint) {
        if (user == null) {
            return false;
        }

        String userRole = getRoleString(user.getRole());
        
        // Define endpoint-role mappings
        return switch (endpoint) {
            case "/api/v1/admin/**" -> 
                userRole.equals("ROLE_SUPER_ADMIN") || userRole.equals("ROLE_ADMIN");
            case "/api/v1/faculty/**" -> 
                userRole.equals("ROLE_FACULTY") || userRole.equals("ROLE_SUPER_ADMIN");
            case "/api/v1/student/**" -> 
                userRole.equals("ROLE_STUDENT") || userRole.equals("ROLE_FACULTY") || userRole.equals("ROLE_SUPER_ADMIN");
            case "/api/v1/attendance/session/start", "/api/v1/attendance/session/end" -> 
                userRole.equals("ROLE_SUPER_ADMIN");
            case "/api/v1/attendance/manual", "/api/v1/attendance/bulk" -> 
                userRole.equals("ROLE_FACULTY") || userRole.equals("ROLE_SUPER_ADMIN");
            case "/api/v1/attendance/scan", "/api/v1/attendance/mark" -> 
                userRole.equals("ROLE_STUDENT") || userRole.equals("ROLE_FACULTY");
            default -> 
                userRole != null; // Authenticated users can access other endpoints
        };
    }
}
