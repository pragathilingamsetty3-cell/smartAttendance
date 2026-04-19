package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ImagePointDTO {
    
    @NotNull(message = "X coordinate is required")
    @Min(value = 0, message = "X coordinate must be >= 0")
    @Max(value = 1, message = "X coordinate must be <= 1 (normalized)")
    private Double x;
    
    @NotNull(message = "Y coordinate is required")
    @Min(value = 0, message = "Y coordinate must be >= 0")
    @Max(value = 1, message = "Y coordinate must be <= 1 (normalized)")
    private Double y;

    public ImagePointDTO() {}

    public ImagePointDTO(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }
}
