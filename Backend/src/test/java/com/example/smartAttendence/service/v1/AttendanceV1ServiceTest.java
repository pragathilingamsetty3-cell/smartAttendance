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
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.security.SecurityAuditLogger;
import com.example.smartAttendence.service.ai.AILearningOptimizer;
import com.example.smartAttendence.service.ai.AISpatialMonitoringEngine;
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
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.api.core.ApiFuture;
import java.util.Map;
import java.util.HashMap;

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
    private UserV1Repository userRepository;

    @Mock
    private ClassroomSessionV1Repository classroomSessionRepository;

    @Mock
    private AttendanceRecordV1Repository attendanceRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AILearningOptimizer aiLearningOptimizer;

    @Mock
    private SecurityAlertV1Repository securityAlertRepository;

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private AISpatialMonitoringEngine spatialEngine;

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference collectionReference;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private ApiFuture<DocumentSnapshot> apiFuture;

    @Mock
    private DocumentSnapshot documentSnapshot;

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
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> {
            AttendanceRecord rec = i.getArgument(0);
            rec.setId(UUID.randomUUID());
            return rec;
        });
    }

    // ========== HALL PASS TESTS ==========

    @Test
    @DisplayName("grantHallPass - Happy Path")
    void grantHallPass_HappyPath_ShouldCreateHallPassInFirestore() {
        // Arrange
        when(firestore.collection("hall_passes")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);

        // Act
        assertDoesNotThrow(() -> attendanceService.grantHallPass(testHallPassRequest));

        // Assert
        verify(firestore).collection("hall_passes");
        verify(documentReference).set(any(Map.class));
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
    @DisplayName("processEnhancedHeartbeat - Happy Path - Should process heartbeat inside geofence")
    void processEnhancedHeartbeat_HappyPath_InsideGeofence_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(apiFuture);
        when(apiFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.getData()).thenReturn(null); // No active hall pass

        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.of(testSession));

        // Act
        assertDoesNotThrow(() -> attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false));

        // Assert
        verify(classroomSessionRepository, times(2)).findById(testSessionId);
    }

    @Test
    @DisplayName("processEnhancedHeartbeat - Exception Path - Should throw exception when session not found")
    void processEnhancedHeartbeat_SessionNotFound_ShouldThrowException() {
        // Arrange
        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(apiFuture);
        try {
            when(apiFuture.get()).thenReturn(documentSnapshot);
            when(documentSnapshot.getData()).thenReturn(null);
        } catch (Exception ignored) {}

        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false)
        );
    }

    @Test
    @DisplayName("processEnhancedHeartbeat - Exception Path - Should handle session not found gracefully")
    void processEnhancedHeartbeat_SessionNotFound_ShouldThrow() {
        when(classroomSessionRepository.findById(testSessionId))
                .thenReturn(Optional.empty());
        
        assertThrows(java.util.NoSuchElementException.class, () -> 
            attendanceService.processEnhancedHeartbeat(testHeartbeatPing, false));
    }

    @Test
    @DisplayName("processEnhancedHeartbeat - Walk Out Detection - Should mark walk-out")
    void processEnhancedHeartbeat_OutsideGeofence_ShouldMarkWalkOut() throws Exception {
        // Arrange
        EnhancedHeartbeatPing outsidePing = new EnhancedHeartbeatPing(
                testStudentId, testSessionId,
                13.0, 78.0, 100, 0.1, 0.2, 0.3, true, Instant.now(),
                "device-fingerprint-123", "biometric-signature-123", 85, false, true, "MOVING", null, 30L, 
                null, 1L // 🔐 signature, 📈 sequence
        );

        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(apiFuture);
        when(apiFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.getData()).thenReturn(null);
        when(documentSnapshot.exists()).thenReturn(false);

        when(classroomSessionRepository.findById(testSessionId)).thenReturn(Optional.of(testSession));
        
        AttendanceRecord mockRecord = new AttendanceRecord();
        mockRecord.setStatus("PRESENT");
        when(attendanceRecordRepository.findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(any(), any()))
            .thenReturn(Optional.of(mockRecord));

        // Act
        System.out.println("--- CALL 1 ---");
        attendanceService.processEnhancedHeartbeat(outsidePing, false);
        System.out.println("--- CALL 2 ---");
        attendanceService.processEnhancedHeartbeat(outsidePing, false);
        System.out.println("--- CALL 3 ---");
        attendanceService.processEnhancedHeartbeat(outsidePing, false);

        // Assert
        assertEquals("WALK_OUT", mockRecord.getStatus());
    }

    // ========== ENHANCED HEARTBEAT TESTS ==========

    /*
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
    */

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
                30L,        // nextHeartbeatInterval
                null,       // requestSignature
                1L          // sequenceId
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
