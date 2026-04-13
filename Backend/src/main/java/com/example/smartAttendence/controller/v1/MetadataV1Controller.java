package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.service.v1.MetadataV1Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/metadata")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class MetadataV1Controller {

    private final MetadataV1Service metadataV1Service;

    public MetadataV1Controller(MetadataV1Service metadataV1Service) {
        this.metadataV1Service = metadataV1Service;
    }

    /**
     * GET /api/v1/admin/metadata/semesters
     * Get distinct semesters from the database.
     */
    @GetMapping("/semesters")
    public ResponseEntity<List<String>> getSemesters() {
        return ResponseEntity.ok(metadataV1Service.getDistinctSemesters());
    }

    /**
     * GET /api/v1/admin/metadata/academic-years
     * Get distinct academic years from the database.
     */
    @GetMapping("/academic-years")
    public ResponseEntity<List<String>> getAcademicYears() {
        return ResponseEntity.ok(metadataV1Service.getDistinctAcademicYears());
    }

    /**
     * GET /api/v1/admin/metadata/roles
     * Get available roles for user management.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getRoles() {
        return ResponseEntity.ok(metadataV1Service.getAvailableRoles());
    }
    
    /**
     * GET /api/v1/admin/metadata/all
     * Combined metadata endpoint for performance.
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<String>>> getAllMetadata() {
        return ResponseEntity.ok(Map.of(
            "semesters", metadataV1Service.getDistinctSemesters(),
            "academicYears", metadataV1Service.getDistinctAcademicYears(),
            "roles", metadataV1Service.getAvailableRoles()
        ));
    }
}
