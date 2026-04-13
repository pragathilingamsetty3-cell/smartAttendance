-- =====================================
-- Flyway Migration V4: Add Note to AttendanceRecords
-- =====================================
-- Purpose: Support AI-generated automated notes in attendance records
-- Author: Antigravity AI
-- Date: 2026-04-04

ALTER TABLE attendance_records ADD COLUMN note TEXT;
