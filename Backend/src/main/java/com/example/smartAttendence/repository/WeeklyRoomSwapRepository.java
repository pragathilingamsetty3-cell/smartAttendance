package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.WeeklyRoomSwap;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WeeklyRoomSwapRepository extends JpaRepository<WeeklyRoomSwap, UUID> {
    
    List<WeeklyRoomSwap> findByApprovedByIsNullOrderBySwapDateAsc();
    
    List<WeeklyRoomSwap> findByOriginalRoomIdOrNewRoomId(UUID originalRoomId, UUID newRoomId);
    
    @Query("SELECT wrs FROM WeeklyRoomSwap wrs WHERE wrs.swapDate = :date")
    List<WeeklyRoomSwap> findBySwapDate(@Param("date") java.time.LocalDate date);
    
    @Query("SELECT wrs FROM WeeklyRoomSwap wrs WHERE wrs.swapDate >= :startDate AND wrs.swapDate <= :endDate " +
           "ORDER BY wrs.swapDate ASC")
    List<WeeklyRoomSwap> findBySwapDateBetween(
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate
    );
    
    List<WeeklyRoomSwap> findByApprovedBy(User approvedBy);
    
    boolean existsByOriginalRoomIdAndNewRoomIdAndSwapDate(UUID originalRoomId, UUID newRoomId, java.time.LocalDate swapDate);
}
