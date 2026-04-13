package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.EmergencySessionChangeRequest;
import com.example.smartAttendence.dto.v1.SubstituteClaimRequest;
import com.example.smartAttendence.entity.EmergencySessionChange;
import com.example.smartAttendence.service.v1.EmergencySessionService;
import java.security.Principal;
import com.example.smartAttendence.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencySessionController {
    
    private final EmergencySessionService emergencySessionService;
    
    public EmergencySessionController(EmergencySessionService emergencySessionService) {
        this.emergencySessionService = emergencySessionService;
    }
    
    /**
     * Handle emergency session changes
     * POST /api/v1/emergency/session-change
     */
    @PostMapping("/session-change")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> handleEmergencyChange(
            @Valid @RequestBody EmergencySessionChangeRequest request,
            Principal principal) {
        
        String email = principal.getName();
        try {
            EmergencySessionChange change = emergencySessionService.handleEmergencyChange(request, email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Emergency change processed successfully",
                "changeId", change.getId(),
                "sessionId", change.getSession().getId(),
                "changeType", change.getChangeType(),
                "effectiveTimestamp", change.getEffectiveTimestamp(),
                "emergencyOverride", change.getEmergencyOverride()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process emergency change: " + e.getMessage()));
        }
    }
    
    /**
     * Handle substitute faculty claim
     * POST /api/v1/emergency/substitute-claim
     */
    @PostMapping("/substitute-claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> handleSubstituteClaim(
            @Valid @RequestBody SubstituteClaimRequest request,
            Principal principal) {
        
        String email = principal.getName();
        try {
            var claim = emergencySessionService.handleSubstituteClaim(request, email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Substitute claim processed successfully",
                "changeId", claim.change().getId(),
                "sessionId", claim.updatedSession().getId(),
                "originalFacultyId", claim.change().getOriginalFacultyId(),
                "newFacultyId", claim.change().getNewFacultyId(),
                "emergencyOverride", claim.change().getEmergencyOverride()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process substitute claim: " + e.getMessage()));
        }
    }
    
    /**
     * Get emergency changes for a specific session
     * GET /api/v1/emergency/session/{sessionId}/changes
     */
    @GetMapping("/session/{sessionId}/changes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> getSessionChanges(@PathVariable UUID sessionId) {
        try {
            List<EmergencySessionChange> changes = emergencySessionService.getSessionChanges(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "changes", changes,
                "totalChanges", changes.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get session changes: " + e.getMessage()));
        }
    }
    
    /**
     * Get recent emergency overrides (for admin monitoring)
     * GET /api/v1/emergency/overrides
     */
    @GetMapping("/overrides")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecentEmergencyOverrides(
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<EmergencySessionChange> overrides = emergencySessionService.getRecentEmergencyOverrides(hours);
            
            return ResponseEntity.ok(Map.of(
                "timeframeHours", hours,
                "emergencyOverrides", overrides,
                "totalOverrides", overrides.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get emergency overrides: " + e.getMessage()));
        }
    }
    
    /**
     * Quick emergency room change (simplified endpoint)
     * POST /api/v1/emergency/quick-room-change
     */
    @PostMapping("/quick-room-change")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> quickRoomChange(
            @RequestParam UUID sessionId,
            @RequestParam UUID newRoomId,
            @RequestParam(required = false) String reason,
            Principal principal) {
        
        String email = principal.getName();
        try {
            EmergencySessionChangeRequest request = new EmergencySessionChangeRequest(
                sessionId,
                reason != null ? reason : "Emergency room change",
                EmergencySessionChangeRequest.EmergencyChangeType.ROOM_CHANGE,
                null, // newFacultyId
                newRoomId,
                null, // newStartTime
                null, // newEndTime
                null, // adminNotes
                true, // notifyStudents
                false // notifyParents
            );
            
            EmergencySessionChange change = emergencySessionService.handleEmergencyChange(request, email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Quick room change processed successfully",
                "changeId", change.getId(),
                "sessionId", sessionId,
                "oldRoomId", change.getOriginalRoomId(),
                "newRoomId", change.getNewRoomId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process quick room change: " + e.getMessage()));
        }
    }
}
