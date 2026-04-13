-- Test Data Setup Script for Smart Attendance System (H2 Compatible)
-- This script creates the necessary test data for integration tests
-- It runs before each test method to ensure clean test data

-- Clean up any existing test data (only if tables exist)
-- Note: H2 doesn't support IF EXISTS for DELETE statements, so we'll handle this gracefully

-- Insert test departments (using UUIDs)
INSERT INTO departments (id, name, code, description, is_active, created_at, updated_at) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'Computer Science', 'CS', 'Department of Computer Science and Engineering', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440002', 'Electrical Engineering', 'EE', 'Department of Electrical Engineering', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440003', 'Mechanical Engineering', 'ME', 'Department of Mechanical Engineering', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test sections (using UUIDs)
INSERT INTO sections (id, name, department_id, academic_year, semester, is_active, created_at, updated_at) VALUES 
('660e8400-e29b-41d4-a716-446655440001', 'CS-A', '550e8400-e29b-41d4-a716-446655440001', '2024', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('660e8400-e29b-41d4-a716-446655440002', 'CS-B', '550e8400-e29b-41d4-a716-446655440001', '2024', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('660e8400-e29b-41d4-a716-446655440003', 'EE-A', '550e8400-e29b-41d4-a716-446655440002', '2024', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('660e8400-e29b-41d4-a716-446655440004', 'ME-A', '550e8400-e29b-41d4-a716-446655440003', '2024', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;
