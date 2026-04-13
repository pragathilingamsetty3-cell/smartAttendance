"use client";

import React from "react";
import { motion } from "framer-motion";
import { 
  Settings as SettingsIcon, 
  Shield, 
  User, 
  Mail, 
  MapPin, 
  Award,
  BookOpen,
  ArrowRight
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { ChangePasswordForm } from "@/components/forms/ChangePasswordForm";

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div className="space-y-8 max-w-6xl mx-auto px-6 py-8">
      {/* Header with Ambient Glow */}
      <div className="relative">
        <div className="flex items-center gap-4 relative z-10">
          <div className="p-3 rounded-2xl bg-primary/10 border border-primary/20 text-primary shadow-[0_0_15px_rgba(124,58,237,0.2)]">
            <SettingsIcon size={24} />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white tracking-tight">System Settings</h1>
            <p className="text-slate-400 mt-1">Manage your identity credentials and personal configuration.</p>
          </div>
        </div>
        <div className="absolute -top-10 -left-10 w-40 h-40 bg-primary/5 blur-[80px] rounded-full pointer-events-none" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Profile Overview */}
        <div className="lg:col-span-4 space-y-6">
          <motion.div 
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="glass-panel overflow-hidden border border-white/5 shadow-2xl relative"
          >
             {/* Gradient Accent */}
             <div className="h-24 bg-gradient-to-br from-primary/30 via-violet-600/20 to-[#0F0F16]" />
             
             <div className="px-6 pb-8 -mt-12">
               <div className="relative inline-block">
                 <div className="h-24 w-24 rounded-2xl bg-[#13131F] border-4 border-[#05050A] flex items-center justify-center text-primary text-3xl font-bold font-mono shadow-xl">
                   {user?.name?.charAt(0)}
                 </div>
                 <div className="absolute -bottom-1 -right-1 h-6 w-6 rounded-full bg-emerald-500 border-4 border-[#05050A] flex items-center justify-center">
                    <CheckCircle2 size={10} className="text-[#05050A]" />
                 </div>
               </div>

               <div className="mt-4">
                 <h2 className="text-xl font-bold text-white tracking-tight">{user?.name}</h2>
                 <p className="text-slate-500 text-sm mb-6 flex items-center gap-2">
                    <Mail size={14} /> {user?.email}
                 </p>
               </div>

               <div className="space-y-4">
                 <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.03] border border-white/5">
                    <div className="flex items-center gap-3">
                       <Award size={16} className="text-violet-400" />
                       <span className="text-xs text-slate-400 font-medium">Role</span>
                    </div>
                    <span className="text-xs font-bold text-white uppercase tracking-widest">{user?.role}</span>
                 </div>

                 <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.03] border border-white/5 shadow-inner">
                    <div className="flex items-center gap-3">
                       <MapPin size={16} className="text-violet-400" />
                       <span className="text-xs text-slate-400 font-medium">Access Pool</span>
                    </div>
                    <span className="text-xs font-bold text-white uppercase tracking-widest">AUTHORIZED</span>
                 </div>
               </div>
             </div>
          </motion.div>

          <div className="p-6 rounded-2xl bg-primary/5 border border-primary/10 overflow-hidden relative group cursor-pointer transition-all hover:bg-primary/10">
             <div className="absolute -right-4 -bottom-4 opacity-5 group-hover:scale-110 transition-transform">
                <BookOpen size={120} />
             </div>
             <h4 className="text-sm font-bold text-white mb-2 flex items-center gap-2">
                Knowledge Base <ArrowRight size={14} className="group-hover:translate-x-1 transition-transform" />
             </h4>
             <p className="text-xs text-slate-500 leading-relaxed max-w-[80%]">Explore the system documentation for advanced identity management protocols.</p>
          </div>
        </div>

        {/* Right Column: Security Controls */}
        <div className="lg:col-span-8 flex flex-col gap-8">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="glass-panel p-8 shadow-2xl relative overflow-hidden"
          >
             {/* Security Background Pattern */}
             <div className="absolute top-0 right-0 p-8 opacity-[0.03] pointer-events-none scale-150 transform translate-x-10 -translate-y-10">
                <Shield size={160} />
             </div>
             
             <div className="relative z-10 max-w-2xl">
                <ChangePasswordForm />
             </div>
          </motion.div>

          {/* Device Management Stub */}
          <motion.div 
             initial={{ opacity: 0, y: 20 }}
             animate={{ opacity: 1, y: 0 }}
             transition={{ delay: 0.2 }}
             className="glass-panel p-8 opacity-50 cursor-not-allowed group border-dashed"
          >
             <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-bold text-white mb-1">Multi-Factor Authentication</h3>
                  <p className="text-slate-500 text-sm">Enhanced biometric verification for administrative actions.</p>
                </div>
                <span className="px-3 py-1 rounded-full bg-slate-800 text-slate-500 text-[10px] font-black tracking-widest">STUB_V2</span>
             </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

// Sub-component Helper
function CheckCircle2({ size, className }: { size: number, className: string }) {
  return (
    <svg 
      width={size} 
      height={size} 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="3" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
      className={className}
    >
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
      <polyline points="22 4 12 14.01 9 11.01" />
    </svg>
  );
}
