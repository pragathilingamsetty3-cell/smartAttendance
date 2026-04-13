'use client';

import React, { useState, useEffect } from 'react';
import { 
  Home, 
  Users, 
  Map, 
  Activity, 
  Settings, 
  Brain, 
  Shield,
  Calendar,
  ArrowUp,
  FileText
} from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { Header } from './Header';
import { cn } from '@/utils/cn';
import Link from 'next/link';
import { motion } from 'framer-motion';

interface MainLayoutProps {
  children: React.ReactNode;
}

interface NavigationItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  href: string;
  requiredRole?: string[];
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = !!useAuthStore((s) => s.accessToken);
  const [isMounted, setIsMounted] = useState(false);
  
  useEffect(() => {
    setIsMounted(true);
  }, []);

  const navigationItems: NavigationItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <Home className="h-4 w-4" />,
      href: '/dashboard',
    },
    {
      id: 'users',
      label: 'User Management',
      icon: <Users className="h-4 w-4" />,
      href: '/dashboard/users',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'onboarding',
      label: 'Onboarding',
      icon: <Shield className="h-4 w-4" />,
      href: '/dashboard/users/onboarding',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'departments',
      label: 'Departments',
      icon: <Map className="h-4 w-4" />,
      href: '/departments',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'rooms',
      label: 'Room Management',
      icon: <Map className="h-4 w-4" />,
      href: '/rooms',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'attendance',
      label: 'Attendance',
      icon: <Activity className="h-4 w-4" />,
      href: '/attendance',
      requiredRole: ['ADMIN', 'SUPER_ADMIN', 'FACULTY', 'STUDENT'],
    },
    {
      id: 'timetable',
      label: 'Timetable',
      icon: <Calendar className="h-4 w-4" />,
      href: '/timetable',
      requiredRole: ['ADMIN', 'SUPER_ADMIN', 'FACULTY'],
    },
    {
      id: 'analytics',
      label: 'AI Analytics',
      icon: <Brain className="h-4 w-4" />,
      href: '/analytics',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'student-promotion',
      label: 'Student Promotion',
      icon: <ArrowUp className="h-4 w-4" />,
      href: '/dashboard/users/promotion',
      requiredRole: ['ADMIN', 'SUPER_ADMIN'],
    },
    {
      id: 'attendance-reports',
      label: 'Attendance Reports',
      icon: <FileText className="h-4 w-4" />,
      href: '/dashboard/attendance/reports',
      requiredRole: ['ADMIN', 'SUPER_ADMIN', 'FACULTY'],
    },
    {
      id: 'cr-lr',
      label: 'CR/LR Assignments',
      icon: <Users className="h-4 w-4" />,
      href: '/dashboard/faculty/cr-lr',
      requiredRole: ['FACULTY'],
    },
  ];

  const filteredNavigation = navigationItems.filter(item => {
    if (!item.requiredRole) return true;
    if (!user) return false;
    
    const userRole = user.role.replace('ROLE_', '');
    return item.requiredRole.some(role => role.replace('ROLE_', '') === userRole);
  });

  if (!isMounted || !isAuthenticated || !user) {
    return <div className="min-h-screen bg-[#05050A] text-white flex items-center justify-center">Loading Obsidian System...</div>;
  }

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar — Deep Obsidian Surface */}
      <aside className="w-66 bg-[#0F0F16] border-r border-white/5 h-screen shadow-2xl shadow-black/80 z-20 flex flex-col overflow-y-auto custom-scrollbar">
        <div className="p-6 pb-2">
          <div className="flex items-center gap-3 mb-8">
            <div className="w-8 h-8 rounded-full bg-[#7C3AED] flex items-center justify-center shadow-[0_0_15px_rgba(124,58,237,0.4)]">
              <span className="text-white text-sm font-bold">SA</span>
            </div>
            <span className="font-semibold text-lg text-white tracking-tight">Smart Admin</span>
          </div>
          <h2 className="text-[10px] font-bold text-gray-500 uppercase tracking-[0.2em] mb-4 px-4">
            Navigation
          </h2>
        </div>
        
        <nav className="px-4 pb-6">
          <ul className="space-y-1.5">
            {filteredNavigation.map((item) => (
              <motion.li
                key={item.id}
                whileHover={{ x: 4 }}
                transition={{ duration: 0.2 }}
              >
                <Link
                  href={item.href}
                  className={cn(
                    'flex items-center space-x-3 px-4 py-2.5 rounded-xl transition-all duration-300 w-full group',
                    'text-gray-400 hover:text-white hover:bg-white/5'
                  )}
                >
                  <div className="p-1.5 rounded-lg group-hover:text-violet-400 transition-colors">
                    {item.icon}
                  </div>
                  <span className="font-medium text-sm tracking-wide">{item.label}</span>
                </Link>
              </motion.li>
            ))}
          </ul>
        </nav>

        {/* Status Bar */}
        <div className="mt-auto p-6 border-t border-white/5">
           <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center border border-white/5 text-xs font-bold text-white uppercase italic tracking-tighter">
                {user.name.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-bold text-white truncate uppercase tracking-wider">{user.name}</p>
                <p className="text-[10px] text-violet-500 font-bold truncate tracking-widest">{user.role}</p>
              </div>
           </div>
        </div>
      </aside>

      {/* Main Content — Deep Obsidian Base */}
      <div className="flex-1 flex flex-col overflow-y-auto bg-[#05050A]">
        <Header />
        
        <main className="flex-1 p-0 relative">
          {/* Ambient Glow */}
          <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-violet-600/5 blur-[120px] rounded-full pointer-events-none -z-10" />
          {children}
        </main>
      </div>
      
      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 5px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: rgba(255, 255, 255, 0.02);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(124, 58, 237, 0.3);
        }
      `}</style>
    </div>
  );
}

export default MainLayout;
