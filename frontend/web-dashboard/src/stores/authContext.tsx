'use client';

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { User, UserRole, LoginRequest } from '@/types/auth';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/stores/authStore';

// Auth state interface (mirrors Zustand store for compatibility)
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

// Auth actions
interface AuthActions {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
  refreshToken: () => Promise<void>;
  updateUser: (user: User) => void;
  getUserRole: () => UserRole | null;
  hasPermission: (permission: string) => boolean;
  hasRole: (roles: UserRole | UserRole[]) => boolean;
  isTokenExpiringSoon: () => boolean;
}

// Combined auth context type
type AuthContextType = AuthState & AuthActions;

// Create context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Provider component
interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const { user, accessToken, setUser, setToken, logout: storeLogout, _hasHydrated } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  const login = async (credentials: LoginRequest): Promise<void> => {
    setError(null);
    try {
      const response = await authService.login(credentials);
      setToken(response.accessToken, response.refreshToken);
      setUser(response.user as any);
    } catch (err: any) {
      const msg = err.message || 'Login failed';
      setError(msg);
      throw err;
    }
  };

  const logout = async (): Promise<void> => {
    await storeLogout();
  };

  const clearError = (): void => {
    setError(null);
  };

  const refreshToken = async (): Promise<void> => {
    try {
      const response = await authService.refreshToken({ 
        refreshToken: useAuthStore.getState().refreshToken || '' 
      });
      setToken(response.token || (response as any).accessToken);
    } catch (err) {
      console.error('Token refresh failed:', err);
      await logout();
    }
  };

  const updateUser = (newUser: User): void => {
    setUser(newUser as any);
  };

  const getUserRole = (): UserRole | null => {
    return (user?.role as UserRole) || null;
  };

  const hasPermission = (permission: string): boolean => {
    return authService.hasPermission(permission);
  };

  const hasRole = (roles: UserRole | UserRole[]): boolean => {
    return authService.hasRole(roles);
  };

  const isTokenExpiringSoon = (): boolean => {
    return authService.isTokenExpiringSoon();
  };

  const value: AuthContextType = {
    user: user as unknown as User,
    isAuthenticated: !!accessToken,
    isLoading: !_hasHydrated,
    error,
    login,
    logout,
    clearError,
    refreshToken,
    updateUser,
    getUserRole,
    hasPermission,
    hasRole,
    isTokenExpiringSoon,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};


// Hook to use auth context
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Selectors for common use cases
export const useUser = (): User | null => {
  const { user } = useAuth();
  return user;
};

export const usePermissions = () => {
  const { hasPermission, hasRole, getUserRole } = useAuth();
  return { hasPermission, hasRole, getUserRole };
};

export default AuthProvider;

