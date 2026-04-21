-- =====================================
-- Flyway Migration V16: Add Sequence Tracking
-- =====================================
-- Purpose: Ensure data integrity and handle out-of-order heartbeat packets
-- Author: Antigravity
-- Date: 2026-04-21

ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS sequence_id BIGINT DEFAULT 0;

-- Index for sequence lookups per student per session
CREATE INDEX IF NOT EXISTS idx_attendance_records_student_session_seq ON attendance_records(student_id, session_id, sequence_id);
