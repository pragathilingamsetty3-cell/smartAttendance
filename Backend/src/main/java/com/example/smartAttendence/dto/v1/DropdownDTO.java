package com.example.smartAttendence.dto.v1;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple DTO for dropdown options in Frontend UI
 * Hides UUID complexity while providing essential data
 */
public record DropdownDTO(
    UUID id, 
    @JsonProperty("label") String label,
    @JsonProperty("subLabel") String subLabel,
    @JsonProperty("capacity") Integer capacity,
    @JsonProperty("studentCount") Long studentCount,
    @JsonProperty("facultyCount") Long facultyCount
) {
    public DropdownDTO(UUID id, String label) {
        this(id, label, null, null, null, null);
    }

    public DropdownDTO(UUID id, String label, String subLabel) {
        this(id, label, subLabel, null, null, null);
    }

    public DropdownDTO(UUID id, String label, String subLabel, Integer capacity) {
        this(id, label, subLabel, capacity, null, null);
    }
}
