-- =====================================
-- Flyway Migration V13: Extreme Performance Indexes
-- =====================================
-- Purpose: Optimize common query patterns for AI Analytics and Dashboard
-- Author: Antigravity AI
-- Date: 2026-04-21

-- 1. Attendance Metrics Optimization (Speeds up Dashboard Trends & Velocity)
CREATE INDEX IF NOT EXISTS idx_perf_attendance_status_recorded ON attendance_records(status, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_perf_attendance_student_recorded ON attendance_records(student_id, recorded_at DESC);

-- 2. Timetable Lookup Optimization (Speeds up Timetable Architect Grid)
CREATE INDEX IF NOT EXISTS idx_perf_timetable_lookup ON timetables(section_id, day_of_week, start_time);
CREATE INDEX IF NOT EXISTS idx_perf_timetable_dates ON timetables(start_date, end_date);

-- 3. Live session Monitoring Optimization
CREATE INDEX IF NOT EXISTS idx_perf_sessions_active_times ON classroom_sessions(active, start_time, end_time);

-- 4. User/Section Counts
CREATE INDEX IF NOT EXISTS idx_perf_users_role_section ON users(role, section_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_perf_users_role_dept ON users(role, department_id) WHERE is_active = true;
