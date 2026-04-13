-- Relax constraints for Timetable to support Holiday Sessions
ALTER TABLE timetables ALTER COLUMN room_id DROP NOT NULL;
ALTER TABLE timetables ALTER COLUMN faculty_id DROP NOT NULL;
