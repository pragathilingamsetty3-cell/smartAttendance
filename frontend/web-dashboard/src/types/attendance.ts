// Attendance Types based on API_BLUEPRINT.md

export interface EnhancedHeartbeatPing {
  studentId: string;
  sessionId: string;
  latitude: number;
  longitude: number;
  stepCount: number;
  accelerationX: number;
  accelerationY: number;
  accelerationZ: number;
  isDeviceMoving: boolean;
  timestamp: string;
  deviceFingerprint: string;
  biometricSignature?: string;  // 🔐 Fingerprint verification signature
  batteryLevel?: number;
  isCharging?: boolean;
  isScreenOn?: boolean;
  deviceState?: 'STATIONARY' | 'MOVING' | 'WALKING';
  nextHeartbeatInterval?: number;
  requestSignature?: string;    // 🔐 HMAC-SHA256 Signature
  sequenceId?: number;          // 📈 Reliability Sequence ID
}

export interface HeartbeatResponse {
  message: string;
  timestamp: string;
  sensorDataProcessed: boolean;
  batteryOptimization: {
    currentBatteryLevel?: number;
    deviceState?: string;
    recommendedInterval: number;
    batteryMode: 'EMERGENCY_MODE' | 'BATTERY_SAVER' | 'BALANCED' | 'PERFORMANCE_MODE';
  };
  gpsOptimization: {
    gpsMode: string;
    accuracyMeters: number;
    updateIntervalMs: number;
    reason: string;
    needsHighAccuracy: boolean;
    deviceMotionState: string;
  };
  aiLearning: {
    optimalHeartbeatInterval: number;
    recommendedGPSMode: string;
    confidence: number;
    reasoning: string;
    totalSessionsAnalyzed: number;
    accuracyScore: number;
    learningStatus: 'HIGH_CONFIDENCE' | 'LEARNING';
  };
}

export interface HallPassRequestDTO {
  studentId: string;
  sessionId: string;
  requestedMinutes: number;
  reason: string;
}

export interface HallPassStatusDTO {
  requestId: string;
  studentId: string;
  studentName: string;
  sessionId: string;
  requestedMinutes: number;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'DENIED' | 'EXPIRED';
  requestedAt: string;
  processedAt?: string;
  processedBy?: string;
  facultyNotes?: string;
}

export interface HallPassApprovalRequest {
  studentId: string;
  sessionId: string;
  approvedMinutes: number;
}

export interface HallPassDenialRequest {
  studentId: string;
  sessionId: string;
  reason: string;
}

export interface SensorStatusResponse {
  sessionId: string;
  studentId: string;
  recentReadingsCount: number;
  lastReading?: string;
  motionAnalysis?: {
    motionState: string;
    confidence: number;
    recommendations: string[];
  };
}

export interface AttendanceSession {
  id: string;
  courseId: string;
  facultyId: string;
  roomId: string;
  startTime: string;
  endTime: string;
  isActive: boolean;
  attendanceRecords: AttendanceRecord[];
  room?: { id: string; name: string };
  section?: { id: string; name: string };
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
