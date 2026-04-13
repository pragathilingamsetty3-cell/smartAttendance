'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { Settings as SettingsIcon, User, Bell, Shield, Keyboard, LogOut } from 'lucide-react';
import { AuthProvider } from '@/stores/authContext';
import { MainLayout } from '@/components/layout/MainLayout';
import { useAuth } from '@/stores/authContext';
import { Card } from '@/components/ui/Card';

function SettingsContent() {
  const { user, logout } = useAuth();

  const settingsSections = [
    { title: 'Profile Settings', icon: User, description: 'Manage your personal information and identity.' },
    { title: 'Notifications', icon: Bell, description: 'Configure how you receive system alerts and updates.' },
    { title: 'Security', icon: Shield, description: 'Update your password and biometric preferences.' },
    { title: 'Accessibility', icon: Keyboard, description: 'Customize keyboard shortcuts and display settings.' },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white tracking-tight flex items-center">
          <SettingsIcon className="h-8 w-8 mr-3 text-violet-500" />
          System Settings
        </h1>
        <p className="text-gray-400 mt-2">Manage your account preferences and global system configuration.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {settingsSections.map((section) => (
          <motion.div
            key={section.title}
            whileHover={{ y: -4 }}
            className="cursor-pointer"
          >
            <Card className="p-6 h-full flex flex-col hover:border-violet-500/30 transition-all">
              <div className="flex items-center space-x-4 mb-4">
                <div className="p-3 bg-violet-600/10 rounded-xl text-violet-400">
                  <section.icon className="h-6 w-6" />
                </div>
                <h3 className="text-lg font-semibold text-white">{section.title}</h3>
              </div>
              <p className="text-gray-400 text-sm leading-relaxed flex-1">
                {section.description}
              </p>
            </Card>
          </motion.div>
        ))}
      </div>

      {/* Account Actions */}
      <div className="pt-8 border-t border-[#ffffff0a]">
        <h2 className="text-xl font-bold text-white mb-6">Account Actions</h2>
        <div className="flex flex-wrap gap-4">
          <button 
            onClick={() => logout()}
            className="flex items-center px-6 py-3 bg-[#13131F] text-red-400 rounded-xl border border-red-500/20 hover:bg-red-500/10 transition-colors"
          >
            <LogOut className="h-5 w-5 mr-3" />
            Sign Out of System
          </button>
        </div>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  return (
    <AuthProvider>
      <MainLayout>
        <SettingsContent />
      </MainLayout>
    </AuthProvider>
  );
}
