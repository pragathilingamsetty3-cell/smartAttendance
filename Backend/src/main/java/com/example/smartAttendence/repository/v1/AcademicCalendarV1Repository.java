package com.example.smartAttendence.repository.v1;

import com.example.smartAttendence.entity.AcademicCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicCalendarV1Repository extends JpaRepository<AcademicCalendar, UUID> {

    /**
     * Find calendar entry by specific date
     */
    Optional<AcademicCalendar> findByDate(LocalDate date);

    /**
     * Find all entries within a date range
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date BETWEEN :startDate AND :endDate ORDER BY ac.date")
    List<AcademicCalendar> findByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all holidays in a date range
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date BETWEEN :startDate AND :endDate " +
           "AND ac.dayType = 'HOLIDAY' ORDER BY ac.date")
    List<AcademicCalendar> findHolidaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all exam days in a date range
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date BETWEEN :startDate AND :endDate " +
           "AND ac.dayType = 'EXAM_DAY' ORDER BY ac.date")
    List<AcademicCalendar> findExamDaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all special events in a date range
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date BETWEEN :startDate AND :endDate " +
           "AND ac.dayType = 'SPECIAL_EVENT' ORDER BY ac.date")
    List<AcademicCalendar> findSpecialEventsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Check if a specific date is a holiday
     */
    @Query("SELECT COUNT(ac) > 0 FROM AcademicCalendar ac WHERE ac.date = :date AND ac.dayType = 'HOLIDAY'")
    boolean isHoliday(@Param("date") LocalDate date);

    /**
     * Check if a specific date is an exam day
     */
    @Query("SELECT COUNT(ac) > 0 FROM AcademicCalendar ac WHERE ac.date = :date AND ac.dayType = 'EXAM_DAY'")
    boolean isExamDay(@Param("date") LocalDate date);

    /**
     * Check if a specific date is a half day
     */
    @Query("SELECT COUNT(ac) > 0 FROM AcademicCalendar ac WHERE ac.date = :date AND ac.dayType = 'HALF_DAY'")
    boolean isHalfDay(@Param("date") LocalDate date);

    /**
     * Find all entries that affect specific sections
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.affectsAllSections = false " +
           "AND ac.affectedSections LIKE %:sectionId%")
    List<AcademicCalendar> findByAffectedSections(@Param("sectionId") String sectionId);

    /**
     * Count holidays in a date range
     */
    @Query("SELECT COUNT(ac) FROM AcademicCalendar ac WHERE ac.date BETWEEN :startDate AND :endDate " +
           "AND ac.dayType = 'HOLIDAY'")
    long countHolidaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get next holiday after a specific date
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date > :date AND ac.dayType = 'HOLIDAY' " +
           "ORDER BY ac.date ASC")
    Optional<AcademicCalendar> findNextHolidayAfter(@Param("date") LocalDate date);

    /**
     * Get next exam day after a specific date
     */
    @Query("SELECT ac FROM AcademicCalendar ac WHERE ac.date > :date AND ac.dayType = 'EXAM_DAY' " +
           "ORDER BY ac.date ASC")
    Optional<AcademicCalendar> findNextExamDayAfter(@Param("date") LocalDate date);
}
