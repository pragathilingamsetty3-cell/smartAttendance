-- =====================================
-- Flyway Migration V10: Add Security Audit Fields to Attendance Records
-- =====================================
-- Purpose: Support detailed security logging for hardware and biometric verification
-- Author: Antigravity AI
-- Date: 2026-04-12

ALTER TABLE attendance_records 
ADD COLUMN device_signature VARCHAR(255),
ADD COLUMN battery_level INTEGER,
ADD COLUMN is_hardware_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN biometric_verified BOOLEAN DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN attendance_records.device_signature IS 'The hardware identifier of the device used for this specific check-in';
COMMENT ON COLUMN attendance_records.battery_level IS 'The phone battery percentage at the time of check-in (for AI optimization)';
COMMENT ON COLUMN attendance_records.is_hardware_verified IS 'True if the device ID matched the student''s registered hardware';
COMMENT ON COLUMN attendance_records.biometric_verified IS 'True if a cryptographic biometric handshake was performed';
