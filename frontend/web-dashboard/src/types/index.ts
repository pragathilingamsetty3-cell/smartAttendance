// Barrel re-exports from modular type files
export * from './auth';
export * from './attendance';
export * from './room-management';
export * from './user-management';
export * from './ai-analytics';
export * from './common';
export * from './emergency';
export * from './student-dashboard';

// --- Enums (from frontend-integration-spec.md Section 3) ---
export enum Role { SUPER_ADMIN = 'SUPER_ADMIN', ADMIN = 'ADMIN', FACULTY = 'FACULTY', STUDENT = 'STUDENT' }
export enum UserStatus { ACTIVE = 'ACTIVE', INACTIVE = 'INACTIVE', SUSPENDED = 'SUSPENDED' }
export enum RoomChangeType { SUDDEN_CHANGE = 'SUDDEN_CHANGE', PRE_PLANNED = 'PRE_PLANNED', WEEKLY_SWAP = 'WEEKLY_SWAP', EMERGENCY_MOVE = 'EMERGENCY_MOVE' }

// --- User Profiles (from frontend-integration-spec.md Section 3) ---
export interface StudentProfileDTO {
  totalAcademicYears: string;
  currentSemester: number;
  enrollmentDate: string;
  expectedGraduationDate: string;
  academicStatus: string | null;
  gpa: number;
  creditsCompleted: number;
  attendancePercentage: number;
}

export interface EnhancedUserDTO {
  id: string;
  name: string;
  email: string;
  registrationNumber: string;
  role: Role;
  status: UserStatus;
  department: string;
  createdAt: string;
  firstLogin: boolean;
  deviceId: string;
  sectionId: string;
  academicProfile?: StudentProfileDTO;
  studentMobile?: string;
  parentMobile?: string;
  phoneNumber?: string;
  studentEmail?: string;
  parentEmail?: string;
}

export interface TokenResponse { accessToken: string; refreshToken: string; user: EnhancedUserDTO; }

// No redundant HallPass DTOs here, using './attendance' re-exports instead.

// --- Attendance Session Types (from backend-architecture.md Section 3.2) ---
export interface AttendanceSession {
  id: string;
  courseId: string;
  facultyId: string;
  roomId: string;
  startTime: string;
  endTime: string;
  isActive: boolean;
  attendanceRecords: AttendanceRecord[];
  room?: {
    id: string;
    name: string;
  };
  section?: {
    id: string;
    name: string;
  };
}

export interface AttendanceRecord {
  id: string;
  studentId: string;
  sessionId: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  timestamp: string;
  location?: {
    latitude: number;
    longitude: number;
  };
  deviceInfo?: {
    deviceFingerprint: string;
    batteryLevel: number;
    isCharging: boolean;
  };
}

// --- Dashboard & Analytics DTOs ---
export interface DashboardStatsDTO {
  totalUsers: number;
  totalStudents?: number;
  activeSessions?: number;
  activeToday?: number;
  anomalies: number;
  attendanceRate: number;
  verifiedCount?: number;
}

export interface AnalyticsTrendDTO {
  time: string;
  value: number;
}

export interface AnomalyBreakdownDTO {
  type: string;
  count: number;
}

export interface AnalyticsDataDTO {
  totalStudents: number;
  liveVerifications: number;
  securityAnomalies: number;
  totalAbsences: number; // Added for the new Absentees card
  velocityTrend: AnalyticsTrendDTO[];
  anomalyBreakdown: AnomalyBreakdownDTO[];
}

// --- CR/LR Assignment (from frontend-integration-spec.md Section 3) ---
export interface CRLRAssignmentRequest {
  studentId: string;
  sectionId: string;
  assignedRole: string; // Either 'CR' or 'LR'
}
