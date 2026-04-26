package com.example.smartAttendence.util;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtils {

    private final UserV1Repository userV1Repository;
    private final com.example.smartAttendence.repository.DepartmentRepository departmentRepository;

    public SecurityUtils(UserV1Repository userV1Repository, 
                        com.example.smartAttendence.repository.DepartmentRepository departmentRepository) {
        this.userV1Repository = userV1Repository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Get the currently authenticated User domain object
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        
        return userV1Repository.findByEmail(authentication.getName());
    }

    /**
     * Get the department ID (as UUID) of the current user, resolving name/code if necessary
     */
    public Optional<UUID> getCurrentUserDepartmentId() {
        return getCurrentUser().map(user -> {
            String deptValue = user.getDepartment();
            return resolveDepartmentUuid(deptValue);
        });
    }

    /**
     * Robustly resolve a department string (ID, Name, or Code) into a UUID
     */
    public UUID resolveDepartmentUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        
        // 1. Try direct UUID parse
        try {
            return UUID.fromString(rawValue);
        } catch (Exception ignored) {}

        // 2. Try lookup by Name
        return departmentRepository.findByName(rawValue)
                .map(com.example.smartAttendence.entity.Department::getId)
                .orElseGet(() -> 
                    // 3. Try lookup by Code
                    departmentRepository.findByCode(rawValue)
                            .map(com.example.smartAttendence.entity.Department::getId)
                            .orElse(null)
                );
    }

    /**
     * Check if the current user is a Super Admin (Global Access)
     */
    public boolean isSuperAdmin() {
        return getCurrentUser()
                .map(user -> Role.SUPER_ADMIN.equals(user.getRole()))
                .orElse(false);
    }

    /**
     * Check if the current user is a normal Admin (Department Restricted)
     */
    public boolean isAdmin() {
        return getCurrentUser()
                .map(user -> Role.ADMIN.equals(user.getRole()))
                .orElse(false);
    }

    /**
     * Check if the current user is a Faculty member
     */
    public boolean isFaculty() {
        return getCurrentUser()
                .map(user -> Role.FACULTY.equals(user.getRole()))
                .orElse(false);
    }

    public String getClientIP() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attrs == null) return "0.0.0.0";
            
            jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null || xfHeader.isBlank()) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0].trim();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public String getUserAgent() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attrs == null) return "UNKNOWN";
            
            jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
            String ua = request.getHeader("User-Agent");
            return ua != null ? ua : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
