-- =====================================
-- Flyway Migration V1: Initial Database Schema with Academic Data Separation
-- =====================================
-- Purpose: Create complete initial schema for Smart Attendance System with clean architecture
-- Author: Flyway Migration
-- Date: 2025-03-30

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable PostGIS extension for geospatial data
CREATE EXTENSION IF NOT EXISTS "postgis";

-- =====================================
-- 1. USERS TABLE
-- =====================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    registration_number VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    first_login BOOLEAN NOT NULL DEFAULT true,
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP WITH TIME ZONE,
    device_id VARCHAR(255),
    biometric_signature TEXT,
    device_fingerprint VARCHAR(255),
    device_registered_at TIMESTAMP WITH TIME ZONE,
    section_id UUID,
    total_academic_years VARCHAR(10), -- Nullable for non-students
    semester INTEGER, -- Nullable for non-students
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    student_mobile VARCHAR(20),
    parent_mobile VARCHAR(20),
    student_email VARCHAR(255),
    parent_email VARCHAR(255),
    is_temporary_password BOOLEAN NOT NULL DEFAULT true,
    otp_code VARCHAR(10),
    otp_expiry TIMESTAMP WITH TIME ZONE
);

-- Create indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_registration_number ON users(registration_number);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_section_id ON users(section_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_department ON users(department);

-- =====================================
-- 2. STUDENT_PROFILES TABLE
-- =====================================
CREATE TABLE student_profiles (
    user_id UUID PRIMARY KEY,
    total_academic_years VARCHAR(10) NOT NULL,
    current_semester INTEGER NOT NULL DEFAULT 1,
    enrollment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expected_graduation_date TIMESTAMP WITH TIME ZONE,
    academic_status VARCHAR(20) DEFAULT 'REGULAR',
    gpa DECIMAL(3,2),
    credits_completed INTEGER DEFAULT 0,
    attendance_percentage DECIMAL(5,2) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for student_profiles table
CREATE INDEX idx_student_profiles_semester ON student_profiles(current_semester);
CREATE INDEX idx_student_profiles_academic_status ON student_profiles(academic_status);
CREATE INDEX idx_student_profiles_total_academic_years ON student_profiles(total_academic_years);
CREATE INDEX idx_student_profiles_gpa ON student_profiles(gpa);
CREATE INDEX idx_student_profiles_attendance ON student_profiles(attendance_percentage);
CREATE INDEX idx_student_profiles_enrollment_date ON student_profiles(enrollment_date);
CREATE INDEX idx_student_profiles_created_at ON student_profiles(created_at);

-- =====================================
-- 3. DEPARTMENTS TABLE
-- =====================================
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for departments table
CREATE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_departments_is_active ON departments(is_active);

-- =====================================
-- 3. SECTIONS TABLE
-- =====================================
CREATE TABLE sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    department_id UUID NOT NULL,
    program VARCHAR(255) NOT NULL,
    batch_year INTEGER NOT NULL,
    total_academic_years VARCHAR(10) NOT NULL,
    current_semester INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    class_advisor_id UUID,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for sections table
CREATE INDEX idx_sections_department_id ON sections(department_id);
CREATE INDEX idx_sections_program ON sections(program);
CREATE INDEX idx_sections_academic_year_semester ON sections(total_academic_years, current_semester);
CREATE INDEX idx_sections_class_advisor_id ON sections(class_advisor_id);
CREATE INDEX idx_sections_is_active ON sections(is_active);

-- =====================================
-- 4. ROOMS TABLE
-- =====================================
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL,
    boundary_polygon GEOMETRY(POLYGON,4326) NOT NULL,
    building VARCHAR(255) NOT NULL,
    floor INTEGER NOT NULL,
    description TEXT
);

-- Create indexes for rooms table
CREATE INDEX idx_rooms_name ON rooms(name);
CREATE INDEX idx_rooms_building_floor ON rooms(building, floor);
CREATE INDEX idx_rooms_boundary_polygon ON rooms USING GIST(boundary_polygon);

-- =====================================
-- 5. TIMETABLES TABLE
-- =====================================
CREATE TABLE timetables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject VARCHAR(255) NOT NULL,
    room_id UUID NOT NULL,
    faculty_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_exam_day BOOLEAN NOT NULL DEFAULT false,
    academic_year VARCHAR(10) NOT NULL,
    semester VARCHAR(10) NOT NULL,
    section_id UUID,
    is_adhoc BOOLEAN NOT NULL DEFAULT false,
    overrides_holiday BOOLEAN NOT NULL DEFAULT false,
    description TEXT,
    -- Break time management fields
    has_lunch_break BOOLEAN DEFAULT false,
    lunch_break_start TIME,
    lunch_break_end TIME,
    has_short_break BOOLEAN DEFAULT false,
    short_break_start TIME,
    short_break_end TIME,
    break_tolerance_minutes INTEGER DEFAULT 10
);

-- Create indexes for timetables table
CREATE INDEX idx_timetables_faculty_id ON timetables(faculty_id);
CREATE INDEX idx_timetables_room_id ON timetables(room_id);
CREATE INDEX idx_timetables_schedule ON timetables(day_of_week, start_time);
CREATE INDEX idx_timetables_section_id ON timetables(section_id);
CREATE INDEX idx_timetables_academic_year_semester ON timetables(academic_year, semester);

-- =====================================
-- 6. CLASSROOM_SESSIONS TABLE
-- =====================================
CREATE TABLE classroom_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timetable_id UUID NOT NULL,
    room_id UUID NOT NULL,
    faculty_id UUID NOT NULL,
    auto_generated BOOLEAN NOT NULL DEFAULT false,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    geofence_polygon GEOMETRY(POLYGON,4326) NOT NULL,
    subject VARCHAR(255),
    section_id UUID,
    is_exam_day BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Create indexes for classroom_sessions table
CREATE INDEX idx_classroom_sessions_timetable_id ON classroom_sessions(timetable_id);
CREATE INDEX idx_classroom_sessions_room_id ON classroom_sessions(room_id);
CREATE INDEX idx_classroom_sessions_faculty_id ON classroom_sessions(faculty_id);
CREATE INDEX idx_classroom_sessions_start_time ON classroom_sessions(start_time);
CREATE INDEX idx_classroom_sessions_section_id ON classroom_sessions(section_id);
CREATE INDEX idx_classroom_sessions_geofence_polygon ON classroom_sessions USING GIST(geofence_polygon);

-- =====================================
-- 7. ATTENDANCE_RECORDS TABLE
-- =====================================
CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    session_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    biometric_signature VARCHAR(500),
    ip_address VARCHAR(45),
    is_mocked BOOLEAN NOT NULL DEFAULT false,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for attendance_records table
CREATE INDEX idx_attendance_records_student_id ON attendance_records(student_id);
CREATE INDEX idx_attendance_records_session_id ON attendance_records(session_id);
CREATE INDEX idx_attendance_records_status ON attendance_records(status);
CREATE INDEX idx_attendance_records_recorded_at ON attendance_records(recorded_at);
CREATE INDEX idx_attendance_records_student_session ON attendance_records(student_id, session_id);

-- =====================================
-- 8. REFRESH_TOKENS TABLE
-- =====================================
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL
);

-- Create indexes for refresh_tokens table
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);

-- =====================================
-- 9. ACADEMIC_CALENDAR TABLE
-- =====================================
CREATE TABLE academic_calendar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL UNIQUE,
    day_type VARCHAR(50) NOT NULL,
    description TEXT,
    affects_all_sections BOOLEAN NOT NULL DEFAULT true,
    affected_sections VARCHAR(255),
    replacement_schedule TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for academic_calendar table
CREATE INDEX idx_academic_calendar_date ON academic_calendar(date);
CREATE INDEX idx_academic_calendar_day_type ON academic_calendar(day_type);
CREATE INDEX idx_academic_calendar_affects_all_sections ON academic_calendar(affects_all_sections);

-- =====================================
-- 10. DEVICE_BINDINGS TABLE
-- =====================================
CREATE TABLE device_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_id VARCHAR(255) NOT NULL UNIQUE,
    device_fingerprint VARCHAR(500),
    device_name VARCHAR(255),
    device_type VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revocation_reason VARCHAR(255)
);

-- Create indexes for device_bindings table
CREATE INDEX idx_device_bindings_user_id ON device_bindings(user_id);
CREATE INDEX idx_device_bindings_device_id ON device_bindings(device_id);
CREATE INDEX idx_device_bindings_is_active ON device_bindings(is_active);

-- =====================================
-- 11. SECURITY_ALERTS TABLE
-- =====================================
CREATE TABLE security_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    alert_type VARCHAR(100) NOT NULL,
    alert_message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ip_address VARCHAR(45),
    user_agent TEXT,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for security_alerts table
CREATE INDEX idx_security_alerts_user_id ON security_alerts(user_id);
CREATE INDEX idx_security_alerts_alert_type ON security_alerts(alert_type);
CREATE INDEX idx_security_alerts_severity ON security_alerts(severity);
CREATE INDEX idx_security_alerts_resolved ON security_alerts(resolved);
CREATE INDEX idx_security_alerts_created_at ON security_alerts(created_at);

-- =====================================
-- 12. WEEKLY_ROOM_SWAPS TABLE
-- =====================================
CREATE TABLE weekly_room_swaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_room_id UUID NOT NULL,
    new_room_id UUID NOT NULL,
    timetable_id UUID NOT NULL,
    swap_date DATE NOT NULL,
    reason VARCHAR(500),
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for weekly_room_swaps table
CREATE INDEX idx_weekly_room_swaps_original_room_id ON weekly_room_swaps(original_room_id);
CREATE INDEX idx_weekly_room_swaps_new_room_id ON weekly_room_swaps(new_room_id);
CREATE INDEX idx_weekly_room_swaps_timetable_id ON weekly_room_swaps(timetable_id);
CREATE INDEX idx_weekly_room_swaps_swap_date ON weekly_room_swaps(swap_date);

-- =====================================
-- 13. ROOM_CHANGE_TRANSITIONS TABLE
-- =====================================
CREATE TABLE room_change_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    changed_by_user_id UUID,
    original_room_id UUID NOT NULL,
    new_room_id UUID NOT NULL,
    transition_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    transition_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    grace_period_minutes INTEGER NOT NULL DEFAULT 15,
    room_change_type VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    notifications_sent BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for room_change_transitions table
CREATE INDEX idx_room_change_transitions_session_id ON room_change_transitions(session_id);
CREATE INDEX idx_room_change_transitions_changed_by_user_id ON room_change_transitions(changed_by_user_id);
CREATE INDEX idx_room_change_transitions_original_room_id ON room_change_transitions(original_room_id);
CREATE INDEX idx_room_change_transitions_new_room_id ON room_change_transitions(new_room_id);
CREATE INDEX idx_room_change_transitions_active ON room_change_transitions(active);
CREATE INDEX idx_room_change_transitions_transition_time ON room_change_transitions(transition_start_time, transition_end_time);

-- =====================================
-- 14. CRLR_ASSIGNMENTS TABLE
-- =====================================
CREATE TABLE cr_lr_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    section_id UUID NOT NULL,
    role_type VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    assigned_by_id UUID,
    active BOOLEAN NOT NULL DEFAULT true,
    academic_year VARCHAR(20) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    notes TEXT,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by_id UUID,
    revocation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for cr_lr_assignments table
CREATE INDEX idx_cr_lr_assignments_user_id ON cr_lr_assignments(user_id);
CREATE INDEX idx_cr_lr_assignments_section_id ON cr_lr_assignments(section_id);
CREATE INDEX idx_cr_lr_assignments_role_type ON cr_lr_assignments(role_type);
CREATE INDEX idx_cr_lr_assignments_active ON cr_lr_assignments(active);
CREATE INDEX idx_cr_lr_assignments_academic_year_semester ON cr_lr_assignments(academic_year, semester);
CREATE INDEX idx_cr_lr_assignments_assigned_at ON cr_lr_assignments(assigned_at);

-- =====================================
-- 15. EMERGENCY_SESSION_CHANGES TABLE
-- =====================================
CREATE TABLE emergency_session_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    changed_by_user_id UUID,
    original_session_id UUID NOT NULL,
    original_room_id UUID,
    new_room_id UUID,
    new_faculty_id UUID,
    original_faculty_id UUID,
    original_start_time TIMESTAMP WITH TIME ZONE,
    original_end_time TIMESTAMP WITH TIME ZONE,
    new_start_time TIMESTAMP WITH TIME ZONE,
    new_end_time TIMESTAMP WITH TIME ZONE,
    reason VARCHAR(500) NOT NULL,
    admin_notes TEXT,
    change_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_by UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    change_effective_at TIMESTAMP WITH TIME ZONE,
    change_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    effective_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    emergency_override BOOLEAN NOT NULL DEFAULT false,
    notify_students BOOLEAN NOT NULL DEFAULT true,
    notify_parents BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for emergency_session_changes table
CREATE INDEX idx_emergency_session_changes_session_id ON emergency_session_changes(session_id);
CREATE INDEX idx_emergency_session_changes_changed_by_user_id ON emergency_session_changes(changed_by_user_id);
CREATE INDEX idx_emergency_session_changes_original_session_id ON emergency_session_changes(original_session_id);
CREATE INDEX idx_emergency_session_changes_new_room_id ON emergency_session_changes(new_room_id);
CREATE INDEX idx_emergency_session_changes_new_faculty_id ON emergency_session_changes(new_faculty_id);
CREATE INDEX idx_emergency_session_changes_status ON emergency_session_changes(status);
CREATE INDEX idx_emergency_session_changes_priority ON emergency_session_changes(priority);

-- =====================================
-- 16. OFFLINE_ATTENDANCE_RECORDS TABLE
-- =====================================
CREATE TABLE offline_attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    session_id UUID NOT NULL,
    client_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    server_timestamp TIMESTAMP WITH TIME ZONE,
    device_fingerprint VARCHAR(255) NOT NULL,
    location_lat DOUBLE PRECISION,
    location_lng DOUBLE PRECISION,
    biometric_signature VARCHAR(500),
    sync_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sync_attempts INTEGER NOT NULL DEFAULT 0,
    last_sync_attempt TIMESTAMP WITH TIME ZONE,
    sync_error_message TEXT,
    is_mocked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for offline_attendance_records table
CREATE INDEX idx_offline_attendance_records_student_id ON offline_attendance_records(student_id);
CREATE INDEX idx_offline_attendance_records_session_id ON offline_attendance_records(session_id);
CREATE INDEX idx_offline_attendance_records_sync_status ON offline_attendance_records(sync_status);
CREATE INDEX idx_offline_attendance_records_client_timestamp ON offline_attendance_records(client_timestamp);

-- =====================================
-- 17. SENSOR_READINGS TABLE (Sensor Fusion)
-- =====================================
CREATE TABLE sensor_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    session_id UUID NOT NULL,
    step_count INTEGER NOT NULL DEFAULT 0,
    acceleration_x FLOAT NOT NULL DEFAULT 0.0,
    acceleration_y FLOAT NOT NULL DEFAULT 0.0,
    acceleration_z FLOAT NOT NULL DEFAULT 0.0,
    is_device_moving BOOLEAN NOT NULL DEFAULT false,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    reading_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    device_fingerprint VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for sensor_readings table
CREATE INDEX idx_sensor_readings_student_session ON sensor_readings(student_id, session_id);
CREATE INDEX idx_sensor_readings_timestamp ON sensor_readings(reading_timestamp);
CREATE INDEX idx_sensor_readings_student_timestamp ON sensor_readings(student_id, reading_timestamp);

-- =====================================
-- 18. SENSOR_FUSION_CONFIG TABLE
-- =====================================
CREATE TABLE sensor_fusion_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_distance_jump_meters DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    min_steps_for_movement INTEGER NOT NULL DEFAULT 5,
    min_acceleration_for_movement DOUBLE PRECISION NOT NULL DEFAULT 2.0,
    walk_out_duration_minutes INTEGER NOT NULL DEFAULT 3,
    drift_detection_window_minutes INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default configuration
INSERT INTO sensor_fusion_config (
    max_distance_jump_meters,
    min_steps_for_movement,
    min_acceleration_for_movement,
    walk_out_duration_minutes,
    drift_detection_window_minutes
) VALUES (
    50.0,
    5,
    2.0,
    3,
    5
);

-- =====================================
-- 19. SPOOFING_ALERTS TABLE
-- =====================================
CREATE TABLE spoofing_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    session_id UUID NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    alert_message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    previous_latitude DOUBLE PRECISION,
    previous_longitude DOUBLE PRECISION,
    step_count_at_alert INTEGER,
    is_device_moving_at_alert BOOLEAN,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for spoofing_alerts table
CREATE INDEX idx_spoofing_alerts_student_session ON spoofing_alerts(student_id, session_id);
CREATE INDEX idx_spoofing_alerts_resolved ON spoofing_alerts(resolved);
CREATE INDEX idx_spoofing_alerts_created_at ON spoofing_alerts(created_at);

-- =====================================
-- FOREIGN KEY CONSTRAINTS
-- =====================================

-- Users foreign keys
ALTER TABLE users 
ADD CONSTRAINT fk_users_section_id 
FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL;

-- Student Profiles foreign keys
ALTER TABLE student_profiles 
ADD CONSTRAINT fk_student_profiles_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Sections foreign keys
ALTER TABLE sections 
ADD CONSTRAINT fk_sections_department_id 
FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT;

ALTER TABLE sections 
ADD CONSTRAINT fk_sections_class_advisor_id 
FOREIGN KEY (class_advisor_id) REFERENCES users(id) ON DELETE SET NULL;

-- Timetables foreign keys
ALTER TABLE timetables 
ADD CONSTRAINT fk_timetables_room_id 
FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

ALTER TABLE timetables 
ADD CONSTRAINT fk_timetables_faculty_id 
FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE timetables 
ADD CONSTRAINT fk_timetables_section_id 
FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL;

-- Classroom Sessions foreign keys
ALTER TABLE classroom_sessions 
ADD CONSTRAINT fk_classroom_sessions_timetable_id 
FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE RESTRICT;

ALTER TABLE classroom_sessions 
ADD CONSTRAINT fk_classroom_sessions_room_id 
FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

ALTER TABLE classroom_sessions 
ADD CONSTRAINT fk_classroom_sessions_faculty_id 
FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE classroom_sessions 
ADD CONSTRAINT fk_classroom_sessions_section_id 
FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL;

-- Attendance Records foreign keys
ALTER TABLE attendance_records 
ADD CONSTRAINT fk_attendance_records_student_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE attendance_records 
ADD CONSTRAINT fk_attendance_records_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

-- Refresh Tokens foreign keys
ALTER TABLE refresh_tokens 
ADD CONSTRAINT fk_refresh_tokens_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Device Bindings foreign keys
ALTER TABLE device_bindings 
ADD CONSTRAINT fk_device_bindings_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Security Alerts foreign keys
ALTER TABLE security_alerts 
ADD CONSTRAINT fk_security_alerts_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE security_alerts 
ADD CONSTRAINT fk_security_alerts_resolved_by 
FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;

-- Weekly Room Swaps foreign keys
ALTER TABLE weekly_room_swaps 
ADD CONSTRAINT fk_weekly_room_swaps_original_room_id 
FOREIGN KEY (original_room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

ALTER TABLE weekly_room_swaps 
ADD CONSTRAINT fk_weekly_room_swaps_new_room_id 
FOREIGN KEY (new_room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

ALTER TABLE weekly_room_swaps 
ADD CONSTRAINT fk_weekly_room_swaps_timetable_id 
FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE;

ALTER TABLE weekly_room_swaps 
ADD CONSTRAINT fk_weekly_room_swaps_approved_by 
FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;

-- Room Change Transitions foreign keys
ALTER TABLE room_change_transitions 
ADD CONSTRAINT fk_room_change_transitions_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

ALTER TABLE room_change_transitions 
ADD CONSTRAINT fk_room_change_transitions_changed_by_user_id 
FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE room_change_transitions 
ADD CONSTRAINT fk_room_change_transitions_original_room_id 
FOREIGN KEY (original_room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

ALTER TABLE room_change_transitions 
ADD CONSTRAINT fk_room_change_transitions_new_room_id 
FOREIGN KEY (new_room_id) REFERENCES rooms(id) ON DELETE RESTRICT;

-- CRLR Assignments foreign keys
ALTER TABLE cr_lr_assignments 
ADD CONSTRAINT fk_cr_lr_assignments_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE cr_lr_assignments 
ADD CONSTRAINT fk_cr_lr_assignments_section_id 
FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE;

ALTER TABLE cr_lr_assignments 
ADD CONSTRAINT fk_cr_lr_assignments_assigned_by_id 
FOREIGN KEY (assigned_by_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE cr_lr_assignments 
ADD CONSTRAINT fk_cr_lr_assignments_revoked_by_id 
FOREIGN KEY (revoked_by_id) REFERENCES users(id) ON DELETE SET NULL;

-- Emergency Session Changes foreign keys
ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_changed_by_user_id 
FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_original_session_id 
FOREIGN KEY (original_session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_new_room_id 
FOREIGN KEY (new_room_id) REFERENCES rooms(id) ON DELETE SET NULL;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_new_faculty_id 
FOREIGN KEY (new_faculty_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_requested_by 
FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT fk_emergency_session_changes_approved_by 
FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;

-- Offline Attendance Records foreign keys
ALTER TABLE offline_attendance_records 
ADD CONSTRAINT fk_offline_attendance_records_student_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE offline_attendance_records 
ADD CONSTRAINT fk_offline_attendance_records_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

-- Sensor Readings foreign keys
ALTER TABLE sensor_readings 
ADD CONSTRAINT fk_sensor_readings_student_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE sensor_readings 
ADD CONSTRAINT fk_sensor_readings_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

-- Spoofing Alerts foreign keys
ALTER TABLE spoofing_alerts 
ADD CONSTRAINT fk_spoofing_alerts_student_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE spoofing_alerts 
ADD CONSTRAINT fk_spoofing_alerts_session_id 
FOREIGN KEY (session_id) REFERENCES classroom_sessions(id) ON DELETE CASCADE;

ALTER TABLE spoofing_alerts 
ADD CONSTRAINT fk_spoofing_alerts_resolved_by 
FOREIGN KEY (resolved_by) REFERENCES users(id);

-- =====================================
-- CHECK CONSTRAINTS
-- =====================================

-- Users table check constraints
ALTER TABLE users 
ADD CONSTRAINT chk_users_role 
CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'FACULTY', 'STUDENT', 'CR', 'LR'));

ALTER TABLE users 
ADD CONSTRAINT chk_users_status 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'DROPPED_OUT', 'SUSPENDED', 'GRADUATED', 'TRANSFERRED', 'RESIGNED'));

-- Student Profiles table check constraints
ALTER TABLE student_profiles 
ADD CONSTRAINT chk_student_profiles_academic_status 
CHECK (academic_status IN ('REGULAR', 'PROBATION', 'ACADEMIC_SUSPENSION', 'ACADEMIC_LEAVE', 'COMPLETED', 'WITHDRAWN'));

ALTER TABLE student_profiles 
ADD CONSTRAINT chk_student_profiles_current_semester 
CHECK (current_semester > 0 AND current_semester <= 12);

ALTER TABLE student_profiles 
ADD CONSTRAINT chk_student_profiles_total_academic_years 
CHECK (total_academic_years ~ '^[1-9][0-9]?$'); -- 1-99 years

ALTER TABLE student_profiles 
ADD CONSTRAINT chk_student_profiles_gpa 
CHECK (gpa >= 0.0 AND gpa <= 4.0);

ALTER TABLE student_profiles 
ADD CONSTRAINT chk_student_profiles_attendance_percentage 
CHECK (attendance_percentage >= 0.0 AND attendance_percentage <= 100.0);

-- Sections table check constraints
ALTER TABLE sections 
ADD CONSTRAINT chk_sections_capacity 
CHECK (capacity > 0);

ALTER TABLE sections 
ADD CONSTRAINT chk_sections_current_semester 
CHECK (current_semester > 0 AND current_semester <= 12);

-- Rooms table check constraints
ALTER TABLE rooms 
ADD CONSTRAINT chk_rooms_capacity 
CHECK (capacity > 0);

ALTER TABLE rooms 
ADD CONSTRAINT chk_rooms_floor 
CHECK (floor >= 0);

-- Timetables table check constraints
ALTER TABLE timetables 
ADD CONSTRAINT chk_timetables_day_of_week 
CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));

ALTER TABLE timetables 
ADD CONSTRAINT chk_timetables_time_order 
CHECK (start_time < end_time);

-- Classroom Sessions table check constraints
ALTER TABLE classroom_sessions 
ADD CONSTRAINT chk_classroom_sessions_time_order 
CHECK (start_time < end_time);

-- Attendance Records table check constraints
ALTER TABLE attendance_records 
ADD CONSTRAINT chk_attendance_records_status 
CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'ON_LEAVE'));

-- Academic Calendar table check constraints
ALTER TABLE academic_calendar 
ADD CONSTRAINT chk_academic_calendar_day_type 
CHECK (day_type IN ('HOLIDAY', 'HALF_DAY', 'EXAM_DAY', 'SPECIAL_EVENT', 'MAKEUP_CLASS'));

-- Device Bindings table check constraints
ALTER TABLE device_bindings 
ADD CONSTRAINT chk_device_bindings_device_type 
CHECK (device_type IN ('MOBILE', 'TABLET', 'LAPTOP', 'DESKTOP', 'OTHER'));

-- Security Alerts table check constraints
ALTER TABLE security_alerts 
ADD CONSTRAINT chk_security_alerts_severity 
CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- Weekly Room Swaps table check constraints
ALTER TABLE weekly_room_swaps 
ADD CONSTRAINT chk_weekly_room_swaps_reason_length 
CHECK (LENGTH(reason) <= 500);

-- Room Change Transitions table check constraints
ALTER TABLE room_change_transitions 
ADD CONSTRAINT chk_room_change_transitions_room_change_type 
CHECK (room_change_type IN ('SUDDEN_CHANGE', 'PRE_PLANNED', 'WEEKLY_SWAP', 'EMERGENCY_MOVE'));

ALTER TABLE room_change_transitions 
ADD CONSTRAINT chk_room_change_transitions_grace_period 
CHECK (grace_period_minutes > 0 AND grace_period_minutes <= 60);

ALTER TABLE room_change_transitions 
ADD CONSTRAINT chk_room_change_transitions_reason_length 
CHECK (LENGTH(reason) <= 500);

-- CRLR Assignments table check constraints
ALTER TABLE cr_lr_assignments 
ADD CONSTRAINT chk_cr_lr_assignments_role_type 
CHECK (role_type IN ('CR', 'LR'));

-- Emergency Session Changes table check constraints
ALTER TABLE emergency_session_changes 
ADD CONSTRAINT chk_emergency_session_changes_priority 
CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT', 'CRITICAL'));

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT chk_emergency_session_changes_status 
CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE emergency_session_changes 
ADD CONSTRAINT chk_emergency_session_changes_reason_length 
CHECK (LENGTH(reason) <= 500);

-- Offline Attendance Records table check constraints
ALTER TABLE offline_attendance_records 
ADD CONSTRAINT chk_offline_attendance_records_sync_status 
CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED', 'DUPLICATE', 'REJECTED'));

-- Sensor Readings table check constraints
ALTER TABLE sensor_readings 
ADD CONSTRAINT chk_sensor_readings_step_count 
CHECK (step_count >= 0);

-- Spoofing Alerts table check constraints
ALTER TABLE spoofing_alerts 
ADD CONSTRAINT chk_spoofing_alerts_severity 
CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

ALTER TABLE spoofing_alerts 
ADD CONSTRAINT chk_spoofing_alerts_alert_type 
CHECK (alert_type IN ('LOCATION_JUMP', 'DEVICE_MOVEMENT', 'ACCELERATION_ANOMALY', 'TIME_DISCREPANCY', 'MULTIPLE_DEVICES', 'SUSPICIOUS_PATTERN'));

-- =====================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================

-- Create or replace function for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for tables with updated_at columns
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sections_updated_at BEFORE UPDATE ON sections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_academic_calendar_updated_at BEFORE UPDATE ON academic_calendar
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cr_lr_assignments_updated_at BEFORE UPDATE ON cr_lr_assignments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sensor_readings_updated_at BEFORE UPDATE ON sensor_readings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sensor_fusion_config_updated_at BEFORE UPDATE ON sensor_fusion_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_student_profiles_updated_at BEFORE UPDATE ON student_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================
-- TABLE COMMENTS FOR DOCUMENTATION
-- =====================================

COMMENT ON TABLE users IS 'Core user table containing all system users including students, faculty, and administrators';
COMMENT ON TABLE student_profiles IS 'Academic profiles for students only, containing semester, GPA, attendance and other academic data';
COMMENT ON TABLE departments IS 'Academic departments organizing various programs and sections';
COMMENT ON TABLE sections IS 'Student sections/batches within departments for academic organization';
COMMENT ON TABLE rooms IS 'Physical classrooms and labs with geofencing boundaries';
COMMENT ON TABLE timetables IS 'Regular class schedules with recurring patterns';
COMMENT ON TABLE classroom_sessions IS 'Individual class session instances generated from timetables';
COMMENT ON TABLE attendance_records IS 'Student attendance records for each classroom session';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';
COMMENT ON TABLE academic_calendar IS 'Academic calendar with holidays, exam days, and special events';
COMMENT ON TABLE device_bindings IS 'Device registration and binding for user authentication';
COMMENT ON TABLE security_alerts IS 'Security alerts and incidents tracking';
COMMENT ON TABLE weekly_room_swaps IS 'Temporary room changes for specific dates';
COMMENT ON TABLE room_change_transitions IS 'Dynamic room changes during active sessions';
COMMENT ON TABLE cr_lr_assignments IS 'Class Representative (CR) and Lab Representative (LR) assignments to users in sections';
COMMENT ON TABLE emergency_session_changes IS 'Emergency modifications to class sessions';
COMMENT ON TABLE offline_attendance_records IS 'Attendance data captured offline and synced later';
COMMENT ON TABLE sensor_readings IS 'Sensor fusion data for movement detection and spoofing prevention';
COMMENT ON TABLE sensor_fusion_config IS 'Configuration parameters for sensor fusion algorithms';
COMMENT ON TABLE spoofing_alerts IS 'Security alerts generated by sensor fusion anomaly detection';

-- =====================================
-- COLUMN COMMENTS FOR KEY FIELDS
-- =====================================

COMMENT ON COLUMN users.status IS 'User lifecycle status: ACTIVE, INACTIVE, DROPPED_OUT, SUSPENDED, GRADUATED, TRANSFERRED, RESIGNED';
COMMENT ON COLUMN users.total_academic_years IS 'Total duration of academic program in years (e.g., 4 for 4-year course)';
COMMENT ON COLUMN users.semester IS 'Current semester progress (1-8 for 4-year course)';
COMMENT ON COLUMN sections.total_academic_years IS 'Total academic years for the section program';
COMMENT ON COLUMN sections.current_semester IS 'Current semester being tracked for this section';
COMMENT ON COLUMN rooms.boundary_polygon IS 'Geofencing boundary for attendance tracking';
COMMENT ON COLUMN classroom_sessions.geofence_polygon IS 'Dynamic geofence for specific session';
COMMENT ON COLUMN attendance_records.biometric_signature IS 'Biometric data for attendance verification';
COMMENT ON COLUMN attendance_records.is_mocked IS 'Flag for potentially fraudulent attendance';
COMMENT ON COLUMN academic_calendar.day_type IS 'Type of day: REGULAR, HOLIDAY, EXAM, SPECIAL_EVENT, WEEKEND';
COMMENT ON COLUMN device_bindings.device_fingerprint IS 'Unique device identifier for security';
COMMENT ON COLUMN security_alerts.severity IS 'Alert severity: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN sensor_readings.reading_timestamp IS 'Exact time when sensor data was captured';
COMMENT ON COLUMN sensor_readings.step_count IS 'Number of steps detected by device accelerometer';
COMMENT ON COLUMN sensor_readings.acceleration_x IS 'X-axis acceleration reading in m/s²';
COMMENT ON COLUMN sensor_readings.acceleration_y IS 'Y-axis acceleration reading in m/s²';
COMMENT ON COLUMN sensor_readings.acceleration_z IS 'Z-axis acceleration reading in m/s²';
COMMENT ON COLUMN sensor_readings.is_device_moving IS 'AI-detected movement state of the device';
COMMENT ON COLUMN sensor_fusion_config.max_distance_jump_meters IS 'Maximum allowed GPS distance jump before triggering alert';
COMMENT ON COLUMN sensor_fusion_config.min_steps_for_movement IS 'Minimum steps required to consider device as moving';
COMMENT ON COLUMN sensor_fusion_config.min_acceleration_for_movement IS 'Minimum acceleration magnitude to detect movement';
COMMENT ON COLUMN spoofing_alerts.alert_type IS 'Type of spoofing detected: LOCATION_JUMP, DEVICE_MOVEMENT, ACCELERATION_ANOMALY, TIME_DISCREPANCY, MULTIPLE_DEVICES, SUSPICIOUS_PATTERN';