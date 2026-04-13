package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    /**
     * Find the most recent sensor reading for a student in a session
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.studentId = :studentId AND sr.sessionId = :sessionId " +
           "ORDER BY sr.readingTimestamp DESC")
    Optional<SensorReading> findFirstByStudentIdAndSessionIdOrderByReadingTimestampDesc(
            @Param("studentId") UUID studentId,
            @Param("sessionId") UUID sessionId
    );

    /**
     * Find recent sensor readings for a student in a session after a specific timestamp
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.studentId = :studentId AND sr.sessionId = :sessionId " +
           "AND sr.readingTimestamp > :cutoffTime ORDER BY sr.readingTimestamp DESC")
    List<SensorReading> findByStudentIdAndSessionIdAndReadingTimestampAfterOrderByReadingTimestampDesc(
            @Param("studentId") UUID studentId,
            @Param("sessionId") UUID sessionId,
            @Param("cutoffTime") Instant cutoffTime
    );

    /**
     * Find all sensor readings for a student in a session
     */
    List<SensorReading> findByStudentIdAndSessionIdOrderByReadingTimestampDesc(
            UUID studentId, 
            UUID sessionId
    );

    /**
     * Find sensor readings within a time range for a session
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.sessionId = :sessionId " +
           "AND sr.readingTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY sr.readingTimestamp ASC")
    List<SensorReading> findBySessionIdAndReadingTimestampBetweenOrderByReadingTimestampAsc(
            @Param("sessionId") UUID sessionId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Count sensor readings for a student in a session
     */
    long countByStudentIdAndSessionId(UUID studentId, UUID sessionId);

    /**
     * Find readings with high acceleration (possible movement)
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.sessionId = :sessionId " +
           "AND SQRT(sr.accelerationX * sr.accelerationX + sr.accelerationY * sr.accelerationY + sr.accelerationZ * sr.accelerationZ) > :threshold " +
           "ORDER BY sr.readingTimestamp DESC")
    List<SensorReading> findHighAccelerationReadings(
            @Param("sessionId") UUID sessionId,
            @Param("threshold") double threshold
    );

    /**
     * Find readings with step count increases
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.sessionId = :sessionId " +
           "AND sr.stepCount > 0 ORDER BY sr.readingTimestamp DESC")
    List<SensorReading> findReadingsWithStepCount(UUID sessionId);

    /**
     * Get average step count for a session
     */
    @Query("SELECT AVG(sr.stepCount) FROM SensorReading sr WHERE sr.sessionId = :sessionId")
    Double getAverageStepCountBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Find readings where device was moving
     */
    List<SensorReading> findBySessionIdAndIsDeviceMovingTrueOrderByReadingTimestampDesc(UUID sessionId);

    /**
     * Find readings where device was stationary
     */
    List<SensorReading> findBySessionIdAndIsDeviceMovingFalseOrderByReadingTimestampDesc(UUID sessionId);

    /**
     * Delete old sensor readings (cleanup)
     */
    @Query("DELETE FROM SensorReading sr WHERE sr.readingTimestamp < :cutoffTime")
    void deleteOldReadings(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Find readings by device fingerprint
     */
    List<SensorReading> findByDeviceFingerprintOrderByReadingTimestampDesc(String deviceFingerprint);
}
