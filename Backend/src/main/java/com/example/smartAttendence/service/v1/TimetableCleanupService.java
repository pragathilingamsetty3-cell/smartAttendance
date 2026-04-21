package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableCleanupService {

    private final TimetableRepository timetableRepository;

    /**
     * 🧹 DAILY TIMETABLE CLEANUP
     * Runs every day at midnight (00:00:00).
     * Deletes all timetable entries where the end_date is in the past.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTimetables() {
        LocalDate today = LocalDate.now();
        log.info("🧹 Starting daily timetable cleanup for expired entries (End Date before {})", today);
        
        try {
            int deletedCount = timetableRepository.deleteByEndDateBefore(today);
            if (deletedCount > 0) {
                log.info("✅ Successfully cleared {} expired timetable entries from the database.", deletedCount);
            } else {
                log.info("ℹ️ No expired timetable entries found to clean up.");
            }
        } catch (Exception e) {
            log.error("❌ Failed to perform timetable cleanup: {}", e.getMessage());
        }
    }
}
