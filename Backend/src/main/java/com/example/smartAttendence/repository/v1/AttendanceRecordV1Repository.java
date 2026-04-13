package com.example.smartAttendence.repository.v1;

import com.example.smartAttendence.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordV1Repository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findFirstByStudent_IdAndSession_IdOrderByRecordedAtDesc(
            UUID studentId,
            UUID sessionId
    );

    boolean existsBySession_IdAndStudent_Id(UUID sessionId, UUID studentId);

    List<AttendanceRecord> findBySessionIdOrderByRecordedAtAsc(UUID sessionId);
    List<AttendanceRecord> findBySessionIdAndStatus(UUID sessionId, String status);

    // AI and Autonomous System Support
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.session.id = :sessionId AND ar.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") UUID sessionId, @Param("status") String status);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.session.id = :sessionId ORDER BY ar.recordedAt DESC")
    List<AttendanceRecord> findBySessionIdOrderByRecordedAtDesc(@Param("sessionId") UUID sessionId);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.recordedAt >= :since")
    List<AttendanceRecord> findByStudentIdAndRecordedAtAfter(@Param("studentId") java.util.UUID studentId, @Param("since") java.time.Instant since);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.status = 'WALK_OUT' AND ar.recordedAt >= :since")
    long countWalkOutsByStudentSince(@Param("studentId") java.util.UUID studentId, @Param("since") java.time.Instant since);

    long countByStatus(String status);
    long countByRecordedAtAfter(Instant since);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.status = :status AND ar.student.department IN :departments")
    long countByStatusAndDepartments(@Param("status") String status, @Param("departments") List<String> departments);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE (ar.status = 'PRESENT' OR ar.status = 'LATE') AND ar.student.department IN :departments")
    long countVerifiedByDepartments(@Param("departments") List<String> departments);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.recordedAt > :since AND ar.student.department IN :departments")
    long countByRecordedAtAfterAndDepartments(@Param("since") Instant since, @Param("departments") List<String> departments);

    
    @Query("SELECT AVG(ar.confidence) FROM AttendanceRecord ar WHERE ar.aiDecision = true " +
           "AND ar.recordedAt >= :since " +
           "AND (:deptId IS NULL OR ar.student.section.department.id = :deptId) " +
           "AND (:sectId IS NULL OR ar.student.section.id = :sectId)")
    Double getAverageAiConfidenceFiltered(@Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.recordedAt > :since " +
           "AND (:deptId IS NULL OR ar.student.section.department.id = :deptId) " +
           "AND (:sectId IS NULL OR ar.student.section.id = :sectId)")
    long countActiveFiltered(@Param("since") Instant since, @Param("deptId") UUID deptId, @Param("sectId") UUID sectId);

    @Query("SELECT COUNT(u) FROM V1User u " +
           "WHERE u.role = com.example.smartAttendence.enums.Role.STUDENT " +
           "AND (:deptId IS NULL OR (u.section IS NOT NULL AND u.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (u.section IS NOT NULL AND u.section.id = :sectId)) " +
           "AND EXISTS (SELECT 1 FROM AttendanceRecord ar WHERE ar.student.id = u.id " +
           "AND ar.status = :status AND ar.recordedAt >= :since)")
    long countDistinctStudentByAiStatusFiltered(@Param("status") String status, @Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(u) FROM V1User u " +
           "WHERE u.role = com.example.smartAttendence.enums.Role.STUDENT " +
           "AND (:deptId IS NULL OR (u.section IS NOT NULL AND u.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (u.section IS NOT NULL AND u.section.id = :sectId)) " +
           "AND EXISTS (SELECT 1 FROM AttendanceRecord ar WHERE ar.student.id = u.id " +
           "AND ar.status IN :statuses AND ar.recordedAt >= :since)")
    long countDistinctStudentByAiStatusInFiltered(@Param("statuses") List<String> statuses, @Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(u) FROM V1User u " +
           "WHERE u.role = com.example.smartAttendence.enums.Role.STUDENT " +
           "AND (:deptId IS NULL OR (u.section IS NOT NULL AND u.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (u.section IS NOT NULL AND u.section.id = :sectId)) " +
           "AND EXISTS (SELECT 1 FROM AttendanceRecord ar " +
           "  WHERE ar.student.id = u.id " +
           "  AND ar.recordedAt >= :since " +
           "  AND ar.status IN :statuses " +
           "  AND ar.recordedAt = (SELECT MAX(ar2.recordedAt) " +
           "                       FROM AttendanceRecord ar2 " +
           "                       WHERE ar2.student.id = u.id AND ar2.recordedAt >= :since))")
    long countByLatestStatusIn(@Param("statuses") List<String> statuses, @Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(u) FROM V1User u " +
           "WHERE u.role = com.example.smartAttendence.enums.Role.STUDENT " +
           "AND (:deptId IS NULL OR (u.section IS NOT NULL AND u.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (u.section IS NOT NULL AND u.section.id = :sectId)) " +
           "AND EXISTS (SELECT 1 FROM AttendanceRecord ar WHERE ar.student.id = u.id " +
           "AND ar.aiDecision = true AND ar.recordedAt >= :since)")
    long countDistinctStudentByAiDecisionTrueFiltered(@Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar " +
           "WHERE ar.aiDecision = true " +
           "AND ar.recordedAt >= :since " +
           "AND (:deptId IS NULL OR (ar.student.section IS NOT NULL AND ar.student.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (ar.student.section IS NOT NULL AND ar.student.section.id = :sectId))")
    long countByAiDecisionTrueFiltered(@Param("since") Instant since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM SecurityAlert a " +
           "WHERE a.resolved = false " +
           "AND a.createdAt >= :since " +
           "AND (a.alertType LIKE '%SPOOF%' OR a.alertType LIKE '%IDENTITY%' OR a.alertType LIKE '%SECURITY%') " +
           "AND (:deptId IS NULL OR (a.user.section IS NOT NULL AND a.user.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (a.user.section IS NOT NULL AND a.user.section.id = :sectId))")
    long countDistinctStudentWithSecurityAlertsFiltered(@Param("since") LocalDateTime since, @Param("deptId") java.util.UUID deptId, @Param("sectId") java.util.UUID sectId);
    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.student s " +
           "JOIN FETCH s.section sect " +
           "JOIN FETCH sect.department d " +
           "WHERE ar.session.active = true " +
           "AND ar.recordedAt = (SELECT MAX(ar2.recordedAt) FROM AttendanceRecord ar2 WHERE ar2.student.id = s.id AND ar2.session.id = ar.session.id) " +
           "AND (:deptId IS NULL OR d.id = :deptId) " +
           "AND (:sectId IS NULL OR sect.id = :sectId) " +
           "ORDER BY ar.recordedAt DESC")
    List<AttendanceRecord> findActiveSpatialRecords(@Param("deptId") UUID deptId, @Param("sectId") UUID sectId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.recordedAt >= :start AND ar.recordedAt < :end " +
           "AND (:deptId IS NULL OR (ar.student.section IS NOT NULL AND ar.student.section.department.id = :deptId)) " +
           "AND (:sectId IS NULL OR (ar.student.section IS NOT NULL AND ar.student.section.id = :sectId))")
    long countBetweenTimesFiltered(@Param("start") Instant start, @Param("end") Instant end, @Param("deptId") UUID deptId, @Param("sectId") UUID sectId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM AttendanceRecord ar WHERE ar.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}

