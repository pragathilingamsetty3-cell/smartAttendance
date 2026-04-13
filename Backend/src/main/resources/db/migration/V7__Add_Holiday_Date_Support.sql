-- Add Holiday Date Support to Timetable
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS holiday_date DATE;

-- Drop old index and create optimized one
DROP INDEX IF EXISTS idx_timetable_holiday;
CREATE INDEX IF NOT EXISTS idx_timetable_holiday ON timetables(is_holiday, holiday_date);
