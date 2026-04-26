package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.entity.Department;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    java.util.Optional<Section> findByName(String name);

    List<Section> findByProgramAndTotalAcademicYearsAndCurrentSemester(String program, String totalAcademicYears, Integer currentSemester);

    List<Section> findByTotalAcademicYearsAndCurrentSemester(String totalAcademicYears, Integer currentSemester);

    @Query("SELECT s FROM Section s WHERE s.isActive = true AND s.totalAcademicYears = :totalAcademicYears AND s.currentSemester = :currentSemester")
    List<Section> findActiveSectionsByAcademicYearAndSemester(@Param("totalAcademicYears") String totalAcademicYears, @Param("currentSemester") Integer currentSemester);

    boolean existsByNameAndTotalAcademicYearsAndCurrentSemester(String name, String totalAcademicYears, Integer currentSemester);

    // Department-related queries
    List<Section> findByDepartment(Department department);
    
    List<Section> findByDepartmentId(UUID departmentId);
    
    List<Section> findByDepartmentIdAndIsActiveTrue(UUID departmentId);
    
    @Query("SELECT s FROM Section s WHERE s.isActive = true AND s.department.id = :departmentId ORDER BY s.name ASC")
    List<Section> findActiveSectionsByDepartmentIdOrderByNameAsc(@Param("departmentId") UUID departmentId);

    // For cascading dropdown - sections by department (using program as department)
    @Query("SELECT s FROM Section s WHERE s.isActive = true AND s.program = :departmentName ORDER BY s.name ASC")
    List<Section> findByDepartmentNameOrderByNameAsc(@Param("departmentName") String departmentName);
}
