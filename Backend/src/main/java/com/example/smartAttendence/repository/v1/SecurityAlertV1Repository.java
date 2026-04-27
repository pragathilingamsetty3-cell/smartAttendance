package com.example.smartAttendence.repository.v1;

import com.example.smartAttendence.entity.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAlertV1Repository extends JpaRepository<SecurityAlert, UUID> {
    
    /**
     * Find unresolved alerts for dashboard monitoring
     */
    List<SecurityAlert> findByResolvedFalseOrderByCreatedAtDesc();
    
    /**
     * Find recent alerts for a specific student
     */
    List<SecurityAlert> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Count unresolved anomalies by type
     */
    @Query("SELECT a.alertType, COUNT(a) FROM SecurityAlert a WHERE a.resolved = false GROUP BY a.alertType")
    List<Object[]> countUnresolvedByAlertType();

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.resolved = false " +
           "AND a.createdAt >= :since " +
           "AND (:deptId IS NULL OR a.user.section.department.id = :deptId) " +
           "AND (:sectId IS NULL OR a.user.section.id = :sectId)")
    long countByResolvedFalseFiltered(@Param("deptId") UUID deptId, @Param("sectId") UUID sectId, @Param("since") java.time.LocalDateTime since);

    @Query("SELECT a FROM SecurityAlert a WHERE a.resolved = false " +
           "AND a.createdAt >= :since " +
           "AND (:deptId IS NULL OR a.user.section.department.id = :deptId) " +
           "AND (:sectId IS NULL OR a.user.section.id = :sectId) " +
           "ORDER BY a.createdAt DESC")
    List<SecurityAlert> findActiveAlertsFiltered(@Param("deptId") UUID deptId, @Param("sectId") UUID sectId, @Param("since") java.time.LocalDateTime since);

    long countByResolvedFalse();

    @Query(value = "SELECT COUNT(*) FROM security_alerts", nativeQuery = true)
    long countNative();

    @Query("SELECT AVG(a.confidence) FROM SecurityAlert a WHERE a.resolved = false")
    Double getAverageConfidence();
    
    List<SecurityAlert> findByUser_IdAndResolvedFalse(UUID userId);
}
