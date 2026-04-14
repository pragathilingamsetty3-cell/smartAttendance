/**
 * Application constants for the smart attendance system
 */

// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_URL || 'https://localhost:8443',
  WS_URL: process.env.NEXT_PUBLIC_WS_URL || 'wss://localhost:8443/ws',
  TIMEOUT: 30000,
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000,
} as const;

// Application Configuration
export const APP_CONFIG = {
  NAME: 'Smart Attendance System',
  VERSION: '1.0.0',
  DESCRIPTION: 'AI-powered attendance management system',
  AUTHOR: 'Smart Attendance Team',
} as const;

// User Roles
export const USER_ROLES = {
  STUDENT: 'STUDENT',
  FACULTY: 'FACULTY',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
} as const;

// Attendance Status
export const ATTENDANCE_STATUS = {
  PRESENT: 'PRESENT',
  ABSENT: 'ABSENT',
  LATE: 'LATE',
  EXCUSED: 'EXCUSED',
  ON_LEAVE: 'ON_LEAVE',
} as const;

// Session Status
export const SESSION_STATUS = {
  CREATED: 'CREATED',
  ACTIVE: 'ACTIVE',
  PAUSED: 'PAUSED',
  ENDED: 'ENDED',
  CANCELLED: 'CANCELLED',
} as const;

// Room Types
export const ROOM_TYPES = {
  CLASSROOM: 'CLASSROOM',
  LAB: 'LAB',
  LECTURE_HALL: 'LECTURE_HALL',
  CONFERENCE_ROOM: 'CONFERENCE_ROOM',
  LIBRARY: 'LIBRARY',
  AUDITORIUM: 'AUDITORIUM',
} as const;

// Boundary Types
export const BOUNDARY_TYPES = {
  RECTANGLE: 'RECTANGLE',
  CIRCLE: 'CIRCLE',
  POLYGON: 'POLYGON',
} as const;

// Hall Pass Status
export const HALL_PASS_STATUS = {
  PENDING_FACULTY_APPROVAL: 'PENDING_FACULTY_APPROVAL',
  APPROVED: 'APPROVED',
  DENIED: 'DENIED',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED',
} as const;

// AI Risk Levels
export const AI_RISK_LEVELS = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL',
} as const;

// Behavior Patterns
export const BEHAVIOR_PATTERNS = {
  CONSISTENT: 'CONSISTENT',
  ERRATIC: 'ERRATIC',
  SUSPICIOUS: 'SUSPICIOUS',
  NORMAL: 'NORMAL',
  ANOMALOUS: 'ANOMALOUS',
} as const;

// Validation Rules
export const VALIDATION_RULES = {
  PASSWORD_MIN_LENGTH: 8,
  PASSWORD_MAX_LENGTH: 128,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 50,
  EMAIL_MAX_LENGTH: 254,
  ROOM_NAME_MAX_LENGTH: 100,
  BUILDING_NAME_MAX_LENGTH: 100,
  REASON_MAX_LENGTH: 500,
} as const;

// File Upload Limits
export const FILE_UPLOAD_LIMITS = {
  MAX_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_TYPES: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'application/pdf',
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ],
} as const;

// Pagination
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  MAX_PAGE_SIZE: 100,
  PAGE_SIZE_OPTIONS: [10, 25, 50, 100],
} as const;

// Time Intervals (in milliseconds)
export const TIME_INTERVALS = {
  HEARTBEAT: 5000, // 5 seconds
  REAL_TIME_UPDATE: 10000, // 10 seconds
  AI_ANALYSIS: 30000, // 30 seconds
  SESSION_REFRESH: 60000, // 1 minute
  TOKEN_REFRESH: 4 * 60 * 60 * 1000, // 4 hours
  INACTIVITY_WARNING: 13 * 60 * 1000, // 13 minutes
  INACTIVITY_TIMEOUT: 15 * 60 * 1000, // 15 minutes
} as const;

// GPS Configuration
export const GPS_CONFIG = {
  ACCURACY_THRESHOLD: 20, // meters
  DRIFT_THRESHOLD: 50, // meters
  UPDATE_INTERVAL: 5000, // milliseconds
  MAX_AGE: 30000, // milliseconds
  TIMEOUT: 10000, // milliseconds
} as const;

// AI Configuration
export const AI_CONFIG = {
  WALK_OUT_RISK_THRESHOLD: 0.7,
  ANOMALY_CONFIDENCE_THRESHOLD: 0.8,
  BEHAVIOR_PATTERN_THRESHOLD: 0.6,
  PREDICTION_CONFIDENCE_THRESHOLD: 0.75,
  MODEL_RETRAIN_INTERVAL: 7 * 24 * 60 * 60 * 1000, // 7 days
} as const;

// UI Configuration
export const UI_CONFIG = {
  TOAST_DURATION: 5000, // milliseconds
  MODAL_ANIMATION_DURATION: 300, // milliseconds
  DEBOUNCE_DELAY: 300, // milliseconds
  THROTTLE_DELAY: 1000, // milliseconds
  VIRTUAL_SCROLL_THRESHOLD: 100, // items
  LAZY_LOAD_THRESHOLD: 200, // pixels
} as const;

// Cache Configuration
export const CACHE_CONFIG = {
  USER_DATA_TTL: 5 * 60 * 1000, // 5 minutes
  ROOM_DATA_TTL: 10 * 60 * 1000, // 10 minutes
  ATTENDANCE_DATA_TTL: 2 * 60 * 1000, // 2 minutes
  AI_DATA_TTL: 30 * 60 * 1000, // 30 minutes
  MAX_CACHE_SIZE: 1000, // items
} as const;

// Error Messages
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network connection failed. Please check your internet connection.',
  UNAUTHORIZED: 'You are not authorized to perform this action.',
  FORBIDDEN: 'You do not have permission to access this resource.',
  NOT_FOUND: 'The requested resource was not found.',
  VALIDATION_ERROR: 'Please check your input and try again.',
  SERVER_ERROR: 'An unexpected error occurred. Please try again later.',
  TIMEOUT_ERROR: 'The request timed out. Please try again.',
  UNKNOWN_ERROR: 'An unexpected error occurred.',
  INVALID_CREDENTIALS: 'Invalid email or password.',
  ACCOUNT_LOCKED: 'Your account has been locked. Please contact support.',
  EMAIL_VERIFICATION_REQUIRED: 'Please verify your email address.',
  PASSWORD_TOO_WEAK: 'Password does not meet security requirements.',
  DUPLICATE_EMAIL: 'An account with this email already exists.',
  INVALID_TOKEN: 'Your session has expired. Please log in again.',
  ROOM_NOT_AVAILABLE: 'The selected room is not available at this time.',
  BOUNDARY_INVALID: 'The room boundary is invalid or too small.',
  GPS_ACCURACY_LOW: 'GPS accuracy is too low for reliable tracking.',
  DEVICE_NOT_SUPPORTED: 'Your device is not supported for this feature.',
  BROWSER_NOT_SUPPORTED: 'Your browser is not supported. Please upgrade to a modern browser.',
} as const;

// Success Messages
export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'Login successful!',
  LOGOUT_SUCCESS: 'Logged out successfully!',
  PROFILE_UPDATED: 'Profile updated successfully!',
  PASSWORD_CHANGED: 'Password changed successfully!',
  USER_CREATED: 'User created successfully!',
  USER_UPDATED: 'User updated successfully!',
  USER_DELETED: 'User deleted successfully!',
  ROOM_CREATED: 'Room created successfully!',
  ROOM_UPDATED: 'Room updated successfully!',
  ROOM_DELETED: 'Room deleted successfully!',
  SESSION_STARTED: 'Session started successfully!',
  SESSION_PAUSED: 'Session paused successfully!',
  SESSION_RESUMED: 'Session resumed successfully!',
  SESSION_ENDED: 'Session ended successfully!',
  HALL_PASS_APPROVED: 'Hall pass approved!',
  HALL_PASS_DENIED: 'Hall pass denied!',
  DATA_EXPORTED: 'Data exported successfully!',
  DATA_IMPORTED: 'Data imported successfully!',
  SETTINGS_SAVED: 'Settings saved successfully!',
} as const;

// Warning Messages
export const WARNING_MESSAGES = {
  UNSAVED_CHANGES: 'You have unsaved changes. Are you sure you want to leave?',
  DELETE_CONFIRMATION: 'Are you sure you want to delete this item?',
  SESSION_END_WARNING: 'Ending the session will stop all attendance tracking. Continue?',
  ROOM_CHANGE_WARNING: 'Changing rooms will affect ongoing sessions. Continue?',
  LOGOUT_WARNING: 'Are you sure you want to log out?',
  INACTIVITY_WARNING: 'You will be logged out due to inactivity in 2 minutes.',
  GPS_PERMISSION_DENIED: 'GPS permission is required for attendance tracking.',
  CAMERA_PERMISSION_DENIED: 'Camera permission is required for QR code scanning.',
  BROWSER_COMPATIBILITY: 'Some features may not work properly in your browser.',
  NETWORK_UNSTABLE: 'Network connection is unstable. Some features may be affected.',
} as const;

// Info Messages
export const INFO_MESSAGES = {
  LOADING: 'Loading...',
  PROCESSING: 'Processing...',
  SAVING: 'Saving...',
  UPDATING: 'Updating...',
  DELETING: 'Deleting...',
  SYNCING: 'Syncing data...',
  NO_DATA: 'No data available.',
  NO_RESULTS: 'No results found.',
  FILTER_APPLIED: 'Filter applied successfully.',
  FILTER_CLEARED: 'Filter cleared successfully.',
  SORT_APPLIED: 'Sort applied successfully.',
  SORT_CLEARED: 'Sort cleared successfully.',
  SESSION_ACTIVE: 'Session is currently active.',
  SESSION_PAUSED: 'Session is currently paused.',
  GPS_TRACKING_ACTIVE: 'GPS tracking is active.',
  GPS_TRACKING_INACTIVE: 'GPS tracking is inactive.',
} as const;

// Color Palette
export const COLORS = {
  PRIMARY: '#3B82F6',
  SECONDARY: '#10B981',
  SUCCESS: '#10B981',
  WARNING: '#F59E0B',
  ERROR: '#EF4444',
  INFO: '#3B82F6',
  GRAY: '#6B7280',
  DARK: '#1F2937',
  LIGHT: '#F9FAFB',
  WHITE: '#FFFFFF',
  BLACK: '#000000',
} as const;

// Breakpoints
export const BREAKPOINTS = {
  SM: 640,
  MD: 768,
  LG: 1024,
  XL: 1280,
  XXL: 1536,
} as const;

// Animation Durations
export const ANIMATIONS = {
  FAST: 150,
  NORMAL: 300,
  SLOW: 500,
  EXTRA_SLOW: 1000,
} as const;

// Z-Index Values
export const Z_INDEX = {
  DROPDOWN: 1000,
  MODAL: 1050,
  TOAST: 1100,
  LOADING: 1200,
  TOOLTIP: 1300,
  OVERLAY: 1400,
} as const;

// Keyboard Shortcuts
export const KEYBOARD_SHORTCUTS = {
  CTRL_K: 'Ctrl+K', // Command palette
  CTRL_S: 'Ctrl+S', // Save
  CTRL_N: 'Ctrl+N', // New
  CTRL_E: 'Ctrl+E', // Export
  CTRL_I: 'Ctrl+I', // Import
  ESC: 'Escape', // Close modal/cancel
  ENTER: 'Enter', // Confirm/submit
  SPACE: 'Space', // Toggle/select
  ARROW_UP: 'ArrowUp', // Navigate up
  ARROW_DOWN: 'ArrowDown', // Navigate down
  ARROW_LEFT: 'ArrowLeft', // Navigate left
  ARROW_RIGHT: 'ArrowRight', // Navigate right
} as const;

// Regular Expressions
export const REGEX_PATTERNS = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>])[A-Za-z\d!@#$%^&*(),.?":{}|<>]{8,}$/,
  PHONE: /^[\+]?[1-9][\d]{0,15}$/,
  USERNAME: /^[a-zA-Z0-9_]{3,50}$/,
  ROOM_CODE: /^[A-Z0-9]{3,10}$/,
  STUDENT_ID: /^[A-Z0-9]{6,20}$/,
  URL: /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$/,
} as const;

// Default Values
export const DEFAULTS = {
  PAGE_SIZE: 10,
  TIMEZONE: 'UTC',
  LOCALE: 'en-US',
  CURRENCY: 'USD',
  DATE_FORMAT: 'YYYY-MM-DD',
  TIME_FORMAT: '24h',
  THEME: 'dark',
  LANGUAGE: 'en',
} as const;

// Feature Flags
export const FEATURE_FLAGS = {
  AI_ANALYTICS: true,
  GPS_TRACKING: true,
  QR_SCANNING: true,
  HALL_PASS: true,
  ROOM_MANAGEMENT: true,
  BULK_OPERATIONS: true,
  EXPORT_IMPORT: true,
  REAL_TIME_UPDATES: true,
  NOTIFICATIONS: true,
  REPORTS: true,
} as const;
