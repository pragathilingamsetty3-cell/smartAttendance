package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByName(String name);

    List<Room> findByBuilding(String building);

    List<Room> findByBuildingAndFloor(String building, Integer floor);

    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity ORDER BY r.capacity ASC")
    List<Room> findByMinimumCapacity(Integer minCapacity);

    boolean existsByName(String name);
}
