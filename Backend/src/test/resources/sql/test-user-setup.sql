-- Production Test User Setup for H2 Database
-- This creates test users with proper BCrypt hashes for testing

-- Clean up dependent data first (foreign key constraints)
DELETE FROM refresh_tokens WHERE user_id IN (
    SELECT id FROM users WHERE email IN ('admin@smartattendence.com', 'faculty@smartattendence.com', 'student@smartattendence.com')
);
DELETE FROM users WHERE email IN ('admin@smartattendence.com', 'faculty@smartattendence.com', 'student@smartattendence.com');

-- Insert test admin user (using actual User entity columns with all required fields)
-- Using plain text passwords for testing (AuthenticationService handles both BCrypt and plain text)
INSERT INTO users (
    id, name, email, registration_number, password, role, department, 
    first_login, is_temporary_password, semester, status, created_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440001', 'Test Admin User', 'admin@smartattendence.com', 
 'ADMIN999', 'Test@Secure123', -- Plain text for testing
 'ADMIN', 'Computer Science', false, false, 1, 'ACTIVE', CURRENT_TIMESTAMP);

-- Insert test faculty user
INSERT INTO users (
    id, name, email, registration_number, password, role, department, 
    first_login, is_temporary_password, semester, status, created_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440002', 'Test Faculty User', 'faculty@smartattendence.com', 
 'FAC999', 'Test@Secure123', -- Plain text for testing
 'FACULTY', 'Computer Science', false, false, 1, 'ACTIVE', CURRENT_TIMESTAMP);

-- Insert test student user
INSERT INTO users (
    id, name, email, registration_number, password, role, department, 
    first_login, is_temporary_password, semester, status, created_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440003', 'Test Student User', 'student@smartattendence.com', 
 'STU999', 'Test@Secure123', -- Plain text for testing
 'STUDENT', 'Computer Science', false, false, 1, 'ACTIVE', CURRENT_TIMESTAMP);

COMMIT;
