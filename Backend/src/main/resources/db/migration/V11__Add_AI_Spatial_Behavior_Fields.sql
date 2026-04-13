-- =====================================
-- Flyway Migration V11: Add AI Spatial Behavior & GPS Fields
-- =====================================
-- Purpose: Support high-resolution spatial behavior tracking and real-time GPS coordinates
-- Author: Antigravity AI
-- Date: 2026-04-12

ALTER TABLE attendance_records 
ADD COLUMN is_moving BOOLEAN DEFAULT FALSE,
ADD COLUMN acceleration_magnitude DOUBLE PRECISION DEFAULT 0.0,
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION,
ADD COLUMN altitude DOUBLE PRECISION,
ADD COLUMN gps_accuracy DOUBLE PRECISION;

-- Add comment for documentation
COMMENT ON COLUMN attendance_records.is_moving IS 'Real-time movement state from device sensors';
COMMENT ON COLUMN attendance_records.acceleration_magnitude IS 'Total force magnitude (G) detected during pulse';
COMMENT ON COLUMN attendance_records.latitude IS 'GPS Latitude at time of check-in';
COMMENT ON COLUMN attendance_records.longitude IS 'GPS Longitude at time of check-in';
COMMENT ON COLUMN attendance_records.gps_accuracy IS 'GPS precision in meters';
