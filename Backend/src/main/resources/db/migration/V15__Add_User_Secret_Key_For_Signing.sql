-- =====================================
-- Flyway Migration V15: Add Secret Key For Heartbeat Signing
-- =====================================
-- Purpose: Support HMAC-SHA256 request signing for elite security
-- Author: Antigravity
-- Date: 2026-04-21

ALTER TABLE users ADD COLUMN IF NOT EXISTS secret_key VARCHAR(255);

-- Generate initial random keys for existing users (Demo purposes)
UPDATE users SET secret_key = encode(gen_random_bytes(32), 'hex') WHERE secret_key IS NULL;

-- Index for security lookups
CREATE INDEX IF NOT EXISTS idx_users_secret_key ON users(secret_key);
