'use client';

import React from 'react';
import { usePathname } from 'next/navigation';
import { MainLayout } from './MainLayout';
import { AuthProvider } from '@/stores/authContext';
import { ErrorBoundary } from '@/components/ui/ErrorBoundary';

/**
 * AppWrapper: Unifies the application layout and authentication state.
 * 
 * This component acts as a high-level router/guard that:
 * 1. Wraps children in AuthProvider for global state access.
 * 2. Conditionally renders MainLayout (with Sidebar/Header) for authenticated routes.
 * 3. Keeps public pages (login, forgot-password) clean of dashboard elements.
 */
export function AppWrapper({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  
  // Define public routes that don't need the dashboard layout
  const publicRoutes = ['/login', '/forgot-password', '/reset-password', '/', '/setup'];
  const isPublicRoute = publicRoutes.includes(pathname || '');

  if (isPublicRoute) {
    return (
      <ErrorBoundary>
        <AuthProvider>
          {children}
        </AuthProvider>
      </ErrorBoundary>
    );
  }

  return (
    <ErrorBoundary>
      <AuthProvider>
        <MainLayout>
          {children}
        </MainLayout>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default AppWrapper;
