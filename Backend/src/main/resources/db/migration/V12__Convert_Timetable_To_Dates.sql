-- =====================================
-- Flyway Migration V12: Convert Timetable to Date-Based Timelines
-- =====================================
-- Purpose: Remove string-based academic year/semester and replace with start/end dates
-- Author: Antigravity AI
-- Date: 2026-04-21

-- Add new date columns
ALTER TABLE timetables ADD COLUMN start_date DATE;
ALTER TABLE timetables ADD COLUMN end_date DATE;

-- Migrate data (Optional: Try to infer dates from academic_year/semester strings if possible, 
-- but since they are free-text, it's safer to just set defaults or nulls)
UPDATE timetables SET start_date = CURRENT_DATE, end_date = (CURRENT_DATE + INTERVAL '180 days') WHERE start_date IS NULL;

-- Remove old columns
ALTER TABLE timetables DROP COLUMN academic_year;
ALTER TABLE timetables DROP COLUMN semester;

-- Update indexes
DROP INDEX IF EXISTS idx_timetables_academic_year_semester;
CREATE INDEX idx_timetables_validity_period ON timetables(start_date, end_date);
