package com.example.smartAttendence.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExamBarcodeScanRequest(
    @NotNull(message = "Session ID is required")
    UUID sessionId,
    
    @NotBlank(message = "Barcode data is required")
    String barcodeData,
    
    @NotNull(message = "Scan timestamp is required")
    LocalDateTime scanTime,
    
    String deviceFingerprint
) {}
