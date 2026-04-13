package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class RoomCreationRequest {
    
    @NotBlank(message = "Room name is required")
    private String name;
    
    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;
    
    @NotBlank(message = "Building is required")
    private String building;
    
    @NotNull(message = "Floor is required")
    private Integer floor;
    
    @NotNull(message = "Boundary points are required")
    private List<CoordinateDTO> boundaryPoints;
    
    private String description;
    
    private String sensorUrl;

    public RoomCreationRequest() {}

    public RoomCreationRequest(String name, Integer capacity, String building, Integer floor, List<CoordinateDTO> boundaryPoints, String description, String sensorUrl) {
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.floor = floor;
        this.boundaryPoints = boundaryPoints;
        this.description = description;
        this.sensorUrl = sensorUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public List<CoordinateDTO> getBoundaryPoints() {
        return boundaryPoints;
    }

    public void setBoundaryPoints(List<CoordinateDTO> boundaryPoints) {
        this.boundaryPoints = boundaryPoints;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSensorUrl() {
        return sensorUrl;
    }

    public void setSensorUrl(String sensorUrl) {
        this.sensorUrl = sensorUrl;
    }
}
