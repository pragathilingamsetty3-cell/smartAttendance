package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.entity.SensorReading;
import com.example.smartAttendence.service.SensorFusionService;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import com.example.smartAttendence.service.v1.AttendanceV1Service;
import com.example.smartAttendence.util.SecurityUtils;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.SecurityAlertV1Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AttendanceV1Controller Pure Unit Tests")
class AttendanceV1ControllerTest {

    private MockMvc mockMvc;
    private AttendanceV1Controller controller;
    private AttendanceV1Service attendanceService;
    private SensorFusionService sensorFusionService;
    private AILearningOptimizer aiLearningOptimizer;
    private SecurityUtils securityUtils;
    private StringRedisTemplate redisTemplate;
    private UserV1Repository userRepository;
    private SecurityAlertV1Repository securityAlertRepository;
    private ObjectMapper objectMapper;

    private UUID testStudentId;
    private UUID testSessionId;
    private EnhancedHeartbeatPing testHeartbeatPing;
    private HallPassRequestDTO testHallPassRequest;

    @BeforeEach
    void setUp() {
        // Create mocks
        attendanceService = mock(AttendanceV1Service.class);
        sensorFusionService = mock(SensorFusionService.class);
        aiLearningOptimizer = mock(AILearningOptimizer.class);
        securityUtils = mock(SecurityUtils.class);
        redisTemplate = mock(StringRedisTemplate.class);
        userRepository = mock(UserV1Repository.class);
        securityAlertRepository = mock(SecurityAlertV1Repository.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create controller with mocked dependencies
        controller = new AttendanceV1Controller(
                attendanceService, sensorFusionService, aiLearningOptimizer, securityUtils, redisTemplate, userRepository, securityAlertRepository);

        // Setup MockMvc with standalone controller (no Spring context)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Create test data
        testStudentId = UUID.randomUUID();
        testSessionId = UUID.randomUUID();
        testHeartbeatPing = createTestHeartbeatPing();
        testHallPassRequest = createTestHallPassRequest();
    }

    // ========== HEARTBEAT ENDPOINT TESTS ==========

    @Test
    @DisplayName("POST /api/v1/attendance/heartbeat - Happy Path")
    void heartbeat_Success() throws Exception {
        // Arrange
        doNothing().when(attendanceService).processHeartbeat(any(EnhancedHeartbeatPing.class), eq(false));

        // Act & Assert
        mockMvc.perform(post("/api/v1/attendance/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testHeartbeatPing)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Heartbeat recorded successfully"));

        verify(attendanceService).processHeartbeat(eq(testHeartbeatPing), eq(false));
    }

    // ========== HALL PASS ENDPOINT TESTS ==========

    @Test
    @DisplayName("POST /api/v1/attendance/hall-pass - Happy Path")
    void grantHallPass_Success() throws Exception {
        // Arrange
        doNothing().when(attendanceService).grantHallPass(any(HallPassRequestDTO.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/attendance/hall-pass")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testHallPassRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hall pass granted for 15 minutes."));

        verify(attendanceService).grantHallPass(eq(testHallPassRequest));
    }

    // ========== ENHANCED HEARTBEAT ENDPOINT TESTS ==========

    @Test
    @DisplayName("POST /api/v1/attendance/heartbeat-enhanced - Happy Path")
    void enhancedHeartbeat_Success() throws Exception {
        // Arrange
        doNothing().when(sensorFusionService).processEnhancedHeartbeat(any(EnhancedHeartbeatPing.class));
        when(sensorFusionService.getRecentReadings(any(UUID.class), any(UUID.class), eq(10)))
            .thenReturn(List.of());
        when(sensorFusionService.detectSpoofing(any(List.class))).thenReturn(false);
        
        when(sensorFusionService.determineOptimalGPSMode(any(EnhancedHeartbeatPing.class)))
            .thenReturn(new com.example.smartAttendence.service.SensorFusionService.GPSModeResult(
                com.example.smartAttendence.service.SensorFusionService.GPSMode.BALANCED, 
                "Stationary device", 20.0, 30000L, "STATIONARY"));
        when(sensorFusionService.needsHighAccuracyGPS(any(EnhancedHeartbeatPing.class), any(List.class)))
            .thenReturn(false);
        
        when(aiLearningOptimizer.optimizeForStudent(any(EnhancedHeartbeatPing.class)))
            .thenReturn(new com.example.smartAttendence.service.ai.AILearningOptimizer.AIOptimizationResult(
                60L, "BALANCED", 0.85, "Medium confidence based on historical data", 100, 0.85));

        when(attendanceService.calculateOptimalInterval(any(EnhancedHeartbeatPing.class))).thenReturn(60L);

        doNothing().when(attendanceService).processEnhancedHeartbeat(any(EnhancedHeartbeatPing.class), eq(false));

        // Act & Assert
        mockMvc.perform(post("/api/v1/attendance/heartbeat-enhanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testHeartbeatPing)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Enhanced heartbeat processed successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.sensorDataProcessed").value(true))
                .andExpect(jsonPath("$.batteryOptimization.currentBatteryLevel").value(85))
                .andExpect(jsonPath("$.batteryOptimization.deviceState").value("STATIONARY"))
                .andExpect(jsonPath("$.batteryOptimization.recommendedInterval").value(60))
                .andExpect(jsonPath("$.batteryOptimization.batteryMode").value("PERFORMANCE_MODE"))
                .andExpect(jsonPath("$.gpsOptimization.gpsMode").value("BALANCED"))
                .andExpect(jsonPath("$.gpsOptimization.accuracyMeters").value(20.0))
                .andExpect(jsonPath("$.gpsOptimization.updateIntervalMs").value(30000))
                .andExpect(jsonPath("$.gpsOptimization.needsHighAccuracy").value(false))
                .andExpect(jsonPath("$.aiLearning.optimalHeartbeatInterval").value(60))
                .andExpect(jsonPath("$.aiLearning.recommendedGPSMode").value("BALANCED"))
                .andExpect(jsonPath("$.aiLearning.confidence").value(0.85))
                .andExpect(jsonPath("$.aiLearning.learningStatus").value("HIGH_CONFIDENCE"));

        verify(sensorFusionService).processEnhancedHeartbeat(eq(testHeartbeatPing));
        verify(sensorFusionService).getRecentReadings(eq(testStudentId), eq(testSessionId), eq(10));
        verify(sensorFusionService).detectSpoofing(any(List.class));
        verify(aiLearningOptimizer).optimizeForStudent(eq(testHeartbeatPing));
        verify(attendanceService).processEnhancedHeartbeat(eq(testHeartbeatPing), eq(false));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/heartbeat-enhanced - Spoofing Detected")
    void enhancedHeartbeat_SpoofingDetected_Returns403() throws Exception {
        // Arrange
        doNothing().when(sensorFusionService).processEnhancedHeartbeat(any(EnhancedHeartbeatPing.class));
        when(sensorFusionService.getRecentReadings(any(UUID.class), any(UUID.class), eq(10)))
            .thenReturn(List.of());
        when(sensorFusionService.detectSpoofing(any(List.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/v1/attendance/heartbeat-enhanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testHeartbeatPing)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Spoofing detected"))
                .andExpect(jsonPath("$.reason").value("Unusual sensor patterns detected (Security Violation)"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(sensorFusionService).detectSpoofing(any(List.class));
        verify(attendanceService, never()).processEnhancedHeartbeat(any(EnhancedHeartbeatPing.class), anyBoolean());
    }

    // ========== SENSOR STATUS ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/attendance/sensor-status/{sessionId}/{studentId} - Happy Path")
    void getSensorStatus_Success() throws Exception {
        // Arrange
        SensorReading mockReading = createMockSensorReading();
        when(sensorFusionService.getRecentReadings(eq(testStudentId), eq(testSessionId), eq(10)))
            .thenReturn(List.of(mockReading));
        when(sensorFusionService.calculateMotionState(any(EnhancedHeartbeatPing.class)))
            .thenReturn("STATIONARY");

        // Act & Assert
        mockMvc.perform(get("/api/v1/attendance/sensor-status/{sessionId}/{studentId}", 
                testSessionId.toString(), testStudentId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value(testSessionId.toString()))
                .andExpect(jsonPath("$.studentId").value(testStudentId.toString()))
                .andExpect(jsonPath("$.recentReadingsCount").value(1))
                .andExpect(jsonPath("$.lastReading").exists())
                .andExpect(jsonPath("$.motionAnalysis").value("STATIONARY"));

        verify(sensorFusionService).getRecentReadings(eq(testStudentId), eq(testSessionId), eq(10));
        verify(sensorFusionService).calculateMotionState(any(EnhancedHeartbeatPing.class));
    }

    // ========== PRIVATE HELPER METHODS ==========

    private EnhancedHeartbeatPing createTestHeartbeatPing() {
        return new EnhancedHeartbeatPing(
                testStudentId, testSessionId,
                12.9716, 77.5946,  // latitude, longitude
                100,               // stepCount
                0.1, 0.2, 0.3,    // accelerationX, Y, Z
                false,             // isDeviceMoving
                Instant.now(),      // timestamp
                "device-fingerprint-123",
                null,              // biometricSignature
                85,                // batteryLevel
                false,             // isCharging
                true,              // isScreenOn
                "STATIONARY",      // deviceState
                null,              // gpsAccuracy
                30L                // nextHeartbeatInterval
        );
    }

    private HallPassRequestDTO createTestHallPassRequest() {
        return new HallPassRequestDTO(
                testStudentId, testSessionId,
                "Medical emergency", 15, "Student needs to visit medical room"
        );
    }

    private SensorReading createMockSensorReading() {
        SensorReading reading = new SensorReading();
        reading.setId(UUID.randomUUID());
        reading.setStudentId(testStudentId);
        reading.setSessionId(testSessionId);
        reading.setLatitude(12.9716);
        reading.setLongitude(77.5946);
        reading.setStepCount(100);
        reading.setAccelerationX(0.1);
        reading.setAccelerationY(0.2);
        reading.setAccelerationZ(0.3);
        reading.setIsDeviceMoving(false);
        reading.setReadingTimestamp(Instant.now());
        reading.setDeviceFingerprint("device-fingerprint-123");
        return reading;
    }
}
