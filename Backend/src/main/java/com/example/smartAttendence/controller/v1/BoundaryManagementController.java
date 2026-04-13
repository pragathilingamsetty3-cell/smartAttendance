package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.util.BoundaryPolygonUtils;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Boundary Management Controller
 * Provides utilities for admins to create and validate room boundaries
 */
@RestController
@RequestMapping("/api/v1/admin/boundaries")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class BoundaryManagementController {

    @Autowired
    private BoundaryPolygonUtils boundaryUtils;

    /**
     * Create rectangular boundary from center point
     * POST /api/v1/admin/boundaries/rectangle
     */
    @PostMapping("/rectangle")
    public ResponseEntity<Map<String, Object>> createRectangleBoundary(@RequestBody Map<String, Object> request) {
        try {
            double centerLat = ((Number) request.get("centerLat")).doubleValue();
            double centerLng = ((Number) request.get("centerLng")).doubleValue();
            double widthMeters = ((Number) request.get("widthMeters")).doubleValue();
            double heightMeters = ((Number) request.get("heightMeters")).doubleValue();

            Polygon rectangle = boundaryUtils.createRectangleBoundary(centerLat, centerLng, widthMeters, heightMeters);
            var validation = boundaryUtils.validatePolygon(rectangle);

            return ResponseEntity.ok(Map.of(
                "message", "Rectangular boundary created successfully",
                "boundaryType", boundaryUtils.getBoundaryType(rectangle),
                "coordinates", boundaryUtils.polygonToCoordinateList(rectangle),
                "area", validation.areaSquareMeters(),
                "isValid", validation.isValid(),
                "warnings", validation.warnings()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create rectangle: " + e.getMessage()));
        }
    }

    /**
     * Create circular boundary from center point
     * POST /api/v1/admin/boundaries/circle
     */
    @PostMapping("/circle")
    public ResponseEntity<Map<String, Object>> createCircularBoundary(@RequestBody Map<String, Object> request) {
        try {
            double centerLat = ((Number) request.get("centerLat")).doubleValue();
            double centerLng = ((Number) request.get("centerLng")).doubleValue();
            double radiusMeters = ((Number) request.get("radiusMeters")).doubleValue();

            Polygon circle = boundaryUtils.createCircularBoundary(centerLat, centerLng, radiusMeters);
            var validation = boundaryUtils.validatePolygon(circle);

            return ResponseEntity.ok(Map.of(
                "message", "Circular boundary created successfully",
                "boundaryType", boundaryUtils.getBoundaryType(circle),
                "coordinates", boundaryUtils.polygonToCoordinateList(circle),
                "area", validation.areaSquareMeters(),
                "isValid", validation.isValid(),
                "warnings", validation.warnings()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create circle: " + e.getMessage()));
        }
    }

    /**
     * Create L-shaped boundary (for L-shaped rooms)
     * POST /api/v1/admin/boundaries/l-shape
     */
    @PostMapping("/l-shape")
    public ResponseEntity<Map<String, Object>> createLShapedBoundary(@RequestBody Map<String, Object> request) {
        try {
            double centerLat = ((Number) request.get("centerLat")).doubleValue();
            double centerLng = ((Number) request.get("centerLng")).doubleValue();
            double longSideMeters = ((Number) request.get("longSideMeters")).doubleValue();
            double shortSideMeters = ((Number) request.get("shortSideMeters")).doubleValue();

            Polygon lShape = boundaryUtils.createLShapedBoundary(centerLat, centerLng, longSideMeters, shortSideMeters);
            var validation = boundaryUtils.validatePolygon(lShape);

            return ResponseEntity.ok(Map.of(
                "message", "L-shaped boundary created successfully",
                "boundaryType", boundaryUtils.getBoundaryType(lShape),
                "coordinates", boundaryUtils.polygonToCoordinateList(lShape),
                "area", validation.areaSquareMeters(),
                "isValid", validation.isValid(),
                "warnings", validation.warnings()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create L-shape: " + e.getMessage()));
        }
    }

    /**
     * Validate custom boundary from coordinates
     * POST /api/v1/admin/boundaries/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateBoundary(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");

            Polygon polygon = boundaryUtils.polygonFromCoordinateList(coordinates);
            var validation = boundaryUtils.validatePolygon(polygon);

            return ResponseEntity.ok(Map.of(
                "message", "Boundary validation completed",
                "isValid", validation.isValid(),
                "boundaryType", boundaryUtils.getBoundaryType(polygon),
                "area", validation.areaSquareMeters(),
                "errors", validation.errors(),
                "warnings", validation.warnings()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to validate boundary: " + e.getMessage()));
        }
    }

    /**
     * Simplify complex boundary
     * POST /api/v1/admin/boundaries/simplify
     */
    @PostMapping("/simplify")
    public ResponseEntity<Map<String, Object>> simplifyBoundary(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");
            double tolerance = request.containsKey("tolerance") ? 
                ((Number) request.get("tolerance")).doubleValue() : 0.00001;

            Polygon original = boundaryUtils.polygonFromCoordinateList(coordinates);
            Polygon simplified = boundaryUtils.simplifyPolygon(original, tolerance);
            var validation = boundaryUtils.validatePolygon(simplified);

            return ResponseEntity.ok(Map.of(
                "message", "Boundary simplified successfully",
                "originalPoints", coordinates.size(),
                "simplifiedPoints", boundaryUtils.polygonToCoordinateList(simplified).get(0).size(),
                "boundaryType", boundaryUtils.getBoundaryType(simplified),
                "area", validation.areaSquareMeters(),
                "isValid", validation.isValid(),
                "warnings", validation.warnings(),
                "simplifiedCoordinates", boundaryUtils.polygonToCoordinateList(simplified)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to simplify boundary: " + e.getMessage()));
        }
    }

    /**
     * Get boundary information and statistics
     * POST /api/v1/admin/boundaries/info
     */
    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> getBoundaryInfo(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");

            Polygon polygon = boundaryUtils.polygonFromCoordinateList(coordinates);
            var validation = boundaryUtils.validatePolygon(polygon);

            // Calculate additional statistics
            double perimeter = polygon.getLength();
            int numPoints = polygon.getCoordinates().length - 1;
            String boundaryType = boundaryUtils.getBoundaryType(polygon);

            return ResponseEntity.ok(Map.of(
                "message", "Boundary information retrieved",
                "boundaryType", boundaryType,
                "numPoints", numPoints,
                "area", validation.areaSquareMeters(),
                "perimeter", perimeter,
                "isValid", validation.isValid(),
                "errors", validation.errors(),
                "warnings", validation.warnings(),
                "recommendations", getRecommendations(validation, boundaryType, numPoints)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get boundary info: " + e.getMessage()));
        }
    }

    /**
     * Get recommendations based on boundary analysis
     */
    private List<String> getRecommendations(BoundaryPolygonUtils.PolygonValidationResult validation, 
                                           String boundaryType, int numPoints) {
        List<String> recommendations = new java.util.ArrayList<>();

        if (!validation.isValid()) {
            recommendations.add("Fix polygon geometry errors before using");
            recommendations.add("Ensure polygon is closed and has no self-intersections");
        }

        if (validation.areaSquareMeters() < 10) {
            recommendations.add("Verify room dimensions - area seems very small");
        } else if (validation.areaSquareMeters() > 1000) {
            recommendations.add("Consider splitting large area into multiple zones");
        }

        if (numPoints > 30) {
            recommendations.add("Simplify polygon for better performance");
            recommendations.add("Use boundary simplification tool");
        }

        if ("COMPLEX_POLYGON".equals(boundaryType)) {
            recommendations.add("Complex boundaries may impact GPS accuracy");
            recommendations.add("Consider using simpler shape if possible");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Boundary looks good - ready to use");
        }

        return recommendations;
    }
}
