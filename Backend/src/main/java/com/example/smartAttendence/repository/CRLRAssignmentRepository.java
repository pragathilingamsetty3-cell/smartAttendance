package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.CRLRAssignment;
import com.example.smartAttendence.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CRLRAssignmentRepository extends JpaRepository<CRLRAssignment, UUID> {
    
    List<CRLRAssignment> findByUserIdAndActiveTrue(UUID userId);
    
    List<CRLRAssignment> findBySectionIdAndActiveTrue(UUID sectionId);
    
    List<CRLRAssignment> findBySectionIdAndRoleTypeAndActiveTrue(UUID sectionId, CRLRAssignment.RoleType roleType);
    
    @Query("SELECT cra FROM CRLRAssignment cra WHERE cra.user.id = :userId " +
           "AND cra.active = true " +
           "AND cra.section.id = :sectionId")
    CRLRAssignment findByUserAndSectionAndActive(UUID userId, UUID sectionId);
    
    @Query("SELECT cra FROM CRLRAssignment cra WHERE cra.user.id = :userId " +
           "AND cra.active = true " +
           "AND cra.roleType = :roleType")
    CRLRAssignment findByUserAndRoleTypeAndActive(UUID userId, CRLRAssignment.RoleType roleType);
    
    @Query("SELECT cra FROM CRLRAssignment cra WHERE cra.section.id = :sectionId " +
           "AND cra.active = true " +
           "ORDER BY cra.roleType, cra.user.name")
    List<CRLRAssignment> findActiveAssignmentsForSection(@Param("sectionId") UUID sectionId);
    
    boolean existsByUserIdAndSectionIdAndActiveTrue(UUID userId, UUID sectionId);
    
    boolean existsByUserIdAndRoleTypeAndActiveTrue(UUID userId, CRLRAssignment.RoleType roleType);
    
    boolean existsByUserIdAndSectionIdAndRoleTypeAndActiveTrue(UUID userId, UUID sectionId, CRLRAssignment.RoleType roleType);
    
    @Query("SELECT COUNT(cra) FROM CRLRAssignment cra WHERE cra.section.id = :sectionId " +
           "AND cra.active = true " +
           "AND cra.roleType = :roleType")
    Long countActiveAssignmentsForSectionAndRole(UUID sectionId, CRLRAssignment.RoleType roleType);
}
