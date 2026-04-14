package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.example.smartAttendence.dto.v1.HallPassRequestDTO;
import com.example.smartAttendence.event.WalkOutEvent;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.SecurityAlertV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.security.SecurityAuditLogger;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceV1Service Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceV1ServiceTest {

    @Mock
    private ClassroomSessionV1Repository classroomSessionRepository;

    @Mock
    private AttendanceRecordV1Repository attendanceRecordRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserV1Repository userRepository;

    @Mock
    private AILearningOptimizer aiLearningOptimizer;

    @Mock
    private SecurityAlertV1Repository securityAlertRepository;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AttendanceV1Service attendanceService;

    @Captor
    private ArgumentCaptor<WalkOutEvent> walkOutEventCaptor;

    @Captor
    private ArgumentCaptor<AttendanceRecord> attendanceRecordCaptor;

    private UUID testSessionId;
    private UUID testStudentId;
    private ClassroomSession testSession;
    private EnhancedHeartbeatPing testHeartbeatPing;
    private HallPassRequestDTO testHallPassRequest;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        testStudentId = UUID.randomUUID();
        testSession = createTestSession();
        testHeartbeatPing = createTestHeartbeatPing();
        testHallPassRequest = createTestHallPassRequest();

        // 🛡️ Setup default mock student for all lookups
        User testStudent = new User();
        testStudent.setId(testStudentId);
        testStudent.setEmail("test-student@university.edu");
        testStudent.setName("Test Student");
        testStudent.setDeviceId("device-fingerprint-123");
        testStudent.setBiometricSignature("biometric-signature-123");
        
        when(userRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(attendanceRecordRepository.existsBySession_IdAndStudent_Id(any(), any())).thenReturn(false);
    }

    // ========== HALL PASS TESTS ==========

    @Test
    @DisplayName("grantHallPass - Happy Path")
    void grantHallPass_HappyPath_ShouldCreateHallPassInRedis() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        assertDoesNotThrow(() -> attendanceService.grantHallPass(testHallPassRequest));

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        
        String expectedKey = "hallpass:" + testSessionId + ":" + testStudentId;
        assertEquals(expectedKey, keyCaptor.getValue());
        assertEquals("ACTIVE", valueCaptor.getValue());
        
        verify(redisTemplate).expire(eq(expectedKey), any());
    }

    @Test
    @DisplayName("grantHallPass - Exception Path - Should throw exception for null request")
    void grantHallPass_NullRequest_ShouldThrowException() {
        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> attendanceService.grantHallPass(null)
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("grantHallPass - Exception Path - Should throw exception for negative minutes")
    void grantHallPass_NegativeMinutes_ShouldThrowException() {
        // Arrange
        HallPassRequestDTO invalidRequest = new HallPassRequestDTO(
                testStudentId,
                testSessionId,
                "Test",
                -5,
                "Test notes"
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.grantHallPass(invalidRequest)
        );
        assertEquals("requestedMinutes must be positive", exception.getMessage());
    }

    // ========== HEARTBEAT TESTS ==========

    @Test
    @DisplayName("processHeartbeat - Happy Path - Should process heartbeat inside geofence")
    void processHeartbeat_HappyPath_InsideGeofence_ShouldProcessSuccessfully() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> attendanceService.processHeartbeat(testHeartbeatPing, false));

        // Assert
        verify(classroomSessionRepository).findById(testSessionId);
        verify(redisTemplate).hasKey("hallpass:" + testSessionId + ":" + testStudentId);
        verify(redisTemplate).delete(contains("drift:"));
    }

    @Test
    @DisplayName("processHeartbeat - Happy Path - Should not process when hall pass is active")
    void processHeartbeat_HappyPath_HallPassActive_ShouldNotProcessAttendance() {
        // Arrange
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> attendanceService.processHeartbeat(testHeartbeatPing, false));

        // Assert
        verify(classroomSessionRepository, never()).findById(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("processHeartbeat - Exception Path - Should throw exception when session not found")
    void processHeartbeat_SessionNotFound_ShouldThrowException() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.processHeartbeat(testHeartbeatPing, false)
        );
        assertEquals("Session not found: " + testSessionId, exception.getMessage());
    }

    @Test
    @DisplayName("processHeartbeat - Exception Path - Should throw exception when geofence not configured")
    void processHeartbeat_GeofenceNotConfigured_ShouldThrowException() {
        // Arrange
        testSession.setGeofencePolygon(null);
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> attendanceService.processHeartbeat(testHeartbeatPing, false)
        );
        assertEquals("Session geofence is not configured: " + testSessionId, exception.getMessage());
    }

    @Test
    @DisplayName("processHeartbeat - Walk Out Detection - Should mark walk-out after 3 drifts")
    void processHeartbeat_OutsideGeofence_ShouldMarkWalkOutAfterThreeDrifts() {
        // Arrange
        EnhancedHeartbeatPing outsidePing = new EnhancedHeartbeatPing(
                testStudentId,
                testSessionId,
                13.0,     // latitude (outside geofence)
                78.0,     // longitude (outside geofence)
                100,      // stepCount
                0.1,      // accelerationX
                0.2,      // accelerationY
                0.3,      // accelerationZ
                true,     // isDeviceMoving
                Instant.now(),
                "device-fingerprint-123",
                null,     // biometricSignature
                85,       // batteryLevel
                false,    // isCharging
                true,     // isScreenOn
                "MOVING",  // deviceState
                null,      // gpsAccuracy
                30L       // nextHeartbeatInterval
        );

        User mockStudent = new User();
        mockStudent.setId(testStudentId);
        
        AttendanceRecord mockRecord = new AttendanceRecord();
        mockRecord.setId(UUID.randomUUID());
        mockRecord.setStudent(mockStudent);
        mockRecord.setSession(testSession);
        mockRecord.setStatus("PRESENT");
        mockRecord.setRecordedAt(Instant.now());

        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(
                testStudentId, testSessionId)).thenReturn(Optional.of(mockRecord));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenReturn(mockRecord);

        // Act
        assertDoesNotThrow(() -> attendanceService.processHeartbeat(outsidePing, false));

        // Assert
        verify(attendanceRecordRepository).save(mockRecord);
        assertEquals("WALK_OUT", mockRecord.getStatus());
        verify(eventPublisher).publishEvent(walkOutEventCaptor.capture());
        WalkOutEvent publishedEvent = walkOutEventCaptor.getValue();
        assertEquals(testStudentId, publishedEvent.studentId());
        assertEquals(testSessionId, publishedEvent.sessionId());
    }

    // ========== ENHANCED HEARTBEAT TESTS ==========

    @Test
    @DisplayName("processEnhancedHeartbeat - Happy Path - Should process enhanced heartbeat")
    void processEnhancedHeartbeat_HappyPath_ShouldProcessSuccessfully() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.expire(anyString(), any())).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false));

        // Assert
        verify(classroomSessionRepository, times(2)).findById(testSessionId);
        verify(redisTemplate).opsForValue();
    }

    @Test
    @DisplayName("processEnhancedHeartbeat - Happy Path - Should not process when hall pass is active")
    void processEnhancedHeartbeat_HallPassActive_ShouldNotProcessAttendance() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("ACTIVE");
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));

        // Act
        assertDoesNotThrow(() -> attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false));

        // Assert - The service should check for hall pass and automatic break pass
        verify(redisTemplate.opsForValue()).get("hallpass:" + testSessionId + ":" + testStudentId);
    }

    @Test
    @DisplayName("processEnhancedHeartbeat - Exception Path - Should throw exception when session not found")
    void processEnhancedHeartbeat_SessionNotFound_ShouldThrowException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false)
        );
        assertEquals("Session not found: " + testSessionId, exception.getMessage());
    }

    // ========== LOCATION VERIFICATION TESTS ==========

    @Test
    @DisplayName("verifyLocationEnhanced - Happy Path - Should verify location successfully")
    void verifyLocationEnhanced_HappyPath_ShouldReturnTrue() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("device-fingerprint-123");
        
        // Create a spy to mock private verification methods
        AttendanceV1Service spyService = org.mockito.Mockito.spy(attendanceService);
        
        // Mock the private verification methods to return true
        org.mockito.Mockito.doReturn(true).when(spyService).verifyDeviceFingerprint(any(), anyString());
        org.mockito.Mockito.doReturn(true).when(spyService).verifyWiFiNetworks(any(), anyString(), anyString());
        org.mockito.Mockito.doReturn(true).when(spyService).verifyIPLocation(any(), anyString(), anyString());
        org.mockito.Mockito.doReturn(true).when(spyService).verifyBehavioralPattern(any(), any(), anyDouble(), anyDouble());
        org.mockito.Mockito.doReturn(true).when(spyService).verifyTimeBasedAccess(any(), any());

        // Act
        boolean result = spyService.verifyLocationEnhanced(
                testSessionId, testStudentId, 12.9721, 77.5951, 
                "device-fingerprint-123", "TechUniversity-WiFi", "192.168.1.100"
        );

        // Assert
        assertTrue(result);
        verify(classroomSessionRepository).findById(testSessionId);
    }

    @Test
    @DisplayName("verifyLocationEnhanced - Exception Path - Should return false when session not found")
    void verifyLocationEnhanced_SessionNotFound_ShouldReturnFalse() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = attendanceService.verifyLocationEnhanced(
                testSessionId, testStudentId, 12.9721, 77.5951, 
                "device-fingerprint-123", "TechUniversity-WiFi", "192.168.1.100"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("verifyLocationEnhanced - Exception Path - Should return false for location outside geofence")
    void verifyLocationEnhanced_OutsideGeofence_ShouldReturnFalse() {
        // Arrange
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));

        // Act - Location outside geofence
        boolean result = attendanceService.verifyLocationEnhanced(
                testSessionId, testStudentId, 13.0, 78.0, 
                "device-fingerprint-123", "TechUniversity-WiFi", "192.168.1.100"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("grantHallPass - Edge Case - Should handle zero minutes request")
    void grantHallPass_ZeroMinutes_ShouldThrowException() {
        // Arrange
        HallPassRequestDTO zeroMinutesRequest = new HallPassRequestDTO(
                testStudentId,
                testSessionId,
                "Test",
                0,
                "Test notes"
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.grantHallPass(zeroMinutesRequest)
        );
        assertEquals("requestedMinutes must be positive", exception.getMessage());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private ClassroomSession createTestSession() {
        ClassroomSession session = new ClassroomSession();
        session.setId(testSessionId);
        session.setGeofencePolygon(createTestGeofencePolygon());
        
        // Create a test room for the session
        com.example.smartAttendence.entity.Room room = new com.example.smartAttendence.entity.Room();
        room.setId(UUID.randomUUID());
        room.setName("Test Room");
        room.setCapacity(30);
        room.setBoundaryPolygon(createTestGeofencePolygon());
        session.setRoom(room);
        
        session.setStartTime(Instant.now().minusSeconds(3600)); // Started 1 hour ago
        session.setEndTime(Instant.now().plusSeconds(3600));   // Ends in 1 hour
        
        return session;
    }

    private Polygon createTestGeofencePolygon() {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        
        // Create a simple square polygon around Bangalore coordinates
        Coordinate[] coordinates = new Coordinate[] {
            new Coordinate(77.5946, 12.9716),  // Bottom-left
            new Coordinate(77.5946, 12.9726),  // Top-left
            new Coordinate(77.5956, 12.9726),  // Top-right
            new Coordinate(77.5956, 12.9716),  // Bottom-right
            new Coordinate(77.5946, 12.9716)   // Close polygon
        };
        
        return factory.createPolygon(coordinates);
    }

    private EnhancedHeartbeatPing createTestHeartbeatPing() {
        // Create a point inside the geofence
        Point insidePoint = createPointInsideGeofence();
        return new EnhancedHeartbeatPing(
                testStudentId, testSessionId,
                insidePoint.getY(), insidePoint.getX(),
                100,      // stepCount
                0.1,      // accelerationX
                0.2,      // accelerationY
                0.3,      // accelerationZ
                false,     // isDeviceMoving
                Instant.now(),
                "device-fingerprint-123",
                "biometric-signature-123", // biometricSignature
                85,        // batteryLevel
                false,      // isCharging
                true,       // isScreenOn
                "STATIONARY", // deviceState
                null,         // gpsAccuracy
                30L         // nextHeartbeatInterval
        );
    }

    private HallPassRequestDTO createTestHallPassRequest() {
        return new HallPassRequestDTO(
                testStudentId, testSessionId,
                "Medical emergency", 15, "Student needs to visit medical room"
        );
    }

    private Point createPointInsideGeofence() {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = factory.createPoint(new Coordinate(77.5951, 12.9721));
        point.setSRID(4326);
        return point;
    }
}
