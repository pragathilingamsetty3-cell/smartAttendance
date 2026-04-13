'use client';

import React from 'react';
import { motion } from 'framer-motion';
import {
  Activity,
  Shield,
  Users,
  Calendar,
  MapPin,
  Clock,
  ArrowRight,
  TrendingUp,
  AlertCircle
} from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { SectionReportActions } from '../attendance/SectionReportActions';

interface FacultyDashboardViewProps {
  stats: any;
  loading: boolean;
  sectionId?: string;
}

export const FacultyDashboardView: React.FC<FacultyDashboardViewProps> = ({ stats, loading, sectionId }) => {
  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0 }
  };

  const quickActions = [
    {
      title: 'Hall Pass Hub',
      desc: 'Approve or deny student exits',
      icon: <Activity className="text-violet-400" size={20} />,
      href: '/dashboard/faculty/hall-pass',
      color: 'violet',
      count: stats?.pendingHallPasses || 0
    },
    {
      title: 'Exam Security',
      desc: 'Verify identities via barcode',
      icon: <Shield className="text-amber-400" size={20} />,
      href: '/dashboard/faculty/exam-mode',
      color: 'amber',
      count: stats?.activeExams || 0
    },
    {
      title: 'Room Transition',
      desc: 'Quick class relocation',
      icon: <MapPin className="text-emerald-400" size={20} />,
      href: '/dashboard/faculty/room-change',
      color: 'emerald'
    }
  ];

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

        {/* Main Stats Area */}
        <div className="lg:col-span-2 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <motion.div variants={itemVariants} className="glass-card p-6 relative overflow-hidden group">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <TrendingUp size={64} className="text-emerald-500" />
              </div>
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-1">Class Attendance Rate</p>
              <p className="text-4xl font-bold text-white mb-4">{stats?.attendanceRate?.toFixed(1) || '0.0'}%</p>
              <div className="flex items-center text-xs text-emerald-400">
                <span className="bg-emerald-400/10 px-2 py-0.5 rounded-full mr-2">+0.0%</span>
                Live Optimization
              </div>
            </motion.div>

            <motion.div variants={itemVariants} className="glass-card p-6 relative overflow-hidden group">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <Clock size={64} className="text-violet-500" />
              </div>
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-1">Active Sessions</p>
              <p className="text-4xl font-bold text-white mb-4">{stats?.activeSessions || 0}</p>
              <div className="flex items-center text-xs text-violet-400">
                <Link href="/attendance" className="hover:underline flex items-center gap-1">
                  Manage live feed <ArrowRight size={12} />
                </Link>
              </div>
            </motion.div>
          </div>

          <motion.div variants={itemVariants} className="glass-card p-8 bg-gradient-to-br from-violet-600/[0.05] to-transparent border-violet-500/20">
            <div className="flex items-start justify-between mb-8">
              <div>
                <h3 className="text-xl font-bold text-white">
                  {stats?.currentSession ? `Current Session: ${stats.currentSession.courseName}` : 'No Active Session'}
                </h3>
                <p className="text-sm text-slate-400 mt-1 uppercase tracking-widest text-[10px]">
                  {stats?.currentSession ? `${stats.currentSession.roomName}` : 'System Standby'}
                </p>
              </div>
              {stats?.currentSession && (
                <div className="flex items-center gap-2 bg-emerald-500/10 text-emerald-500 px-3 py-1 rounded-full text-[10px] font-bold border border-emerald-500/20">
                  <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" /> LIVE
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
              <div className="text-center md:text-left">
                <p className="text-[10px] font-bold text-slate-500 uppercase mb-1">Present</p>
                <p className="text-2xl font-bold text-white">{stats?.currentSession?.presentCount || 0}</p>
              </div>
              <div className="text-center md:text-left border-l border-white/5 pl-6">
                <p className="text-[10px] font-bold text-slate-500 uppercase mb-1">Total</p>
                <p className="text-2xl font-bold text-slate-500">{stats?.currentSession?.totalStudents || 0}</p>
              </div>
              <div className="text-center md:text-left border-l border-white/5 pl-6">
                <p className="text-[10px] font-bold text-slate-500 uppercase mb-1">Anomalies</p>
                <p className="text-2xl font-bold text-amber-500">{stats?.currentSession?.anomalies || 0}</p>
              </div>
              <div className="text-center md:text-left border-l border-white/5 pl-6">
                <p className="text-[10px] font-bold text-slate-500 uppercase mb-1">Out of Room</p>
                <p className="text-2xl font-bold text-violet-400">{stats?.currentSession?.outOfRoom || 0}</p>
              </div>
            </div>
          </motion.div>
        </div>

        {/* Quick Actions Sidebar */}
        <div className="space-y-6">
          {sectionId && (
            <motion.div variants={itemVariants}>
              <SectionReportActions sectionId={sectionId} />
            </motion.div>
          )}

          <h4 className="text-[10px] font-bold text-slate-500 uppercase tracking-[0.3em] pl-2">Critical Actions</h4>
          <div className="grid grid-cols-1 gap-4">
            {quickActions.map((action) => (
              <Link key={action.title} href={action.href}>
                <motion.div
                  whileHover={{ x: 6 }}
                  className="glass-card p-5 group flex items-center justify-between hover:border-violet-500/30 transition-all border-white/5"
                >
                  <div className="flex items-center gap-4">
                    <div className={`p-3 rounded-2xl bg-${action.color}-500/10 border border-${action.color}-500/20`}>
                      {action.icon}
                    </div>
                    <div>
                      <h5 className="font-bold text-white text-sm tracking-tight">{action.title}</h5>
                      <p className="text-[10px] text-slate-500">{action.desc}</p>
                    </div>
                  </div>
                  {action.count !== undefined && action.count > 0 && (
                    <div className="bg-violet-600 text-white text-[10px] font-black px-2 py-1 rounded-lg shadow-lg shadow-violet-600/30 animate-bounce">
                      {action.count}
                    </div>
                  )}
                  <ArrowRight size={14} className="text-slate-600 group-hover:text-white transition-colors" />
                </motion.div>
              </Link>
            ))}
          </div>

          <Card glass className="p-6 border-amber-500/10 bg-amber-500/[0.01]">
            <div className="flex gap-3">
              <AlertCircle className="text-amber-500 shrink-0" size={18} />
              <div>
                <p className="text-xs font-bold text-amber-500 uppercase tracking-widest mb-1">System Notice</p>
                <p className="text-[10px] text-slate-400 leading-relaxed">Identity verification protocols for Hall 402 updated. Ensure Bluetooth scanners are online.</p>
              </div>
            </div>
          </Card>
        </div>

      </div>
    </div>
  );
};
