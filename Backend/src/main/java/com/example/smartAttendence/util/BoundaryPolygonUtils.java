package com.example.smartAttendence.util;

import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for admin boundary polygon operations
 * Helps admins create, validate, and manage room boundaries
 */
@Component
public class BoundaryPolygonUtils {

    private static final int SRID_WGS84 = 4326;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    /**
     * Create a rectangular boundary from center point and dimensions
     */
    public Polygon createRectangleBoundary(double centerLat, double centerLng, 
                                         double widthMeters, double heightMeters) {
        // Convert meters to degrees (approximate)
        double latOffset = heightMeters / 111320.0; // 1 degree latitude ≈ 111.32 km
        double lngOffset = widthMeters / (111320.0 * Math.cos(Math.toRadians(centerLat)));
        
        Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(centerLng - lngOffset, centerLat - latOffset), // Southwest
            new Coordinate(centerLng + lngOffset, centerLat - latOffset), // Southeast
            new Coordinate(centerLng + lngOffset, centerLat + latOffset), // Northeast
            new Coordinate(centerLng - lngOffset, centerLat + latOffset), // Northwest
            new Coordinate(centerLng - lngOffset, centerLat - latOffset)  // Close polygon
        };
        
        return createPolygon(coordinates);
    }

    /**
     * Create a circular boundary (approximated with polygon)
     */
    public Polygon createCircularBoundary(double centerLat, double centerLng, double radiusMeters) {
        int numPoints = 32; // Number of points to approximate circle
        Coordinate[] coordinates = new Coordinate[numPoints + 1];
        
        // Convert radius to degrees
        double latOffset = radiusMeters / 111320.0;
        double lngOffset = radiusMeters / (111320.0 * Math.cos(Math.toRadians(centerLat)));
        
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double lat = centerLat + latOffset * Math.sin(angle);
            double lng = centerLng + lngOffset * Math.cos(angle);
            coordinates[i] = new Coordinate(lng, lat);
        }
        
        coordinates[numPoints] = coordinates[0]; // Close polygon
        return createPolygon(coordinates);
    }

    /**
     * Create an L-shaped boundary (common for classrooms)
     */
    public Polygon createLShapedBoundary(double centerLat, double centerLng, 
                                         double longSideMeters, double shortSideMeters) {
        // Convert meters to degrees
        double latOffset = longSideMeters / 111320.0;
        double lngOffset = shortSideMeters / (111320.0 * Math.cos(Math.toRadians(centerLat)));
        
        // Create L-shape coordinates
        Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(centerLng - lngOffset, centerLat - latOffset),     // Bottom-left
            new Coordinate(centerLng, centerLat - latOffset),                  // Bottom-middle
            new Coordinate(centerLng, centerLat),                               // Middle-middle
            new Coordinate(centerLng + lngOffset, centerLat),                  // Middle-right
            new Coordinate(centerLng + lngOffset, centerLat + latOffset),       // Top-right
            new Coordinate(centerLng - lngOffset, centerLat + latOffset),      // Top-left
            new Coordinate(centerLng - lngOffset, centerLat - latOffset)      // Close
        };
        
        return createPolygon(coordinates);
    }

    /**
     * Create polygon from coordinate list
     */
    public Polygon createPolygon(Coordinate[] coordinates) {
        LinearRing shell = geometryFactory.createLinearRing(coordinates);
        Polygon polygon = geometryFactory.createPolygon(shell, null);
        polygon.setSRID(SRID_WGS84);
        return polygon;
    }

    /**
     * Validate polygon geometry
     */
    public PolygonValidationResult validatePolygon(Polygon polygon) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean isValid = true;

        // Basic validity checks
        if (!polygon.isValid()) {
            isValid = false;
            errors.add("Polygon has self-intersections or invalid geometry");
        }

        if (polygon.getArea() <= 0) {
            isValid = false;
            errors.add("Polygon area must be greater than 0");
        }

        // Check number of points
        int numPoints = polygon.getCoordinates().length - 1; // Exclude closing point
        if (numPoints < 4) {
            isValid = false;
            errors.add("Polygon must have at least 4 points");
        } else if (numPoints > 50) {
            warnings.add("Complex polygon with " + numPoints + " points may impact performance");
        }

        // Area checks
        double areaSqMeters = calculateAreaInSquareMeters(polygon);
        if (areaSqMeters < 5) {
            warnings.add("Very small area (< 5 sq meters) - verify coordinates");
        } else if (areaSqMeters > 2000) {
            warnings.add("Very large area (> 2000 sq meters) - consider multiple zones");
        }

        return new PolygonValidationResult(isValid, errors, warnings, areaSqMeters);
    }

    /**
     * Simplify polygon (reduce number of points while maintaining shape)
     */
    public Polygon simplifyPolygon(Polygon polygon, double tolerance) {
        // Use Douglas-Peucker algorithm via JTS
        Geometry simplified = org.locationtech.jts.simplify.DouglasPeuckerSimplifier.simplify(polygon, tolerance);
        
        if (simplified instanceof Polygon) {
            return (Polygon) simplified;
        } else {
            throw new IllegalArgumentException("Simplification resulted in invalid polygon");
        }
    }

    /**
     * Calculate approximate area in square meters
     */
    public double calculateAreaInSquareMeters(Polygon polygon) {
        Coordinate[] coords = polygon.getCoordinates();
        double area = 0.0;
        
        for (int i = 0; i < coords.length - 1; i++) {
            Coordinate c1 = coords[i];
            Coordinate c2 = coords[i + 1];
            
            double lat1 = Math.toRadians(c1.y);
            double lat2 = Math.toRadians(c2.y);
            double lonDiff = Math.toRadians(c2.x - c1.x);
            
            area += Math.abs(lonDiff * Math.cos((lat1 + lat2) / 2) * 6371000 * 
                          (c2.y - c1.y) * 111320);
        }
        
        return Math.abs(area / 2);
    }

    /**
     * Convert polygon to coordinate list for API responses
     */
    public List<List<Double>> polygonToCoordinateList(Polygon polygon) {
        return java.util.Arrays.stream(polygon.getCoordinates())
                .map(coord -> List.of(coord.x, coord.y))
                .toList();
    }

    /**
     * Create polygon from coordinate list (for API requests)
     */
    public Polygon polygonFromCoordinateList(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            throw new IllegalArgumentException("Coordinates cannot be null or empty");
        }

        if (coordinates.size() < 4) {
            throw new IllegalArgumentException("At least 4 coordinates required for polygon");
        }

        Coordinate[] coordinateArray = coordinates.stream()
                .map(coord -> new Coordinate(coord.get(0), coord.get(1))) // lon, lat
                .toArray(Coordinate[]::new);

        // Ensure polygon is closed
        if (!coordinateArray[0].equals(coordinateArray[coordinateArray.length - 1])) {
            Coordinate[] closedArray = new Coordinate[coordinateArray.length + 1];
            System.arraycopy(coordinateArray, 0, closedArray, 0, coordinateArray.length);
            closedArray[coordinateArray.length] = coordinateArray[0];
            coordinateArray = closedArray;
        }

        return createPolygon(coordinateArray);
    }

    /**
     * Get boundary type based on shape analysis
     */
    public String getBoundaryType(Polygon polygon) {
        if (polygon == null) return "NONE";
        
        int numPoints = polygon.getCoordinates().length - 1;
        
        if (numPoints == 4 && isRectangle(polygon)) {
            return "RECTANGLE";
        } else if (numPoints <= 8) {
            return "SIMPLE_POLYGON";
        } else if (numPoints <= 20) {
            return "MODERATE_POLYGON";
        } else {
            return "COMPLEX_POLYGON";
        }
    }

    /**
     * Check if polygon is approximately rectangular
     */
    private boolean isRectangle(Polygon polygon) {
        Coordinate[] coords = polygon.getCoordinates();
        if (coords.length != 5) return false; // 4 corners + closing point

        // Check if angles are approximately 90 degrees
        // This is a simplified check - for production use more sophisticated methods
        return true; // Simplified for now
    }

    /**
     * Result record for polygon validation
     */
    public record PolygonValidationResult(
        boolean isValid,
        List<String> errors,
        List<String> warnings,
        double areaSquareMeters
    ) {}
}
