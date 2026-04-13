// User Management Types based on API_BLUEPRINT.md

export interface StudentOnboardingRequest {
  name: string;
  email: string;
  registrationNumber: string;
  sectionId: string;
  department: string;
  parentEmail: string;
  parentMobile: string;
  studentMobile: string;
  totalAcademicYears: string;
  semester: number;
}

export interface FacultyOnboardingRequest {
  name: string;
  email: string;
  employeeId: string;
  department: string;
  specialization: string;
  mobile: string;
}

export interface AdminOnboardingRequest {
  name: string;
  email: string;
  registrationNumber?: string;
  department: string;
  role: 'ADMIN' | 'SUPER_ADMIN';
}

export interface OnboardingResponseDTO {
  userId: string;
  name: string;
  email: string;
  message: string;
}

export interface DropdownDTO {
  id: string; // UUID mapped to String
  label: string;
  subLabel?: string;
  name?: string;
  code?: string;
  capacity?: number;
  studentCount?: number;
  facultyCount?: number;
}

export interface DepartmentCreateRequest {
  name: string;
  code: string;
  description?: string;
}

export interface SectionCreateRequest {
  name: string;
  departmentId: string;
  program: string;
  capacity: number;
}

export interface BulkPromotionRequest {
  studentIds: string[];
  targetSectionId: string;
  autoIncrementSemester: boolean;
}

export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  reason: string;
}

export interface UserStatusUpdateRequest {
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  reason: string;
}

export interface UserListResponse {
  users: UserListItem[];
  total: number;
}

export interface UserListItem {
  id: string;
  name: string;
  email: string;
  role: string;
  department: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  registrationNumber?: string;
  employeeId?: string;
}

export interface DeviceRegistrationRequest {
  deviceId: string;
  deviceType: string;
  manufacturer: string;
  model: string;
  osVersion: string;
  appVersion: string;
}

export interface BiometricValidationRequest {
  biometricSignature: string;
  deviceId: string;
}

export interface OfflineSyncRequest {
  offlineRecords: OfflineAttendanceRecord[];
}

export interface OfflineAttendanceRecord {
  sessionId: string;
  timestamp: string;
  latitude: number;
  longitude: number;
  attendanceStatus: 'PRESENT' | 'ABSENT';
}

export interface UserUpdateRequest {
  name: string;
  email: string;
  role: string;
  department?: string;
  sectionId?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  studentMobile?: string;
  parentMobile?: string;
  semester?: number;
  totalAcademicYears?: string;
}

export interface UserActivity {
  id: string;
  type: 'ATTENDANCE' | 'SECURITY' | 'ADMIN';
  action: string;
  description: string;
  timestamp: string;
  metadata?: string;
}
