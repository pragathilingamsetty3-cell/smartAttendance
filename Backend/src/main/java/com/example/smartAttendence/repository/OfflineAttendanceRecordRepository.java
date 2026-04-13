package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.OfflineAttendanceRecord;
import com.example.smartAttendence.entity.OfflineAttendanceRecord.SyncStatus;
import com.example.smartAttendence.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfflineAttendanceRecordRepository extends JpaRepository<OfflineAttendanceRecord, UUID> {

    List<OfflineAttendanceRecord> findByStudentAndSyncStatus(User student, SyncStatus syncStatus);

    List<OfflineAttendanceRecord> findBySyncStatus(SyncStatus syncStatus);

    List<OfflineAttendanceRecord> findByDeviceFingerprintAndSyncStatus(String deviceFingerprint, SyncStatus syncStatus);

    List<OfflineAttendanceRecord> findByStudentIdAndSessionIdAndSyncStatus(UUID studentId, UUID sessionId, SyncStatus syncStatus);

    boolean existsByStudentAndSessionIdAndSyncStatus(User student, UUID sessionId, SyncStatus syncStatus);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OfflineAttendanceRecord o WHERE o.student.id = :studentId AND o.sessionId = :sessionId AND (o.syncStatus = com.example.smartAttendence.entity.OfflineAttendanceRecord$SyncStatus.PENDING OR o.syncStatus = com.example.smartAttendence.entity.OfflineAttendanceRecord$SyncStatus.SYNCED)")
    boolean hasPendingOrSyncedRecord(@Param("studentId") UUID studentId, @Param("sessionId") UUID sessionId);

    // Additional derived query methods for complex enum checks
    List<OfflineAttendanceRecord> findByStudentIdAndSessionIdAndSyncStatusIn(UUID studentId, UUID sessionId, List<SyncStatus> syncStatuses);

    void deleteByStudentAndSyncStatus(User student, SyncStatus syncStatus);
}
