'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Role } from '../../types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Role[];
}

export default function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthorized, setIsAuthorized] = useState(false);

  useEffect(() => {
    // In production, sync with Zustand auth store
    const token = localStorage.getItem('accessToken');
    const userStr = localStorage.getItem('user');

    if (!token || !userStr) {
      router.replace('/login');
      return;
    }

    try {
      const user = JSON.parse(userStr);
      
      // If specific roles are required, check against user's role
      if (allowedRoles && allowedRoles.length > 0) {
        if (!allowedRoles.includes(user.role)) {
          console.warn(`User role ${user.role} is not permitted to access ${pathname}`);
          router.replace('/unauthorized'); // or dashboard fallback
          return;
        }
      }

      setIsAuthorized(true);
    } catch (e) {
      console.error('Invalid user data stored locally', e);
      router.replace('/login');
    }
  }, [router, pathname, allowedRoles]);

  if (!isAuthorized) {
    // We can show a futuristic glassmorphism spinner here
    return (
      <div className="flex items-center justify-center min-h-screen bg-black">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500"></div>
      </div>
    );
  }

  return <>{children}</>;
}
