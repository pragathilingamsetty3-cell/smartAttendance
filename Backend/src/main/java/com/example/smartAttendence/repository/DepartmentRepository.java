package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, java.util.UUID> {
    
    Optional<Department> findByCode(String code);
    
    Optional<Department> findByName(String name);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
    
    List<Department> findByIsActive(Boolean isActive);
    
    List<Department> findByIsActiveTrue();
    
    // Delete method for test data cleanup
    @Modifying
    @Transactional
    @Query("DELETE FROM Department d WHERE d.name = :name")
    void deleteByName(@Param("name") String name);
}
