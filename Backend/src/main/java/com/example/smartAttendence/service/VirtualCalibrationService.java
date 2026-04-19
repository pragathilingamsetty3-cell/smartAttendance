package com.example.smartAttendence.service;

import com.example.smartAttendence.dto.v1.CoordinateDTO;
import com.example.smartAttendence.dto.v1.ImageCalibrationRequest;
import com.example.smartAttendence.dto.v1.ImagePointDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual Calibration Service (The "Calibration Bridge")
 * Maps 2D pixel coordinates from an image constraint to 3D/Geographic Map Coordinates
 * using Camera Perspective Ray-Casting.
 */
@Service
public class VirtualCalibrationService {

    private static final double EARTH_RADIUS = 6371000.0; // in meters

    /**
     * Translates 4 image pixels into 4 Geographic Coordinates.
     */
    public List<CoordinateDTO> calculateBoundaryCoordinates(ImageCalibrationRequest request) {
        List<CoordinateDTO> boundary = new ArrayList<>();

        double baseLat = request.getBaseLatitude();
        double baseLon = request.getBaseLongitude();
        double heading = request.getHeading(); // 0 is North, 90 is East
        double pitch = request.getPitch(); // Tilt downwards. 0 is looking straight forward, 90 is looking straight down
        double height = request.getCameraHeight();
        double vfov = request.getFov();

        // Assume standard 16:9 mobile aspect ratio for horizontal FOV approximation
        double hfov = vfov * (16.0 / 9.0);

        for (ImagePointDTO point : request.getPoints()) {
            // 1. Calculate angles relative to the camera's center
            // point.getX() and point.getY() are normalized between 0.0 and 1.0 (Top-Left is 0,0)
            double angleY = (point.getY() - 0.5) * vfov; // Positive means looking further downwards from center
            double angleX = (point.getX() - 0.5) * hfov; // Positive means looking right from center

            // 2. Absolute Pitch (downward angle) for this specific pixel ray
            double rayPitch = pitch + angleY;

            // Clamp ray pitch to prevent infinite distances or backwards rays
            if (rayPitch < 1.0) {
                rayPitch = 1.0; // Max out at a very far distance instead of looking at the horizon/ceiling
            } else if (rayPitch >= 90.0) {
                rayPitch = 89.9; // Practically directly underneath
            }

            // 3. Absolute Heading for this specific pixel ray
            double rayHeading = heading + angleX;

            // Normalize heading to 0-360
            rayHeading = (rayHeading % 360 + 360) % 360;

            // 4. Calculate Ground Distance using Trigonometry
            // tan(pitch) = Opposite (Height) / Adjacent (Ground Distance)
            // Therefore, Ground Distance = Height / tan(pitch)
            double groundDistance = height / Math.tan(Math.toRadians(rayPitch));

            // 5. Convert Ground Distance and Heading to Geographic Coordinates
            CoordinateDTO coord = calculateDestinationCoordinate(baseLat, baseLon, rayHeading, groundDistance);
            boundary.add(coord);
        }

        return boundary;
    }

    /**
     * Calculates the destination coordinate given a starting coordinate, true bearing, and distance over the Earth.
     * Uses Haversine/Spherical Earth Model.
     */
    private CoordinateDTO calculateDestinationCoordinate(double lat, double lon, double bearingDegrees, double distanceMeters) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double bearingRad = Math.toRadians(bearingDegrees);
        double angularDistance = distanceMeters / EARTH_RADIUS;

        double destLatRad = Math.asin(
            Math.sin(latRad) * Math.cos(angularDistance) +
            Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        );

        double destLonRad = lonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
            Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(destLatRad)
        );

        // Normalize longitude
        destLonRad = (destLonRad + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new CoordinateDTO(Math.toDegrees(destLatRad), Math.toDegrees(destLonRad));
    }
}
