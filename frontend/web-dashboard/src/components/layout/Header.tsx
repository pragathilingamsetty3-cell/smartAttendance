'use client';

import React from 'react';
import { User, LogOut, Bell, Settings } from 'lucide-react';
import { useAuth } from '@/stores/authContext';
import { Button } from '@/components/ui/Button';
import { cn } from '@/utils/cn';
import Link from 'next/link';

interface HeaderProps {
  className?: string;
}

export const Header: React.FC<HeaderProps> = ({ className }) => {
  const { user, logout, isAuthenticated } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  if (!isAuthenticated || !user) {
    return null;
  }

  return (
    <header className={cn(
      'bg-[#0F0F16]/80 backdrop-blur-xl border-b border-white/5 px-6 py-4 sticky top-0 z-30 shadow-xl shadow-black/40',
      className
    )}>
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link href="/dashboard/settings">
            <Button variant="glass" size="sm" className="hover:text-primary transition-colors">
              <Settings className="h-4 w-4" />
            </Button>
          </Link>
          <h1 className="text-2xl font-semibold text-white tracking-tight">
            Smart Attendance
          </h1>
        </div>

        <div className="flex items-center space-x-4">
          {/* Notifications */}
          <Button
            variant="glass"
            size="sm"
            className="relative"
          >
            <Bell className="h-4 w-4" />
            <span className="absolute -top-1 -right-1 h-3 w-3 bg-red-500 rounded-full"></span>
          </Button>

          {/* User Menu */}
          <div className="flex items-center space-x-3">
            <div className="text-right">
              <p className="text-sm font-medium text-white">
                {user.name}
              </p>
              <p className="text-xs text-gray-400">
                {user.role.replace('_', ' ')}
              </p>
            </div>
            
            <div className="h-8 w-8 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center">
              <User className="h-4 w-4 text-white" />
            </div>
          </div>

          {/* Logout */}
          <Button
            variant="glass"
            size="sm"
            onClick={handleLogout}
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </header>
  );
};

export default Header;
