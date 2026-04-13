import { useAuthStore } from '../stores/authStore';
import apiClient from '../lib/apiClient';
import {
  LoginRequest,
  LoginResponse,
  CompleteSetupRequest,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  ResetPasswordWithOTPRequest,
  RefreshTokenRequest,
  RefreshTokenResponse,
  User,
  UserRole,
} from '../types/auth';
import { createErrorHandler } from '../utils/errorHandler';
// Note: Token management is now handled centrally by apiClient.ts and authStore.ts

const handleAuthError = createErrorHandler('AuthService');

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', credentials);
      return response.data;
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async completeSetup(request: CompleteSetupRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/auth/complete-setup', request);
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/auth/change-password', request);
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async forgotPassword(request: ForgotPasswordRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/auth/forgot-password', request);
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async resetPassword(request: ResetPasswordWithOTPRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/auth/reset-password', request);
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async refreshToken(request: RefreshTokenRequest): Promise<RefreshTokenResponse> {
    try {
      const response = await apiClient.post<RefreshTokenResponse>('/api/v1/auth/refresh-token', request);
      return response.data;
    } catch (error: unknown) {
      throw handleAuthError(error, '');
    }
  }

  async logout(): Promise<void> {
    try {
      await apiClient.post('/api/v1/auth/logout');
    } catch (error) {
      console.error('Logout API call failed:', error);
    } // Token clearing is handled by the caller via useAuthStore().logout()
  }

  // --- Auth state helpers for AuthContext compatibility ---
  isAuthenticated(): boolean {
    return !!useAuthStore.getState().accessToken;
  }

  getCurrentUser(): User | null {
    const user = useAuthStore.getState().user;
    if (!user) return null;
    return user as unknown as User;
  }

  getToken(): string | null {
    return useAuthStore.getState().accessToken;
  }

  hasPermission(permission: string): boolean {
    const user = this.getCurrentUser();
    // Simplified stub
    return !!user;
  }

  hasRole(roles: UserRole | UserRole[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    
    // Normalize user role by removing 'ROLE_' prefix if present
    const userRole = user.role.replace('ROLE_', '');
    const roleArr = Array.isArray(roles) ? roles : [roles];
    
    // Check against normalized strings
    return roleArr.some(role => role.replace('ROLE_', '') === userRole);
  }


  isTokenExpiringSoon(): boolean {
    return false; // Stub
  }
}

export const authService = new AuthService();
export default authService;
