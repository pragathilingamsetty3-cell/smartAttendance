package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.RoomChangeTransition;
import com.example.smartAttendence.domain.ClassroomSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomChangeTransitionRepository extends JpaRepository<RoomChangeTransition, UUID> {
    
    List<RoomChangeTransition> findBySessionIdOrderByTransitionStartTimeDesc(UUID sessionId);
    
    List<RoomChangeTransition> findByChangedByIdOrderByTransitionStartTimeDesc(UUID changedById);
    
    @Query("SELECT rct FROM RoomChangeTransition rct WHERE rct.session.id = :sessionId " +
           "AND rct.active = true " +
           "AND rct.transitionStartTime <= :currentTime " +
           "AND rct.transitionEndTime >= :currentTime")
    List<RoomChangeTransition> findActiveTransitionsForSession(
        @Param("sessionId") UUID sessionId,
        @Param("currentTime") Instant currentTime
    );
    
    @Query("SELECT rct FROM RoomChangeTransition rct WHERE rct.active = true " +
           "AND rct.transitionStartTime <= :currentTime " +
           "AND rct.transitionEndTime >= :currentTime")
    List<RoomChangeTransition> findAllActiveTransitions(@Param("currentTime") Instant currentTime);
    
    @Query("SELECT rct FROM RoomChangeTransition rct WHERE rct.roomChangeType = :changeType " +
           "AND rct.transitionStartTime >= :since " +
           "ORDER BY rct.transitionStartTime DESC")
    List<RoomChangeTransition> findByChangeTypeSince(
        @Param("changeType") RoomChangeTransition.RoomChangeType changeType,
        @Param("since") Instant since
    );
    
    @Query("SELECT COUNT(rct) FROM RoomChangeTransition rct WHERE rct.session.id = :sessionId " +
           "AND rct.transitionStartTime >= :since")
    Long countTransitionsSince(@Param("sessionId") UUID sessionId, @Param("since") Instant since);
    
    @Query("SELECT rct FROM RoomChangeTransition rct WHERE rct.active = true " +
           "AND rct.notificationsSent = false")
    List<RoomChangeTransition> findTransitionsWithoutNotifications();
}
