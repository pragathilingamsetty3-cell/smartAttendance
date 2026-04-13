'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield, BookOpen, User, CheckCircle2, ArrowRight, UserPlus, List } from 'lucide-react';
import { StudentOnboardingForm } from '@/components/forms/StudentOnboardingForm';
import { FacultyOnboardingForm } from '@/components/forms/FacultyOnboardingForm';
import { AdminOnboardingForm } from '@/components/forms/AdminOnboardingForm';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { useAuthStore } from '@/stores/authStore';
import { Role } from '@/types';

type RoleTab = 'STUDENT' | 'FACULTY' | 'ADMIN';

const tabs: { key: RoleTab; label: string; icon: React.ReactNode }[] = [
  { key: 'STUDENT', label: 'Student', icon: <BookOpen className="w-5 h-5 mr-3" /> },
  { key: 'FACULTY', label: 'Faculty', icon: <User className="w-5 h-5 mr-3" /> },
  { key: 'ADMIN',   label: 'Admin',   icon: <Shield className="w-5 h-5 mr-3" /> },
];

export default function OnboardingPage() {
  const [activeTab, setActiveTab] = useState<RoleTab>('STUDENT');
  const [showSuccess, setShowSuccess] = useState(false);
  const [newUserData, setNewUserData] = useState<any>(null);
  const { user } = useAuthStore();
  const router = useRouter();

  // Filter tabs: Regular ADMINs cannot onboard other ADMINs
  const availableTabs = tabs.filter(tab => {
    if (tab.key === 'ADMIN' && user?.role === Role.ADMIN) return false;
    return true;
  });

  const handleSuccess = (response: any) => {
    setNewUserData(response);
    setShowSuccess(true);
  };

  const handleCancel = () => {
    router.push('/dashboard/users');
  };

  return (
    <div className="min-h-screen bg-[#05050A] space-y-8 max-w-5xl mx-auto pb-16 pt-8 px-6">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-semibold text-white tracking-tight">Onboard New User</h1>
        <p className="text-gray-400 mt-2 text-base">Select a role to provision a new identity into the system.</p>
      </div>

      {/* Role Tabs — Forced Hex-Code Obsidian */}
      <div className="flex space-x-2 bg-[#0F0F16] p-1.5 rounded-xl border border-[#ffffff0a] max-w-md">
        {availableTabs.map(tab => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`relative flex-1 flex items-center justify-center py-3 px-4 rounded-lg font-medium text-sm transition-all duration-300 border ${
                isActive
                  ? 'bg-[#7C3AED] text-white shadow-[0_0_20px_rgba(124,58,237,0.4)] border-[#8B5CF680]'
                  : 'text-[#9ca3af] hover:text-white hover:bg-[#ffffff0a] border-transparent'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Form Panel */}
      <AnimatePresence mode="wait">
        {!showSuccess ? (
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
          >
            {activeTab === 'STUDENT' && <StudentOnboardingForm onSuccess={handleSuccess} onCancel={handleCancel} />}
            {activeTab === 'FACULTY' && <FacultyOnboardingForm onSuccess={handleSuccess} onCancel={handleCancel} />}
            {activeTab === 'ADMIN' && <AdminOnboardingForm onSuccess={handleSuccess} onCancel={handleCancel} />}
          </motion.div>
        ) : (
          <motion.div
            key="success"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex flex-col items-center justify-center py-20 px-6 text-center glass-panel max-w-2xl mx-auto shadow-2xl relative overflow-hidden"
          >
            {/* Animated Background Glow */}
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-emerald-500/20 blur-[120px] rounded-full pointer-events-none" />
            
            <motion.div 
               animate={{ y: [0, -10, 0] }}
               transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
               className="h-24 w-24 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-8 relative z-10"
            >
               <CheckCircle2 size={48} />
            </motion.div>

            <h2 className="text-3xl font-bold text-white mb-2 relative z-10">Onboarding Successful</h2>
            <p className="text-slate-400 mb-10 max-w-md relative z-10">
               {newUserData?.name || 'User'} has been correctly provisioned and their identity hash is now live in the system.
            </p>

            <div className="flex flex-col sm:flex-row items-center gap-4 relative z-10">
               <Button 
                 variant="primary" 
                 className="gap-2 shadow-lg shadow-primary/20 w-full sm:w-auto"
                 onClick={() => router.push(`/dashboard/users/${newUserData?.userId || newUserData?.id}`)}
               >
                  View Profile <ArrowRight size={18} />
               </Button>
               <Button 
                 variant="secondary" 
                 className="gap-2 border-white/5 w-full sm:w-auto"
                 onClick={() => {
                   setShowSuccess(false);
                   setNewUserData(null);
                 }}
               >
                  <UserPlus size={18} /> Onboard Another
               </Button>
               <Button 
                 variant="glass" 
                 className="gap-2 w-full sm:w-auto"
                 onClick={() => router.push('/dashboard/users')}
               >
                  <List size={18} /> Back to Users
               </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
