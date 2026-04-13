package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.entity.SensorReading;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SpoofingDetectionAlgorithm {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double MAX_DISTANCE_JUMP_METERS = 50.0;
    private static final int MIN_STEPS_FOR_MOVEMENT = 5;
    private static final double MIN_ACCELERATION_FOR_MOVEMENT = 2.0;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * Detect GPS spoofing using sensor fusion
     */
    public SpoofingDetectionResult detectSpoofing(EnhancedHeartbeatPing current, SensorReading previous) {
        // Calculate distance between current and previous location
        double distance = calculateDistance(
                previous.getLatitude(), previous.getLongitude(),
                current.latitude(), current.longitude()
        );

        // Check for impossible movement (distance jump without sensor evidence)
        if (distance > MAX_DISTANCE_JUMP_METERS) {
            if (!hasMovementEvidence(current, previous)) {
                return new SpoofingDetectionResult(
                    true,
                    String.format("Impossible jump: %.2f meters in %.1f seconds without movement evidence", 
                                distance, getTimeDifferenceSeconds(current, previous)),
                    "HIGH"
                );
            }
        }

        // Check for GPS drift (small movements without device motion)
        if (distance > 5.0 && !current.isDeviceMoving() && current.stepCount() <= previous.getStepCount()) {
            return new SpoofingDetectionResult(
                true,
                String.format("GPS drift detected: %.2f meter movement without device motion", distance),
                    "MEDIUM"
            );
        }

        // Check for static GPS with changing step count (possible step spoofing)
        if (distance < 1.0 && current.stepCount() > previous.getStepCount() + 10) {
            return new SpoofingDetectionResult(
                true,
                String.format("Step spoofing detected: %d steps added without location change", 
                            current.stepCount() - previous.getStepCount()),
                "MEDIUM"
            );
        }

        return new SpoofingDetectionResult(false, null, null);
    }

    /**
     * Check if there's evidence of real movement
     */
    private boolean hasMovementEvidence(EnhancedHeartbeatPing current, SensorReading previous) {
        // Check if steps increased significantly
        int stepIncrease = current.stepCount() - previous.getStepCount();
        if (stepIncrease >= MIN_STEPS_FOR_MOVEMENT) {
            return true;
        }

        // Check if acceleration indicates movement
        double accelerationMagnitude = Math.sqrt(
                current.accelerationX() * current.accelerationX() +
                current.accelerationY() * current.accelerationY() +
                current.accelerationZ() * current.accelerationZ()
        );

        if (accelerationMagnitude > MIN_ACCELERATION_FOR_MOVEMENT) {
            return true;
        }

        // Check if device reports movement
        if (current.isDeviceMoving()) {
            return true;
        }

        return false;
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculate time difference in seconds between two readings
     */
    private double getTimeDifferenceSeconds(EnhancedHeartbeatPing current, SensorReading previous) {
        Instant currentTime = current.timestamp() != null ? current.timestamp() : Instant.now();
        return (currentTime.toEpochMilli() - previous.getReadingTimestamp().toEpochMilli()) / 1000.0;
    }

    /**
     * Create a Point from coordinates for spatial calculations
     */
    public Point createPoint(double latitude, double longitude) {
        Coordinate coordinate = new Coordinate(longitude, latitude); // JTS uses x=longitude, y=latitude
        return GEOMETRY_FACTORY.createPoint(coordinate);
    }

    /**
     * Check if location jump is physically possible given time and motion data
     */
    public boolean isPhysicallyPossible(double distance, double timeSeconds, int stepIncrease, boolean isMoving) {
        // Maximum human running speed: ~10 m/s
        double maxPossibleDistance = timeSeconds * 10.0;
        
        if (distance > maxPossibleDistance) {
            return false;
        }

        // If distance > 50m, need evidence of movement
        if (distance > MAX_DISTANCE_JUMP_METERS) {
            return stepIncrease >= MIN_STEPS_FOR_MOVEMENT || isMoving;
        }

        return true;
    }

    // Result record
    public record SpoofingDetectionResult(
        boolean spoofingDetected,
        String reason,
        String severity
    ) {}
}
