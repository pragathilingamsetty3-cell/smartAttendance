package com.example.smartAttendence.service;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.entity.SensorReading;
import com.example.smartAttendence.repository.SensorReadingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SensorFusionService {

    private final SensorReadingRepository sensorReadingRepository;

    public SensorFusionService(SensorReadingRepository sensorReadingRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
    }

    /**
     * Enhanced sensor fusion for GPS spoofing detection
     */
    public boolean detectSpoofing(List<SensorReading> readings) {
        // Simple spoofing detection logic
        if (readings.isEmpty()) return false;
        
        // Check for unrealistic GPS jumps
        return readings.stream()
                .anyMatch(reading -> isUnrealisticMovement(reading));
    }

    /**
     * Calculate acceleration magnitude for AI analysis
     */
    public double calculateAccelerationMagnitude(SensorReading reading) {
        double ax = reading.getAccelerationX();
        double ay = reading.getAccelerationY();
        double az = reading.getAccelerationZ();
        
        return Math.sqrt(ax * ax + ay * ay + az * az);
    }

    /**
     * Detect GPS drift vs actual movement
     */
    public boolean detectGPSDrift(List<SensorReading> readings) {
        if (readings.size() < 2) return false;
        
        // Simple drift detection based on sensor consistency
        return readings.stream()
                .filter(this::isUnrealisticMovement)
                .count() > readings.size() * 0.5; // More than 50% unrealistic readings
    }

    /**
     * Process enhanced heartbeat with sensor fusion
     */
    public void processEnhancedHeartbeat(EnhancedHeartbeatPing ping) {
        // Save sensor reading for AI analysis
        SensorReading reading = new SensorReading();
        reading.setStudentId(ping.studentId());
        reading.setSessionId(ping.sessionId());
        reading.setLatitude(ping.latitude());
        reading.setLongitude(ping.longitude());
        reading.setAccelerationX(ping.accelerationX());
        reading.setAccelerationY(ping.accelerationY());
        reading.setAccelerationZ(ping.accelerationZ());
        reading.setReadingTimestamp(Instant.now());
        
        sensorReadingRepository.save(reading);
    }

    /**
     * Get recent sensor readings for analysis
     */
    public List<SensorReading> getRecentReadings(UUID studentId, UUID sessionId, int limit) {
        // 🏎️ PERFORMANCE: Using optimized repository method instead of findAll().stream()
        // This prevents full table scans and memory crashes as the database grows.
        return sensorReadingRepository.findTop10ByStudentIdAndSessionIdOrderByReadingTimestampDesc(studentId, sessionId);
    }

    /**
     * Calculate motion state from heartbeat data
     */
    public String calculateMotionState(EnhancedHeartbeatPing ping) {
        double accelerationMagnitude = Math.sqrt(
            ping.accelerationX() * ping.accelerationX() +
            ping.accelerationY() * ping.accelerationY() +
            ping.accelerationZ() * ping.accelerationZ()
        );

        if (accelerationMagnitude > 15.0) {
            return "RUNNING";
        } else if (accelerationMagnitude > 5.0) {
            return "WALKING";
        } else if (accelerationMagnitude > 1.5) {
            return "STANDING";
        } else {
            return "STILL";
        }
    }

    // 🛰️ PHASE 2: ADAPTIVE GPS OPTIMIZATION
    
    /**
     * Determine optimal GPS mode based on device state and context
     */
    public GPSModeResult determineOptimalGPSMode(EnhancedHeartbeatPing ping) {
        String deviceState = calculateMotionState(ping);
        GPSMode gpsMode;
        String reason;
        double accuracyMeters;
        long updateIntervalMs;

        // Base GPS mode on device state
        switch (deviceState) {
            case "STILL":
                gpsMode = GPSMode.NO_GPS;
                accuracyMeters = 0.0;
                updateIntervalMs = 300000; // 5 minutes
                reason = "Device stationary - no GPS needed";
                break;
                
            case "STANDING":
                gpsMode = GPSMode.LOW_POWER;
                accuracyMeters = 100.0;
                updateIntervalMs = 120000; // 2 minutes
                reason = "Standing - low power GPS sufficient";
                break;
                
            case "WALKING":
                gpsMode = GPSMode.BALANCED;
                accuracyMeters = 20.0;
                updateIntervalMs = 30000; // 30 seconds
                reason = "Walking - balanced GPS mode";
                break;
                
            case "RUNNING":
                gpsMode = GPSMode.HIGH_ACCURACY;
                accuracyMeters = 5.0;
                updateIntervalMs = 10000; // 10 seconds
                reason = "Running - high accuracy GPS needed";
                break;
                
            default:
                gpsMode = GPSMode.BALANCED;
                accuracyMeters = 20.0;
                updateIntervalMs = 30000;
                reason = "Default - balanced GPS mode";
        }

        // Adjust for battery level
        if (ping.batteryLevel() != null && ping.batteryLevel() < 20) {
            gpsMode = GPSMode.EMERGENCY;
            accuracyMeters = 200.0;
            updateIntervalMs = 600000; // 10 minutes
            reason = "Low battery - emergency GPS mode";
        }

        // Adjust for charging status
        if (Boolean.TRUE.equals(ping.isCharging())) {
            // Can use higher accuracy when charging
            if (gpsMode == GPSMode.LOW_POWER) {
                gpsMode = GPSMode.BALANCED;
                reason = "Charging - upgraded to balanced GPS";
            }
        }

        return new GPSModeResult(
            gpsMode,
            reason,
            accuracyMeters,
            updateIntervalMs,
            deviceState
        );
    }

    /**
     * Check if high accuracy GPS is needed (geofence transition)
     */
    public boolean needsHighAccuracyGPS(EnhancedHeartbeatPing ping, List<SensorReading> recentReadings) {
        if (recentReadings.isEmpty()) {
            return true; // First reading needs high accuracy
        }

        SensorReading lastReading = recentReadings.get(0);
        
        // Check if near geofence boundary (within 50 meters)
        double distanceFromLast = calculateDistance(
            ping.latitude(), ping.longitude(),
            lastReading.getLatitude(), lastReading.getLongitude()
        );

        // If moved more than 50 meters, need high accuracy
        return distanceFromLast > 50.0;
    }

    /**
     * Calculate distance between two GPS coordinates in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    // GPS Mode enum and result record
    public enum GPSMode {
        NO_GPS,          // No GPS updates
        LOW_POWER,        // Low power GPS (~100m accuracy)
        BALANCED,         // Balanced GPS (~20m accuracy)  
        HIGH_ACCURACY,   // High accuracy GPS (~5m accuracy)
        EMERGENCY         // Emergency GPS (~200m accuracy)
    }

    public record GPSModeResult(
        GPSMode mode,
        String reason,
        double accuracyMeters,
        long updateIntervalMs,
        String deviceState
    ) {}

    private boolean isUnrealisticMovement(SensorReading reading) {
        // Check for unrealistic GPS coordinates or acceleration
        return reading.getLatitude() == 0 || 
               reading.getLongitude() == 0 ||
               Math.abs(calculateAccelerationMagnitude(reading)) > 20.0; // 20g threshold
    }

    public record SpoofingDetectionResult(
        boolean isSpoofing,
        double confidence,
        String reason
    ) {}
}
