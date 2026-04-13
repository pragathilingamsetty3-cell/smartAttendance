package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public class StartSessionRequest {
    private UUID facultyId;
    private String subject;
    private double latitude;
    private double longitude;
    private int radiusMeters = 30;

    public StartSessionRequest() {}

    public StartSessionRequest(UUID facultyId, String subject, double latitude, double longitude, int radiusMeters) {
        this.facultyId = facultyId;
        this.subject = subject;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
    }

    public UUID getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(UUID facultyId) {
        this.facultyId = facultyId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(int radiusMeters) {
        this.radiusMeters = radiusMeters;
    }
}
