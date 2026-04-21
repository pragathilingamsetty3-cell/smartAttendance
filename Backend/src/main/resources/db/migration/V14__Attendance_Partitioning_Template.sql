-- ============================================================================
-- 📈 ULTRA-PERFORMANCE: ATTENDANCE RECORD PARTITIONING TEMPLATE
-- ============================================================================
-- PURPOSE: This script provides a roadmap for partitioning the attendance_records 
-- table. Partitioning is the strategy of splitting one massive table into 
-- multiple smaller tables (partitions) based on a key (like Academic Year).
-- 
-- WHEN TO USE: When attendance_records exceeds 1,000,000 rows.
-- IMPACT: Keeps query times near-instant by only searching relevant years.
-- ============================================================================

/*
-- STEP 1: CREATE THE MASTER PARTITIONED TABLE
CREATE TABLE attendance_records_partitioned (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    session_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id, recorded_at) -- Partition key must be part of PK
) PARTITION BY RANGE (recorded_at);

-- STEP 2: CREATE PARTITIONS FOR EACH ACADEMIC YEAR
CREATE TABLE attendance_records_2025 PARTITION OF attendance_records_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE attendance_records_2026 PARTITION OF attendance_records_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- STEP 3: MIGRATION DATA (DO THIS DURING MAINTENANCE)
-- INSERT INTO attendance_records_partitioned SELECT * FROM attendance_records;
-- ALTER TABLE attendance_records RENAME TO attendance_records_old;
-- ALTER TABLE attendance_records_partitioned RENAME TO attendance_records;

-- STEP 4: CREATE INDEXES ON EACH PARTITION
-- CREATE INDEX idx_attendance_records_2025_query ON attendance_records_2025 (student_id, recorded_at DESC);
*/

-- IMPORTANT: This script is a template. Do NOT uncomment it until you 
-- have backed up your database and have a scheduled maintenance window.
-- 🏎️ PRO-TIP: Partitioning essentially gives you $O(log N/P)$ instead of $O(log N)$.
