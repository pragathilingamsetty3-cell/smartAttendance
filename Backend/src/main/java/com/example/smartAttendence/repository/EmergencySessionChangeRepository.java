package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.EmergencySessionChange;
import com.example.smartAttendence.domain.ClassroomSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencySessionChangeRepository extends JpaRepository<EmergencySessionChange, UUID> {
    
    List<EmergencySessionChange> findBySessionIdOrderByChangeTimestampDesc(UUID sessionId);
    
    List<EmergencySessionChange> findByChangedByIdOrderByChangeTimestampDesc(UUID changedById);
    
    @Query("SELECT esc FROM EmergencySessionChange esc WHERE esc.session.id = :sessionId " +
           "AND esc.effectiveTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY esc.changeTimestamp DESC")
    List<EmergencySessionChange> findChangesInTimeRange(
        @Param("sessionId") UUID sessionId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
    
    @Query("SELECT esc FROM EmergencySessionChange esc WHERE esc.changeType = :changeType " +
           "AND esc.effectiveTimestamp >= :since " +
           "ORDER BY esc.changeTimestamp DESC")
    List<EmergencySessionChange> findByChangeTypeSince(
        @Param("changeType") EmergencySessionChange.EmergencyChangeType changeType,
        @Param("since") Instant since
    );
    
    @Query("SELECT COUNT(esc) FROM EmergencySessionChange esc WHERE esc.session.id = :sessionId " +
           "AND esc.changeTimestamp >= :since")
    Long countChangesSince(@Param("sessionId") UUID sessionId, @Param("since") Instant since);
    
    @Query("SELECT esc FROM EmergencySessionChange esc WHERE esc.emergencyOverride = true " +
           "AND esc.effectiveTimestamp >= :since " +
           "ORDER BY esc.changeTimestamp DESC")
    List<EmergencySessionChange> findEmergencyOverridesSince(@Param("since") Instant since);
}
