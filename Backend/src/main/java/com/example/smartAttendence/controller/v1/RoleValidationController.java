package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.service.v1.RoleConsistencyService;
import com.example.smartAttendence.security.RoleValidationService;
import com.example.smartAttendence.dto.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔐 ROLE VALIDATION CONTROLLER
 * 
 * Provides endpoints for testing and validating role consistency
 * Helps prevent and diagnose 403 Forbidden errors
 */
@RestController
@RequestMapping("/api/v1/role-validation")
public class RoleValidationController {

    private final RoleConsistencyService roleConsistencyService;
    private final RoleValidationService roleValidationService;

    public RoleValidationController(RoleConsistencyService roleConsistencyService,
                                   RoleValidationService roleValidationService) {
        this.roleConsistencyService = roleConsistencyService;
        this.roleValidationService = roleValidationService;
    }

    /**
     * 🔐 Check role consistency across the database
     * Only accessible by SUPER_ADMIN
     */
    @GetMapping("/consistency-check")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Object>>> checkRoleConsistency() {
        var report = roleConsistencyService.checkRoleConsistency();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", report.totalUsers());
        result.put("consistentUsers", report.consistentUsers());
        result.put("inconsistentUsers", report.inconsistentUsers());
        result.put("consistencyPercentage", report.getConsistencyPercentage());
        result.put("isHealthy", report.isHealthy());
        result.put("availableRoles", roleValidationService.getAllRolesWithPrefix());

        return ResponseEntity.ok(BaseResponse.success(result, "Role consistency check completed"));
    }

    /**
     * 🔐 Fix inconsistent roles in database
     * Only accessible by SUPER_ADMIN
     */
    @PostMapping("/fix-inconsistent-roles")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Object>>> fixInconsistentRoles() {
        int fixedCount = roleConsistencyService.fixInconsistentRoles();
        
        Map<String, Object> result = new HashMap<>();
        result.put("fixedCount", fixedCount);
        result.put("message", "Fixed " + fixedCount + " inconsistent roles");

        return ResponseEntity.ok(BaseResponse.success(result, "Role fix completed"));
    }

    /**
     * 🔐 Test role validation
     * Public endpoint for testing role conversion
     */
    @GetMapping("/test-role/{role}")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testRoleValidation(@PathVariable String role) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String roleWithPrefix = roleValidationService.ensureRolePrefix(role);
            result.put("inputRole", role);
            result.put("roleWithPrefix", roleWithPrefix);
            result.put("isValid", true);
            
            // Try to convert to enum
            try {
                var enumRole = roleValidationService.validateAndConvertRole(role);
                result.put("enumRole", enumRole.toString());
                result.put("enumSuccess", true);
            } catch (Exception e) {
                result.put("enumError", e.getMessage());
                result.put("enumSuccess", false);
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("isValid", false);
        }

        return ResponseEntity.ok(BaseResponse.success(result, "Role validation test"));
    }

    /**
     * 🔐 Get all available roles
     * Public endpoint for API documentation
     */
    @GetMapping("/available-roles")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getAvailableRoles() {
        Map<String, Object> result = new HashMap<>();
        result.put("rolesWithPrefix", roleValidationService.getAllRolesWithPrefix());
        result.put("enumValues", java.util.Arrays.toString(com.example.smartAttendence.enums.Role.values()));

        return ResponseEntity.ok(BaseResponse.success(result, "Available roles"));
    }

    /**
     * 🔐 Test endpoint access validation
     * Only accessible by authenticated users
     */
    @GetMapping("/test-endpoint-access")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testEndpointAccess(
            @RequestParam(required = false) String endpoint) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Test common endpoints
        String[] testEndpoints = {
            "/api/v1/admin/**",
            "/api/v1/faculty/**", 
            "/api/v1/student/**",
            "/api/v1/attendance/session/start",
            "/api/v1/attendance/scan"
        };

        Map<String, Boolean> accessResults = new HashMap<>();
        
        for (String testEndpoint : testEndpoints) {
            // This would need user context in real implementation
            // For now, just show the endpoint patterns
            accessResults.put(testEndpoint, true); // Placeholder
        }
        
        result.put("testedEndpoints", accessResults);
        result.put("message", "Endpoint access validation requires user context");

        return ResponseEntity.ok(BaseResponse.success(result, "Endpoint access test"));
    }
}
