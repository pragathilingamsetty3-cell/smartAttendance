package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WeeklyRoomSwapConfig(
    @NotNull(message = "Section 1 ID is required")
    UUID section1Id,
    
    @NotNull(message = "Section 2 ID is required")
    UUID section2Id,
    
    @NotNull(message = "Room 1 ID is required")
    UUID room1Id,
    
    @NotNull(message = "Room 2 ID is required")
    UUID room2Id,
    
    // Swap schedule
    SwapFrequency frequency,
    
    // Swap timing
    Instant nextSwapTime,
    
    // Active flag
    Boolean active,
    
    // Configuration details
    String description,
    
    // Auto-swap or manual confirmation
    Boolean autoSwap,
    
    // Notification preferences
    Boolean notifyBeforeSwap,
    Integer notifyBeforeMinutes,
    Boolean notifyOnSwap
) {
    public enum SwapFrequency {
        WEEKLY,
        BI_WEEKLY,
        MONTHLY
    }
    
    public WeeklyRoomSwapConfig {
        if (active == null) active = true;
        if (autoSwap == null) autoSwap = false;
        if (notifyBeforeSwap == null) notifyBeforeSwap = true;
        if (notifyBeforeMinutes == null) notifyBeforeMinutes = 30;
        if (notifyOnSwap == null) notifyOnSwap = true;
    }
}
