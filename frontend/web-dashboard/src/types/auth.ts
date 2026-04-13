// Authentication Types based on API_BLUEPRINT.md

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'FACULTY' | 'STUDENT' | 'CR' | 'LR';

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  registrationNumber?: string;
  employeeId?: string;
  department?: string;
  sectionId?: string;
  semester?: number;
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  requiresFirstLoginSetup: boolean;
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: User;
}

export interface CompleteSetupRequest {
  deviceId?: string;
  biometricSignature?: string;
  phoneNumber?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ForgotPasswordRequest {
  emailOrPhone: string;
  method?: 'EMAIL' | 'PHONE';
}

export interface ResetPasswordWithOTPRequest {
  emailOrPhone: string;
  otp: string;
  newPassword: string;
  confirmPassword: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  token: string;
  refreshToken: string;
}

// JWT Token interface
export interface JWTPayload {
  email: string;
  role: UserRole;
  deviceFingerprint: string;
  sessionId: string;
  clientIP: string;
  userAgent: string;
  geoLocation: string;
  iat: number;
  exp: number;
}
