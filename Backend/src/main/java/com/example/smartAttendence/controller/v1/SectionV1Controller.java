package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.service.v1.SectionV1Service;
import com.example.smartAttendence.service.v1.AdminV1Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sections")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SectionV1Controller {

    private final SectionV1Service sectionV1Service;
    private final AdminV1Service adminV1Service;

    public SectionV1Controller(SectionV1Service sectionV1Service, AdminV1Service adminV1Service) {
        this.sectionV1Service = sectionV1Service;
        this.adminV1Service = adminV1Service;
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<Map<String, Object>> getSectionDetails(@PathVariable UUID sectionId) {
        try {
            Map<String, Object> sectionDetails = adminV1Service.getSectionDetails(sectionId);
            return ResponseEntity.ok(sectionDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve section details: " + e.getMessage()));
        }
    }
}
