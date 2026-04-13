-- =====================================
-- Flyway Migration V9: Add AI Feedback Correction Fields
-- =====================================
-- Purpose: Support manual corrections by faculty and AI learning feedback loops
-- Author: Antigravity AI
-- Date: 2026-04-09

-- Add correction tracking columns to attendance_records
ALTER TABLE attendance_records ADD COLUMN manually_corrected BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE attendance_records ADD COLUMN original_ai_status VARCHAR(255);
ALTER TABLE attendance_records ADD COLUMN corrector_id UUID;
