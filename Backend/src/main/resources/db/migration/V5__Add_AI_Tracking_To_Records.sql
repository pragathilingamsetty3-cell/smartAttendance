-- =====================================
-- Flyway Migration V5: Add AI Tracking to Records
-- =====================================
-- Purpose: Support real-time AI analytics by tracking decisions and confidence
-- Author: Antigravity AI
-- Date: 2026-04-04

-- Add tracking columns to attendance_records
ALTER TABLE attendance_records ADD COLUMN is_ai_decision BOOLEAN DEFAULT false;
ALTER TABLE attendance_records ADD COLUMN confidence DOUBLE PRECISION;

-- Add confidence column to security_alerts
ALTER TABLE security_alerts ADD COLUMN confidence DOUBLE PRECISION;
