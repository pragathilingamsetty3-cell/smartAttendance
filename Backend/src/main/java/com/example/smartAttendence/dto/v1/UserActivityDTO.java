package com.example.smartAttendence.dto.v1;

import java.time.Instant;
import java.util.UUID;

public record UserActivityDTO(
    UUID id,
    String type, // ATTENDANCE, SECURITY, ADMIN
    String action, // PRESENT, LATE, WALK_OUT, DEVICE_RESET, STATUS_CHANGE
    String description,
    Instant timestamp,
    String metadata // Session name, Room, etc.
) {}
