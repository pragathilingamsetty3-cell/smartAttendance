# Frontend Integration Specification - Smart Attendance System (AUDITED V2)

This specification represents the integration layer between the Next.js frontend and the Spring Boot backend. Do not write any React/Next.js code until the architecture has been fully understood and adopted.

## 1. Authentication Flow (JWT) & Zero-Trust Security

The backend employs an **advanced stateless JWT architecture** with zero-trust principles, rate-limiting, device-fingerprinting, and multi-factor validation.

### 1.1 Token Generation & Delivery
- **Login Request**: The frontend sends `{ email, password }` to `POST /api/v1/auth/login`.
- **Response**: The backend returns a JSON payload containing `accessToken`, `refreshToken`, and a `user` object.
- **Zero-Trust Bindings**: The backend validates and binds the token to the `deviceFingerprint`, `clientIP`, `userAgent`, and `geoLocation`. The Next.js frontend **MUST** collect a `deviceFingerprint` (e.g., using a library like FingerprintJS) and send it when required (e.g., in `CompleteSetupRequest` and heartbeat mechanisms).

### 1.2 Storage Mechanism (Frontend)
- **Access Token**: Store the `accessToken` in **memory** (Zustand/React Context) and securely proxy it via Next.js Server Components. 
- **Refresh Token**: Must be stored securely in an **HTTPOnly cookie**. Never expose it to JavaScript directly.
- **CORS Requirements**: The backend strictly permits ports `3000`, `5173`, and `8443` over localhost. Ensure Next.js is running on one of these authorized ports, and always include `credentials: 'include'` to pass cookies.

### 1.3 Refresh Token Logic
- The backend features a `/api/v1/auth/refresh-token` endpoint. 
- **Trigger**: Axios/Fetch interceptor should catch `401 Unauthorized`.
- **Action**: Pause the failed request, fire a request to `POST /api/v1/auth/refresh-token` with `{ "refreshToken": "..." }`.
- **Validation**: The backend will ensure the `deviceFingerprint` of the refresh request matches the original token generation.

---

## 2. Exhaustive Endpoint Map (Aggressive Audit Results)

All backend interactions are prefixed with `$BASE_URL`. Ensure `Authorization: Bearer <token>` is added alongside `Content-Type: application/json`.

| Controller Domain | Method | Endpoint | Expected Payload | Return Type |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/login` | `LoginRequest` | `{ accessToken, refreshToken, user }` |
| **Auth** | `POST` | `/api/v1/auth/refresh-token` | `{ refreshToken: string }` | `TokenResponse` |
| **Auth** | `POST` | `/api/v1/auth/complete-setup` | `CompleteSetupRequest` | `MessageResponse` |
| **Auth** | `POST` | `/api/v1/auth/forgot-password` | `ForgotPasswordRequest` | `MessageResponse` |
| **Auth** | `POST` | `/api/v1/auth/reset-password` | `ResetPasswordWithOTPRequest` | `MessageResponse` |
| **Auth** | `POST` | `/api/v1/auth/change-password` | `ChangePasswordRequest` | `MessageResponse` |
| **Auth** | `POST` | `/api/v1/auth/logout` | None | `MessageResponse` |
| **Admin (Users)** | `POST` | `/api/v1/admin/onboard/student` | `StudentOnboardingRequest`| `OnboardingResponseDTO` |
| **Admin (Users)** | `POST` | `/api/v1/admin/onboard/faculty` | `FacultyOnboardingRequest`| `OnboardingResponseDTO` |
| **Admin (Users)** | `POST` | `/api/v1/admin/onboard/admin` | `AdminOnboardingRequest`| `OnboardingResponseDTO` |
| **Admin (Users)** | `GET` | `/api/v1/admin/users` | None | `EnhancedUserDTO[]` |
| **Admin (Dept)** | `GET` | `/api/v1/admin/departments` | None | `DropdownDTO[]` |
| **Admin (Dept)** | `POST` | `/api/v1/admin/departments` | `DepartmentCreateRequest` | `ResponseEntity` |
| **Admin (Sect)** | `GET` | `/api/v1/admin/departments/{id}/sections`| None | `DropdownDTO[]` |
| **Admin (Sect)** | `POST` | `/api/v1/admin/sections` | `SectionCreateRequest` | `ResponseEntity` |
| **Admin (Rooms)** | `GET` | `/api/v1/admin/rooms` | None | `Room[]` |
| **Admin (Rooms)** | `POST` | `/api/v1/admin/rooms` | `RoomCreationRequest` | `ResponseEntity` |
| **Boundary Mgmt** | `POST` | `/api/v1/admin/boundaries/rectangle` | `Map<String, Any>` | `ResponseEntity` |
| **Boundary Mgmt** | `POST` | `/api/v1/admin/boundaries/circle` | `Map<String, Any>` | `ResponseEntity` |
| **Boundary Mgmt** | `POST` | `/api/v1/admin/boundaries/l-shape` | `Map<String, Any>` | `ResponseEntity` |
| **Room Change** | `POST` | `/api/v1/room-change/qr` | `QRRoomChangeRequest` | `ResponseEntity` |
| **Room Change** | `POST` | `/api/v1/room-change/pre-planned` | `RoomChangeRequest` | `ResponseEntity` |
| **Room Change** | `POST` | `/api/v1/room-change/weekly-swap-config`| `WeeklyRoomSwapConfig` | `ResponseEntity` |
| **Emergency** | `POST` | `/api/v1/emergency/session-change` | `EmergencySessionChangeRequest` | `ResponseEntity` |
| **Emergency** | `POST` | `/api/v1/emergency/substitute-claim` | `SubstituteClaimRequest` | `ResponseEntity` |
| **Emergency** | `POST` | `/api/v1/emergency/quick-room-change`| None | `ResponseEntity` |
| **CR/LR Assign** | `POST` | `/api/v1/cr-lr-assignments/assign` | `CRLRAssignmentRequest` | `ResponseEntity` |
| **CR/LR Assign** | `GET` | `/api/v1/cr-lr-assignments/section/{id}` | None | `ResponseEntity` |
| **Device Mgmt** | `POST` | `/api/v1/device/register` | `DeviceRegistrationRequest` | `ResponseEntity` |
| **Device Mgmt** | `POST` | `/api/v1/device/validate-biometric` | `BiometricValidationRequest`| `ResponseEntity` |
| **Device Mgmt** | `POST` | `/api/v1/device/sync-offline` | `OfflineSyncRequest` | `ResponseEntity` |
| **Faculty & Sessions**| `POST` | `/api/v1/faculty/hall-pass/request` | `HallPassRequestDTO` | `ResponseEntity` |
| **Faculty & Sessions**| `GET` | `/api/v1/faculty/hall-pass/pending` | None | `ResponseEntity` |
| **Faculty & Sessions**| `POST` | `/api/v1/faculty/hall-pass/approve` | `HallPassApprovalRequest` | `ResponseEntity` |
| **Exam Day** | `POST` | `/api/v1/exam/scan-barcode` | `ExamBarcodeScanRequest` | `ResponseEntity` |
| **Exam Day** | `GET` | `/api/v1/exam/today-sessions` | None | `ResponseEntity` |
| **Attendance** | `POST` | `/api/v1/attendance/heartbeat` | `EnhancedHeartbeatPing` | `ResponseEntity` |
| **Attendance** | `GET` | `/api/v1/attendance/sensor-status/{s}/{u}`| None | `ResponseEntity` |
| **Reports** | `GET` | `/api/v1/reports/attendance/excel` | None | `byte[] (File Download)` |
| **AI Analytics** | `POST` | `/api/v1/ai-analytics/spatial-analysis/{s}/{s}`| None | `ResponseEntity` |
| **Performance** | `GET` | `/api/v1/performance/metrics` | None | `JSON Metrics` |
| **Monitoring** | `GET` | `/api/v1/monitoring/system-metrics` | None | `JSON Metrics` |

*(This table reflects 86 active production-ready endpoints discovered during the audit. Test and debug endpoints have been removed.)*

---

## 3. Precise Data Interfaces (TypeScript)

These map to exact Java properties, accounting for `Instant` -> `string` representations and Optional wrappers.

```typescript
// --- Enums ---
export enum Role { SUPER_ADMIN = 'SUPER_ADMIN', ADMIN = 'ADMIN', FACULTY = 'FACULTY', STUDENT = 'STUDENT' }
export enum UserStatus { ACTIVE = 'ACTIVE', INACTIVE = 'INACTIVE', SUSPENDED = 'SUSPENDED' }
export enum RoomChangeType { SUDDEN_CHANGE = 'SUDDEN_CHANGE', PRE_PLANNED = 'PRE_PLANNED', WEEKLY_SWAP = 'WEEKLY_SWAP', EMERGENCY_MOVE = 'EMERGENCY_MOVE' }

// --- User Profiles ---
export interface StudentProfileDTO {
  totalAcademicYears: string;
  currentSemester: number; // Mapped from Java Integer
  enrollmentDate: string; // ISO 8601 string from Java Instant
  expectedGraduationDate: string; 
  academicStatus: string | null;
  gpa: number; // Mapped from Java Double
  creditsCompleted: number;
  attendancePercentage: number;
}

export interface EnhancedUserDTO {
  id: string; // UUID
  name: string;
  email: string;
  registrationNumber: string;
  role: Role;
  status: UserStatus;
  department: string;
  createdAt: string; 
  firstLogin: boolean;
  deviceId: string;
  sectionId: string; // UUID
  academicProfile?: StudentProfileDTO; // Optional Nested Object
  studentMobile?: string;
  parentMobile?: string;
  studentEmail?: string;
  parentEmail?: string;
}

// --- Payloads ---
export interface LoginRequest { email: string; password?: string; }
export interface TokenResponse { accessToken: string; refreshToken: string; user: EnhancedUserDTO; }

export interface CompleteSetupRequest {
  deviceId: string;
  deviceFingerprint: string;
  biometricSignature?: string;
}

// Ensure OTP and newpass match Spring Boot security validators
export interface ResetPasswordWithOTPRequest {
  emailOrPhone: string;
  otp: string;
  newPassword: string;
  confirmPassword: string;
}

// Room Definitions
export interface RoomChangeRequest {
  roomId: string; // UUID
  changeType: RoomChangeType; 
  sectionId?: string; // UUID 
  facultyId?: string; // UUID
  scheduledTime?: string; // ISO 8601 string from Java Instant
  swapWithRoomId?: string; // UUID
  swapWithSectionId?: string; // UUID
  reason?: string;
  notifyStudents: boolean; // Sent as true by default
  notifyFaculty: boolean;
  notifyParents: boolean;
}

// Zero-Trust Device & Heartbeat Models
export interface EnhancedHeartbeatPing {
  studentId: string;
  latitude: number;
  longitude: number;
  stepCount: number;
  accelerationX: number;
  accelerationY: number;
  accelerationZ: number;
  isDeviceMoving: boolean;
  timestamp: string;
  deviceFingerprint: string;     // CRITICAL for JwtUtil bindings
  batteryLevel: number;
  isCharging: boolean;
  isScreenOn: boolean;
  deviceState: string;
  nextHeartbeatInterval: number; // Mapped from Java Integer
}

export interface CRLRAssignmentRequest {
  studentId: string;
  sectionId: string;
  assignedRole: string; // Either 'CR' or 'LR'
}

export interface DropdownDTO {
  id: string; // UUID mapped to String
  label: string;
}

export interface OnboardingResponseDTO {
  userId: string;
  name: string;
  email: string;
  message: string;
}
```
