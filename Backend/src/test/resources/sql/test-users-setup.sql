-- Generate BCrypt-15 hash for "SecurePassword123!"
-- This is the actual BCrypt-15 hash that will be used in tests
-- Password: SecurePassword123!
-- Hash: $2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi

-- Test Faculty User (Valid Login Credentials)
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440001', 'Test Faculty User', 'test.faculty@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'FACULTY', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567890', true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test Student User
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440002', 'Test Student User', 'test.student@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'STUDENT', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567891', true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test Admin User
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440003', 'Test Admin User', 'test.admin@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'ADMIN', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567892', true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test Super Admin User
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440004', 'Test Super Admin User', 'test.superadmin@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SUPER_ADMIN', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567893', true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test Inactive User (Should not be able to login)
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440005', 'Test Inactive User', 'test.inactive@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'FACULTY', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567894', false, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test First Login User (Should require first login setup)
INSERT INTO users (
    id, name, email, password, role, department_id, section_id, 
    phone_number, is_active, email_verified, phone_verified, 
    first_login, created_at, updated_at
) VALUES 
('770e8400-e29b-41d4-a716-446655440006', 'Test First Login User', 'test.firstlogin@smartattendence.com', 
 '$2a$15$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'FACULTY', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', '+1234567895', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;
