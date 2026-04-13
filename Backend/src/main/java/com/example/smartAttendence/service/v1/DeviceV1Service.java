package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.DeviceRegistrationRequest;
import com.example.smartAttendence.dto.v1.BiometricValidationRequest;
import com.example.smartAttendence.dto.v1.OfflineSyncRequest;
import com.example.smartAttendence.entity.DeviceBinding;
import com.example.smartAttendence.entity.OfflineAttendanceRecord;
import com.example.smartAttendence.repository.DeviceBindingRepository;
import com.example.smartAttendence.repository.OfflineAttendanceRecordRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
@Slf4j
public class DeviceV1Service {

    private final DeviceBindingRepository deviceBindingRepository;
    private final UserV1Repository userV1Repository;
    private final OfflineAttendanceRecordRepository offlineAttendanceRecordRepository;
    private final AttendanceRecordV1Repository attendanceRecordRepository;
    private final ClassroomSessionV1Repository classroomSessionRepository;

    public DeviceV1Service(
            DeviceBindingRepository deviceBindingRepository,
            UserV1Repository userV1Repository,
            OfflineAttendanceRecordRepository offlineAttendanceRecordRepository,
            AttendanceRecordV1Repository attendanceRecordRepository,
            ClassroomSessionV1Repository classroomSessionRepository) {
        this.deviceBindingRepository = deviceBindingRepository;
        this.userV1Repository = userV1Repository;
        this.offlineAttendanceRecordRepository = offlineAttendanceRecordRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.classroomSessionRepository = classroomSessionRepository;
    }

    public Map<String, Object> registerDevice(DeviceRegistrationRequest request, String ipAddress) {
        // Get current authenticated user (this would be injected from security context)
        // For now, we'll assume the user is passed or retrieved from authentication
        
        // Check if device already exists
        if (deviceBindingRepository.existsByDeviceId(request.deviceId())) {
            throw new IllegalArgumentException("Device already registered");
        }

        if (deviceBindingRepository.existsByDeviceFingerprint(request.deviceFingerprint())) {
            throw new IllegalArgumentException("Device fingerprint already exists");
        }

        // Create device binding
        DeviceBinding deviceBinding = new DeviceBinding();
        // deviceBinding.setUser(user); // Set from authentication context
        deviceBinding.setDeviceId(request.deviceId());
        deviceBinding.setDeviceFingerprint(request.deviceFingerprint());
        deviceBinding.setDeviceName("Mobile Device"); // Default device name
        deviceBinding.setDeviceType("MOBILE"); // Default device type
        deviceBinding.setRegisteredAt(Instant.now());
        deviceBinding.setIsActive(true);

        deviceBindingRepository.save(deviceBinding);

        return Map.of(
                "message", "Device registered successfully",
                "deviceId", request.deviceId(),
                "registeredAt", deviceBinding.getRegisteredAt()
        );
    }

    public Map<String, Object> validateBiometric(BiometricValidationRequest request) {
        // This would validate the biometric data against stored public key
        // For now, return a mock response
        
        return Map.of(
                "status", "VALID",
                "message", "Biometric validation successful",
                "biometricType", request.biometricType()
        );
    }

    public Map<String, Object> syncOfflineAttendance(OfflineSyncRequest request, String ipAddress) {
        List<Map<String, Object>> syncResults = new ArrayList<>();
        
        for (OfflineSyncRequest.OfflineAttendanceRecord record : request.records()) {
            try {
                Map<String, Object> result = processOfflineRecord(record, ipAddress);
                syncResults.add(result);
            } catch (Exception e) {
                syncResults.add(Map.of(
                        "sessionId", record.sessionId(),
                        "status", "FAILED",
                        "error", e.getMessage()
                ));
            }
        }

        return Map.of(
                "message", "Offline sync completed",
                "processed", syncResults.size(),
                "results", syncResults
        );
    }

    private Map<String, Object> processOfflineRecord(OfflineSyncRequest.OfflineAttendanceRecord record, String ipAddress) {
        // Check if attendance already exists
        UUID sessionId = UUID.fromString(record.sessionId());
        UUID studentId = UUID.fromString("student-id-from-auth"); // Get from auth context
        
        if (attendanceRecordRepository.existsBySession_IdAndStudent_Id(sessionId, studentId)) {
            return Map.of(
                    "sessionId", record.sessionId(),
                    "status", "DUPLICATE",
                    "message", "Attendance already recorded"
            );
        }

        // Check if there's already a synced record for this student and session
        List<OfflineAttendanceRecord> syncedRecords = offlineAttendanceRecordRepository
                .findByStudentIdAndSessionIdAndSyncStatus(studentId, sessionId, OfflineAttendanceRecord.SyncStatus.SYNCED);
        
        if (!syncedRecords.isEmpty()) {
            return Map.of(
                    "sessionId", record.sessionId(),
                    "status", "DUPLICATE",
                    "message", "Offline record already synced"
            );
        }

        // Check for pending or synced records using the new method
        List<OfflineAttendanceRecord> existingRecords = offlineAttendanceRecordRepository
                .findByStudentIdAndSessionIdAndSyncStatusIn(studentId, sessionId, 
                        List.of(OfflineAttendanceRecord.SyncStatus.PENDING, OfflineAttendanceRecord.SyncStatus.SYNCED));
        
        if (!existingRecords.isEmpty()) {
            return Map.of(
                    "sessionId", record.sessionId(),
                    "status", "DUPLICATE",
                    "message", "Record already exists"
            );
        }

        // Validate session exists and is active
        Optional<ClassroomSession> sessionOpt = classroomSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Map.of(
                    "sessionId", record.sessionId(),
                    "status", "FAILED",
                    "message", "Session not found"
            );
        }

        // Create attendance record
        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setSession(sessionOpt.get());
        // attendanceRecord.setStudent(student); // Set from auth context
        attendanceRecord.setStatus("PRESENT");
        attendanceRecord.setIpAddress(ipAddress);
        attendanceRecord.setRecordedAt(record.clientTimestamp());
        attendanceRecord.setBiometricSignature(record.biometricSignature());
        attendanceRecord.setMocked(false);

        attendanceRecordRepository.save(attendanceRecord);

        return Map.of(
                "sessionId", record.sessionId(),
                "status", "SYNCED",
                "message", "Attendance synced successfully",
                "recordId", attendanceRecord.getId()
        );
    }

    public Map<String, Object> getDeviceStatus() {
        // Get current user's device status
        // For now, return mock status
        
        return Map.of(
                "deviceRegistered", true,
                "deviceBound", true,
                "biometricRegistered", true,
                "lastSync", Instant.now().toString()
        );
    }
}
