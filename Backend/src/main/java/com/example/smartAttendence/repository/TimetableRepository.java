package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, UUID> {

    List<Timetable> findByDayOfWeekAndStartTimeBetween(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);

    /**
     * Find timetable entries for a specific day and time range
     * Used by autonomous scheduler to find active sessions
     */
    @Query("SELECT t FROM Timetable t WHERE t.dayOfWeek = :dayOfWeek " +
           "AND t.startTime <= :endTime AND t.endTime >= :startTime")
    List<Timetable> findByDayOfWeekAndStartTimeBeforeAndEndTimeAfter(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Find timetable entries for exact start time
     * Used for reminder notifications
     */
    List<Timetable> findByDayOfWeekAndStartTime(DayOfWeek dayOfWeek, LocalTime startTime);

    List<Timetable> findByFacultyId(UUID facultyId);

    @Query("SELECT t FROM Timetable t WHERE t.sectionId = :sectionId OR t.section.id = :sectionId")
    List<Timetable> findBySectionId(@Param("sectionId") UUID sectionId);

    List<Timetable> findByRoomId(UUID roomId);
    long countByRoomId(UUID roomId);

    @Query("SELECT t FROM Timetable t WHERE t.faculty.id = :facultyId AND t.dayOfWeek = :dayOfWeek ORDER BY t.startTime")
    List<Timetable> findByFacultyAndDayOfWeek(@Param("facultyId") UUID facultyId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT t FROM Timetable t WHERE t.room.id = :roomId AND t.dayOfWeek = :dayOfWeek ORDER BY t.startTime")
    List<Timetable> findByRoomAndDayOfWeek(@Param("roomId") UUID roomId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT t FROM Timetable t WHERE (t.sectionId = :sectionId OR t.section.id = :sectionId) AND t.dayOfWeek = :dayOfWeek ORDER BY t.startTime")
    List<Timetable> findBySectionAndDayOfWeek(@Param("sectionId") UUID sectionId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Find timetable entries by faculty and day
     */
    List<Timetable> findByFacultyIdAndDayOfWeek(UUID facultyId, DayOfWeek dayOfWeek);

    /**
     * Find timetable entries by room and day
     */
    List<Timetable> findByRoomIdAndDayOfWeek(UUID roomId, DayOfWeek dayOfWeek);

    /**
     * Find timetable entries by section and day
     */
    List<Timetable> findBySectionIdAndDayOfWeek(UUID sectionId, DayOfWeek dayOfWeek);

    /**
     * Check if timetable exists for section and day
     */
    boolean existsBySectionIdAndDayOfWeek(UUID sectionId, DayOfWeek dayOfWeek);

    /**
     * Find exam day entries
     */
    List<Timetable> findByIsExamDayTrue();

    /**
     * Find exam day entries for specific date range
     */
    @Query("SELECT t FROM Timetable t WHERE t.isExamDay = true " +
           "AND t.dayOfWeek = :dayOfWeek")
    List<Timetable> findExamDaysByDayOfWeek(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Count timetable entries for a faculty
     */
    long countByFacultyId(UUID facultyId);

    /**
     * Check if room is already booked for a time slot
     */
    @Query("SELECT COUNT(t) > 0 FROM Timetable t WHERE t.room.id = :roomId " +
           "AND t.dayOfWeek = :dayOfWeek " +
           "AND ((t.startTime < :endTime AND t.endTime > :startTime))")
    boolean isRoomBooked(
            @Param("roomId") UUID roomId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Check if faculty is already booked for a time slot
     */
    @Query("SELECT COUNT(t) > 0 FROM Timetable t WHERE t.faculty.id = :facultyId " +
           "AND t.dayOfWeek = :dayOfWeek " +
           "AND ((t.startTime < :endTime AND t.endTime > :startTime))")
    boolean isFacultyBooked(
            @Param("facultyId") UUID facultyId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    Optional<Timetable> findByFacultyIdAndRoomIdAndDayOfWeekAndStartTime(UUID facultyId, UUID roomId, DayOfWeek dayOfWeek, LocalTime startTime);
    
    // New methods for room change functionality
    @Query("SELECT t FROM Timetable t WHERE t.section.id = :sectionId " +
           "AND t.dayOfWeek = :dayOfWeek " +
           "AND t.startTime <= :currentTime " +
           "AND t.endTime >= :currentTime")
    List<Timetable> findCurrentSessionForSection(
        @Param("sectionId") UUID sectionId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("currentTime") LocalTime currentTime
    );
    
    @Query("SELECT t FROM Timetable t WHERE t.section.id = :sectionId " +
           "AND t.dayOfWeek = :dayOfWeek " +
           "AND t.startTime = :startTime")
    List<Timetable> findSessionForSectionAtTime(
        @Param("sectionId") UUID sectionId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startTime") LocalTime startTime
    );
    
    @Query("SELECT t FROM Timetable t WHERE t.faculty.id = :facultyId " +
           "AND t.dayOfWeek = :dayOfWeek " +
           "AND t.startTime <= :currentTime " +
           "AND t.endTime >= :currentTime")
    List<Timetable> findCurrentSessionForFaculty(
        @Param("facultyId") UUID facultyId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("currentTime") LocalTime currentTime
    );

    long countByDayOfWeek(DayOfWeek dayOfWeek);

    long countByDayOfWeekAndSection_Department_Id(DayOfWeek dayOfWeek, UUID departmentId);

    /**
     * Delete all timetable entries where the end date is before the given date
     */
    int deleteByEndDateBefore(java.time.LocalDate date);
}
