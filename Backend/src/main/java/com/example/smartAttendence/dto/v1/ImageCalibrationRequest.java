package com.example.smartAttendence.dto.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ImageCalibrationRequest {

    @NotNull(message = "Base latitude is required")
    private Double baseLatitude;

    @NotNull(message = "Base longitude is required")
    private Double baseLongitude;

    @NotNull(message = "Camera heading (compass direction) is required")
    private Double heading;

    @NotNull(message = "Camera pitch (tilt angle) is required")
    private Double pitch;

    private Double cameraHeight = 1.5; // Default height in meters

    private Double fov = 60.0; // Default vertical field of view in degrees

    @Valid
    @NotNull(message = "Pinned points are required")
    @Size(min = 4, max = 4, message = "Exactly 4 pinned points are required to define the boundary")
    private List<ImagePointDTO> points;

    public ImageCalibrationRequest() {}

    public Double getBaseLatitude() {
        return baseLatitude;
    }

    public void setBaseLatitude(Double baseLatitude) {
        this.baseLatitude = baseLatitude;
    }

    public Double getBaseLongitude() {
        return baseLongitude;
    }

    public void setBaseLongitude(Double baseLongitude) {
        this.baseLongitude = baseLongitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getPitch() {
        return pitch;
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    public Double getCameraHeight() {
        return cameraHeight;
    }

    public void setCameraHeight(Double cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    public Double getFov() {
        return fov;
    }

    public void setFov(Double fov) {
        this.fov = fov;
    }

    public List<ImagePointDTO> getPoints() {
        return points;
    }

    public void setPoints(List<ImagePointDTO> points) {
        this.points = points;
    }
}
