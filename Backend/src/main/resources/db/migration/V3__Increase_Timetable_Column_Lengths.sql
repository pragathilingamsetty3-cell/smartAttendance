-- =========================================================================
-- FLYWAY MIGRATION V3: Increase Timetable Column Lengths
-- =========================================================================
-- Purpose: Support longer Academic Year and Semester names (e.g. 'Spring 2026')
-- Impacted Tables: timetables, cr_lr_assignments
-- =========================================================================

ALTER TABLE timetables ALTER COLUMN academic_year TYPE VARCHAR(50);
ALTER TABLE timetables ALTER COLUMN semester TYPE VARCHAR(50);

ALTER TABLE cr_lr_assignments ALTER COLUMN academic_year TYPE VARCHAR(50);
ALTER TABLE cr_lr_assignments ALTER COLUMN semester TYPE VARCHAR(50);
