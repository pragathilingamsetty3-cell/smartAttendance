-- Add Holiday Support to Timetable and Sessions
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS is_holiday BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS is_holiday BOOLEAN NOT NULL DEFAULT false;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_timetable_holiday ON timetables(is_holiday);
CREATE INDEX IF NOT EXISTS idx_session_holiday ON classroom_sessions(is_holiday);
