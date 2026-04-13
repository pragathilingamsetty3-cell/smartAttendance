package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.QRRoomChangeRequest;
import com.example.smartAttendence.dto.v1.RoomChangeRequest;
import com.example.smartAttendence.dto.v1.WeeklyRoomSwapConfig;
import com.example.smartAttendence.entity.RoomChangeTransition;
import com.example.smartAttendence.entity.WeeklyRoomSwap;
import com.example.smartAttendence.service.v1.RoomChangeService;
import java.security.Principal;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/room-change")
public class RoomChangeController {
    
    private final RoomChangeService roomChangeService;
    private final UserV1Repository userRepository;
    
    public RoomChangeController(RoomChangeService roomChangeService, UserV1Repository userRepository) {
        this.roomChangeService = roomChangeService;
        this.userRepository = userRepository;
    }
    
    /**
     * Handle QR-based room change (CR/LR/Faculty)
     * POST /api/v1/room-change/qr
     */
    @PostMapping("/qr")
    @PreAuthorize("hasAnyRole('FACULTY', 'CR', 'LR')")
    public ResponseEntity<Map<String, Object>> handleQRRoomChange(
            @Valid @RequestBody QRRoomChangeRequest request,
            Principal principal) {
        
        String email = principal.getName();
        try {
            RoomChangeTransition transition = roomChangeService.handleQRRoomChange(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Room change processed successfully",
                "transitionId", transition.getId(),
                "sessionId", transition.getSession().getId(),
                "originalRoomId", transition.getOriginalRoomId(),
                "newRoomId", transition.getNewRoomId(),
                "gracePeriodMinutes", transition.getGracePeriodMinutes(),
                "transitionStart", transition.getTransitionStartTime(),
                "transitionEnd", transition.getTransitionEndTime()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process QR room change: " + e.getMessage()));
        }
    }
    
    /**
     * Handle pre-planned room change
     * POST /api/v1/room-change/pre-planned
     */
    @PostMapping("/pre-planned")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> handlePrePlannedRoomChange(
            @Valid @RequestBody RoomChangeRequest request,
            Principal principal) {
        
        String email = principal.getName();
        try {
            RoomChangeTransition transition = roomChangeService.handlePrePlannedRoomChange(request, email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Pre-planned room change scheduled successfully",
                "transitionId", transition.getId(),
                "sessionId", transition.getSession().getId(),
                "scheduledTime", request.scheduledTime(),
                "newRoomId", request.roomId(),
                "sectionId", request.sectionId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to schedule pre-planned room change: " + e.getMessage()));
        }
    }
    
    /**
     * Handle weekly room swap
     * POST /api/v1/room-change/weekly-swap/{swapConfigId}
     */
    @PostMapping("/weekly-swap/{swapConfigId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> handleWeeklyRoomSwap(
            @PathVariable UUID swapConfigId,
            Principal principal) {
        
        String email = principal.getName();
        try {
            List<RoomChangeTransition> transitions = roomChangeService.handleWeeklyRoomSwap(swapConfigId, email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Weekly room swap executed successfully",
                "swapConfigId", swapConfigId,
                "transitions", transitions.stream().map(t -> Map.of(
                    "transitionId", t.getId(),
                    "sessionId", t.getSession().getId(),
                    "originalRoomId", t.getOriginalRoomId(),
                    "newRoomId", t.getNewRoomId()
                )).toList(),
                "totalTransitions", transitions.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to execute weekly room swap: " + e.getMessage()));
        }
    }
    
    /**
     * Create weekly room swap configuration
     * POST /api/v1/room-change/weekly-swap-config
     */
    @PostMapping("/weekly-swap-config")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createWeeklySwapConfig(
            @Valid @RequestBody WeeklyRoomSwapConfig config,
            Principal principal) {
        
        String email = principal.getName();
        try {
            WeeklyRoomSwap swapConfig = roomChangeService.createWeeklySwapConfig(config, email);
            
            return ResponseEntity.status(201).body(Map.of(
                "message", "Weekly room swap configuration created successfully",
                "swapConfigId", swapConfig.getId(),
                "originalRoomId", swapConfig.getOriginalRoom().getId(),
                "newRoomId", swapConfig.getNewRoom().getId(),
                "swapDate", swapConfig.getSwapDate(),
                "reason", swapConfig.getReason(),
                "approvedBy", swapConfig.getApprovedBy() != null ? swapConfig.getApprovedBy().getId() : null,
                "approvedAt", swapConfig.getApprovedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create weekly swap configuration: " + e.getMessage()));
        }
    }
    
    /**
     * Check if student is in grace period
     * GET /api/v1/room-change/grace-period/{studentId}/{sessionId}
     */
    @GetMapping("/grace-period/{studentId}/{sessionId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'STUDENT')")
    public ResponseEntity<Map<String, Object>> checkGracePeriod(
            @PathVariable UUID studentId,
            @PathVariable UUID sessionId) {
        
        try {
            boolean inGracePeriod = roomChangeService.isStudentInGracePeriod(studentId, sessionId);
            
            return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "sessionId", sessionId,
                "inGracePeriod", inGracePeriod,
                "gracePeriodMinutes", 15,
                "message", inGracePeriod ? 
                    "Student is in grace period for room transition" : 
                    "Student is not in grace period"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to check grace period: " + e.getMessage()));
        }
    }
    
    /**
     * Get all active transitions (for monitoring)
     * GET /api/v1/room-change/active-transitions
     */
    @GetMapping("/active-transitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getActiveTransitions() {
        
        try {
            List<RoomChangeTransition> transitions = roomChangeService.getAllActiveTransitions();
            
            return ResponseEntity.ok(Map.of(
                "activeTransitions", transitions,
                "totalActive", transitions.size(),
                "timestamp", java.time.Instant.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get active transitions: " + e.getMessage()));
        }
    }
    
    /**
     * Quick room change for faculty (simplified)
     * POST /api/v1/room-change/quick
     */
    @PostMapping("/quick")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Map<String, Object>> quickRoomChange(
            @RequestParam UUID roomId,
            @RequestParam UUID sectionId,
            @RequestParam(required = false, defaultValue = "Sudden room change") String reason,
            Principal principal) {
        
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            QRRoomChangeRequest request = new QRRoomChangeRequest(
                roomId, currentUser.getId(), sectionId, reason, false, true, true, false
            );
            
            RoomChangeTransition transition = roomChangeService.handleQRRoomChange(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Quick room change processed successfully",
                "transitionId", transition.getId(),
                "roomId", roomId,
                "sectionId", sectionId,
                "gracePeriodMinutes", transition.getGracePeriodMinutes()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to process quick room change: " + e.getMessage()));
        }
    }
}
