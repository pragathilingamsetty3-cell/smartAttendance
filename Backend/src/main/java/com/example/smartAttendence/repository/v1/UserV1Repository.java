package com.example.smartAttendence.repository.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserV1Repository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailIgnoreCase(String email);
    
    Optional<User> findByRegistrationNumber(String registrationNumber);
    
    boolean existsByEmail(String email);
    
    boolean existsByRegistrationNumber(String registrationNumber);
    
    long countByRole(Role role);
    
    @Query("SELECT COUNT(u) FROM V1User u WHERE u.department IN :departments")
    long countByDepartments(@Param("departments") List<String> departments);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.department IN :departments AND u.role IN :roles")
    long countByDepartmentsAndRoles(@Param("departments") List<String> departments, @Param("roles") List<Role> roles);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.role IN :roles")
    long countByRole(@Param("roles") List<Role> roles);

    // AI and Autonomous System Support
    List<User> findBySectionAndRole(com.example.smartAttendence.entity.Section section, Role role);

    @Query("SELECT u FROM V1User u WHERE u.sectionId = :sectionId AND u.role = :role AND u.status = 'ACTIVE'")
    List<User> findBySectionIdAndRole(@Param("sectionId") UUID sectionId, @Param("role") Role role);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.sectionId = :sectionId AND u.role = :role AND u.status = 'ACTIVE'")
    long countBySectionIdAndRole(@Param("sectionId") UUID sectionId, @Param("role") Role role);

    // Bulk promotion update method
    @Modifying
    @Transactional
    @Query("UPDATE V1User u SET u.sectionId = :sectionId WHERE u.id IN :studentIds")
    int bulkPromoteStudents(@Param("studentIds") List<UUID> studentIds, 
                           @Param("sectionId") UUID sectionId);

    // Faculty filtering for timetabling
    @Query("SELECT u FROM V1User u WHERE u.role IN ('FACULTY', 'HOD', 'DEAN') AND u.status = 'ACTIVE'")
    List<User> findActiveFaculty();

    // Department and section filtering methods
    
    // Migration support methods
    @Query("SELECT u FROM V1User u WHERE u.totalAcademicYears IS NOT NULL OR u.semester IS NOT NULL")
    List<User> findUsersWithAcademicData();
    @Query("SELECT COUNT(u) FROM V1User u WHERE u.section.department.id = :deptId AND u.role = :role AND u.status = 'ACTIVE'")
    long countBySection_Department_IdAndRole(@Param("deptId") UUID deptId, @Param("role") Role role);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.department IN :departments AND u.role IN :roles AND u.status = :status")
    long countByDepartmentsRoleAndStatus(@Param("departments") List<String> departments, @Param("roles") List<Role> roles, @Param("status") com.example.smartAttendence.domain.UserStatus status);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.sectionId IN :sectionIds AND u.role IN :roles AND u.status = :status")
    long countBySectionIdInRoleAndStatus(@Param("sectionIds") List<UUID> sectionIds, @Param("roles") List<Role> roles, @Param("status") com.example.smartAttendence.domain.UserStatus status);

    @Query("SELECT u FROM V1User u WHERE u.sectionId IN :sectionIds AND u.role = 'STUDENT' AND u.status = 'ACTIVE'")
    List<User> findStudentsBySections(@Param("sectionIds") List<UUID> sectionIds);

    @Query("SELECT COUNT(u) FROM V1User u WHERE u.sectionId = :sectionId AND u.role = :role AND u.status = :status")
    long countBySectionIdRoleAndStatus(@Param("sectionId") UUID sectionId, @Param("role") Role role, @Param("status") com.example.smartAttendence.domain.UserStatus status);

    @Query("SELECT u FROM V1User u WHERE u.department IN :departments AND u.role = 'FACULTY' AND u.status = 'ACTIVE'")
    List<User> findFacultyByDepartments(@Param("departments") List<String> departments);

    @Query("SELECT u FROM V1User u WHERE u.department IN :departments AND u.status = 'ACTIVE'")
    List<User> findUsersByDepartments(@Param("departments") List<String> departments);

    @Query("SELECT u FROM V1User u WHERE u.sectionId = :sectionId AND u.role = 'STUDENT' AND u.status = 'ACTIVE'")
    List<User> findStudentsBySectionId(@Param("sectionId") UUID sectionId);

    // Re-added for SharedUtilityService compatibility
    List<User> findByDepartment(String department);
    @Query("SELECT u FROM V1User u WHERE u.sectionId = :sectionId")
    List<User> findBySectionIdExplicit(@Param("sectionId") UUID sectionId);

    List<User> findBySectionId(UUID sectionId);
    @Query("SELECT COUNT(u) FROM V1User u WHERE u.sectionId IN :sectionIds")
    long countBySectionIdIn(@Param("sectionIds") List<UUID> sectionIds);

    @Query(value = "SELECT COUNT(*) FROM users u WHERE u.role IN :roles AND u.status = 'ACTIVE'", nativeQuery = true)
    long countByRoleInAndStatusNative(@Param("roles") List<String> roles);

    @Query(value = "SELECT COUNT(*) FROM users u WHERE u.role IN :roles", nativeQuery = true)
    long countByRoleInNative(@Param("roles") List<String> roles);

    long countByRoleInAndStatus(List<Role> roles, com.example.smartAttendence.domain.UserStatus status);
    long countByRoleIn(List<Role> roles);

    @Query("SELECT u.email FROM V1User u GROUP BY u.email HAVING COUNT(u.email) > 1")
    List<String> findDuplicateEmails();
    
    @Query("SELECT u FROM V1User u WHERE u.email = :email ORDER BY u.createdAt DESC")
    List<User> findByEmailOrderByCreatedAtDesc(@Param("email") String email);
    
    // Delete methods for test data cleanup
    @Modifying
    @Transactional
    @Query("DELETE FROM V1User u WHERE u.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Query("SELECT u.sectionId, COUNT(u) FROM V1User u WHERE u.role = :role AND u.status = 'ACTIVE' GROUP BY u.sectionId")
    List<Object[]> countActiveUsersPerSection(@Param("role") Role role);
}

