package com.example.smartAttendence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "boundary_polygon", nullable = false, columnDefinition = "geometry(Polygon,4326)")
    private Polygon boundaryPolygon;

    @Column(nullable = false)
    private String building;

    @Column(nullable = false)
    private Integer floor;

    private String description;

    @Column(name = "sensor_url")
    private String sensorUrl;

    // Explicit getters and setters for Lombok compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public Polygon getBoundaryPolygon() { return boundaryPolygon; }
    public void setBoundaryPolygon(Polygon boundaryPolygon) { this.boundaryPolygon = boundaryPolygon; }
    
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSensorUrl() { return sensorUrl; }
    public void setSensorUrl(String sensorUrl) { this.sensorUrl = sensorUrl; }
}
