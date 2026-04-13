package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.DeviceRegistrationRequest;
import com.example.smartAttendence.dto.v1.BiometricValidationRequest;
import com.example.smartAttendence.dto.v1.OfflineSyncRequest;
import com.example.smartAttendence.service.v1.DeviceV1Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceV1Controller {

    private final DeviceV1Service deviceV1Service;

    public DeviceV1Controller(DeviceV1Service deviceV1Service) {
        this.deviceV1Service = deviceV1Service;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        Map<String, Object> result = deviceV1Service.registerDevice(request, ipAddress);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-biometric")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> validateBiometric(
            @Valid @RequestBody BiometricValidationRequest request
    ) {
        Map<String, Object> result = deviceV1Service.validateBiometric(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync-offline")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> syncOfflineAttendance(
            @Valid @RequestBody OfflineSyncRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        Map<String, Object> result = deviceV1Service.syncOfflineAttendance(request, ipAddress);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getDeviceStatus() {
        Map<String, Object> result = deviceV1Service.getDeviceStatus();
        return ResponseEntity.ok(result);
    }
}
