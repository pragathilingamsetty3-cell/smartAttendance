'use client';

import React from 'react';
import { motion } from 'framer-motion';
import {
  Activity,
  Shield,
  BookOpen,
  Calendar,
  MapPin,
  Clock,
  ArrowRight,
  TrendingUp,
  AlertCircle,
  CheckCircle2,
  XCircle,
  Fingerprint
} from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { StudentDashboardStatsDTO, EnhancedUserDTO } from '@/types';

interface StudentDashboardViewProps {
  stats: StudentDashboardStatsDTO | null;
  loading: boolean;
  user: EnhancedUserDTO | null;
}

export const StudentDashboardView: React.FC<StudentDashboardViewProps> = ({ stats, loading, user }) => {
  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0 }
  };

  const quickActions = [
    {
      title: 'Biometric Setup',
      desc: 'Register fingerprint & device',
      icon: <Shield className="text-primary" size={20} />,
      href: '/setup',
      color: 'primary'
    },
    {
      title: 'Exit Request',
      desc: 'Digital Hall Pass request',
      icon: <Activity className="text-violet-400" size={20} />,
      href: '/dashboard/student/hall-pass/request',
      color: 'violet'
    },
    {
      title: 'My Schedule',
      desc: 'Full semester timetable',
      icon: <Calendar className="text-amber-400" size={20} />,
      href: '/timetable',
      color: 'amber'
    },
    {
      title: 'Attendance Log',
      desc: 'Session-wise analytics',
      icon: <BookOpen className="text-emerald-400" size={20} />,
      href: '/attendance',
      color: 'emerald'
    }
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  const needsSetup = !user?.deviceId || user.deviceId === 'UNBOUND_DEVICE';

  return (
    <div className="space-y-8">
      {/* Security Alert if setup needed */}
      {needsSetup && (
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-6 rounded-3xl bg-red-500/10 border border-red-500/20 flex flex-col md:flex-row items-center justify-between gap-4"
        >
          <div className="flex gap-4 items-center">
            <div className="p-3 bg-red-500/20 rounded-2xl text-red-500">
              <Fingerprint size={24} />
            </div>
            <div>
              <p className="text-white font-bold">Biometric Setup Required</p>
              <p className="text-slate-400 text-xs mt-1">
                Your account is currently not bound to any device. You must register your device and fingerprint to mark attendance.
              </p>
            </div>
          </div>
          <Link href="/setup">
            <Button className="bg-red-500 hover:bg-red-600 text-white font-bold rounded-xl px-6 py-2">
              Complete Setup Now
            </Button>
          </Link>
        </motion.div>
      )}

      {/* Header Info */}
      <motion.div 
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        className="flex flex-col md:flex-row md:items-end justify-between gap-4"
      >
        <div>
          <h2 className="text-3xl font-bold text-white tracking-tight">Student Dashboard</h2>
          <p className="text-slate-400 text-sm mt-1 uppercase tracking-widest font-medium">
            {stats?.departmentName} • Section {stats?.sectionName} • Semester {stats?.semester}
          </p>
        </div>
        <div className="bg-white/5 border border-white/10 px-4 py-2 rounded-2xl">
          <p className="text-[10px] font-bold text-slate-500 uppercase mb-0.5 tracking-tighter">Reg Number</p>
          <p className="text-sm font-mono text-violet-400 font-bold">{stats?.registrationNumber}</p>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Content (Left) */}
        <div className="lg:col-span-2 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Overall Attendance Card */}
            <motion.div variants={itemVariants} className="glass-card p-6 relative overflow-hidden group">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <TrendingUp size={64} className="text-emerald-500" />
              </div>
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-1">My Attendance Rate</p>
              <div className="flex items-end gap-3 mb-4">
                <p className="text-5xl font-black text-white tracking-tighter">{stats?.overallAttendance?.toFixed(1) || '0.0'}%</p>
                <div className="flex flex-col mb-1">
                    <span className="text-[10px] font-bold text-emerald-400 uppercase tracking-widest">Good Standing</span>
                    <span className="text-[10px] text-slate-500 uppercase tracking-widest">Required: 75%</span>
                </div>
              </div>
              <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
                <motion.div 
                    initial={{ width: 0 }}
                    animate={{ width: `${stats?.overallAttendance || 0}%` }}
                    className="h-full bg-gradient-to-r from-emerald-500 to-emerald-400"
                />
              </div>
            </motion.div>

            {/* Session Info Card */}
            <motion.div variants={itemVariants} className="glass-card p-6 relative overflow-hidden group border-violet-500/10">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity whitespace-nowrap overflow-hidden">
                <Clock size={64} className="text-violet-500" />
              </div>
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-1">Classes This Month</p>
              <p className="text-5xl font-black text-white tracking-tighter mb-4">
                {stats?.attendedClasses || 0}<span className="text-slate-600 text-2xl font-light">/{stats?.totalClasses || 0}</span>
              </p>
              <div className="flex items-center text-xs text-violet-400">
                <Link href="/attendance" className="hover:underline flex items-center gap-1 font-bold">
                  View full history <ArrowRight size={12} />
                </Link>
              </div>
            </motion.div>
          </div>

          {/* Current Active Session */}
          <motion.div variants={itemVariants} className={`glass-card p-8 bg-gradient-to-br from-violet-600/[0.05] to-transparent border-violet-500/20 ${!stats?.activeSession && 'opacity-60'}`}>
            <div className="flex items-start justify-between mb-8">
              <div className="flex gap-4">
                <div className={`p-4 rounded-3xl ${stats?.activeSession ? 'bg-violet-600 shadow-xl shadow-violet-600/20 text-white' : 'bg-slate-800 text-slate-500'}`}>
                    <BookOpen size={24} />
                </div>
                <div>
                  <h3 className="text-xl font-black text-white tracking-tight">
                    {stats?.activeSession ? stats.activeSession.subject : 'No Session Active'}
                  </h3>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1">
                    <p className="flex items-center gap-1.5 text-xs text-slate-400 uppercase tracking-widest">
                        <MapPin size={12} className="text-violet-500" /> {stats?.activeSession?.room?.name || '---'}
                    </p>
                    <p className="flex items-center gap-1.5 text-xs text-slate-400 uppercase tracking-widest">
                        <Clock size={12} className="text-violet-500" /> {stats?.activeSession ? `${stats.activeSession.startTime} - ${stats.activeSession.endTime}` : '--:--'}
                    </p>
                  </div>
                </div>
              </div>
              {stats?.activeSession && (
                <div className="flex items-center gap-2 bg-emerald-500/10 text-emerald-500 px-3 py-1 rounded-2xl text-[10px] font-black border border-emerald-500/20 tracking-widest">
                  <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" /> LIVE TRACKING
                </div>
              )}
            </div>

            <div className="pt-2">
                {stats?.activeSession ? (
                    <div className="flex flex-col sm:flex-row items-center justify-between gap-6 p-6 rounded-3xl bg-white/[0.03] border border-white/5">
                        <div className="flex gap-4 items-center">
                            <div className="p-3 bg-emerald-500/20 rounded-2xl text-emerald-500">
                                <CheckCircle2 size={24} />
                            </div>
                            <div>
                                <p className="text-xs font-bold text-white tracking-tight">Attendance Verified</p>
                                <p className="text-[10px] text-slate-500 uppercase tracking-widest">Via Mobile Sensor Sync</p>
                            </div>
                        </div>
                        <Button variant="glass" size="sm" className="rounded-xl border-violet-500/30 hover:bg-violet-500/10 text-violet-400 font-bold tracking-tight">
                            Request Exit
                        </Button>
                    </div>
                ) : (
                    <div className="text-center py-6">
                        <p className="text-xs text-slate-500 font-bold uppercase tracking-[0.2em]">Automatic attendance system standby</p>
                    </div>
                )}
            </div>
          </motion.div>

          {/* Today's Schedule */}
          <motion.div variants={itemVariants} className="space-y-6">
            <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] pl-2">Today's Timeline</h4>
            <div className="space-y-4">
              {stats?.todayClasses && stats.todayClasses.length > 0 ? (
                stats.todayClasses.map((cls, idx) => (
                  <div key={cls.id || idx} className="flex gap-4 group">
                    <div className="flex flex-col items-center">
                        <div className={`w-3 h-3 rounded-full mt-1.5 border-2 ${stats?.activeSession?.id === cls.id ? 'bg-violet-500 border-violet-400 shadow-lg shadow-violet-500/40' : 'bg-transparent border-slate-700 group-hover:border-slate-500'}`} />
                        {idx !== stats.todayClasses.length - 1 && <div className="w-0.5 grow bg-slate-800 my-1 group-hover:bg-slate-700 transition-colors" />}
                    </div>
                    <div className={`grow glass-card p-4 flex items-center justify-between transition-all group-hover:bg-white/[0.03] ${stats?.activeSession?.id === cls.id ? 'border-violet-500/30 bg-violet-500/[0.02]' : 'border-transparent'}`}>
                        <div className="flex gap-4">
                            <div className="min-w-[60px]">
                                <p className="text-[10px] font-black text-white tracking-tighter">{cls.startTime}</p>
                                <p className="text-[10px] text-slate-500 font-medium">{cls.endTime}</p>
                            </div>
                            <div>
                                <p className={`text-sm font-bold tracking-tight ${stats?.activeSession?.id === cls.id ? 'text-violet-400' : 'text-white'}`}>{cls.subject}</p>
                                <p className="text-[10px] text-slate-500 uppercase tracking-widest">{cls.room?.name} • {cls.faculty?.name}</p>
                            </div>
                        </div>
                        {stats?.activeSession?.id === cls.id ? (
                             <Activity size={16} className="text-violet-500 animate-pulse" />
                        ) : (
                            <Clock size={16} className="text-slate-700 group-hover:text-slate-500" />
                        )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-12 glass-card border-dashed border-slate-800">
                    <p className="text-sm text-slate-600 font-bold uppercase tracking-widest">No classes scheduled for today</p>
                </div>
              )}
            </div>
          </motion.div>
        </div>

        {/* Sidebar (Right) */}
        <div className="space-y-8">
          {/* AI Verification Strength (NEW) */}
          <motion.div 
            variants={itemVariants}
            className="p-6 rounded-[2rem] border border-violet-500/20 bg-violet-500/[0.03] overflow-hidden relative group"
          >
            <div className="absolute top-0 right-0 p-4 opacity-5 group-hover:opacity-10 transition-opacity">
                <Shield size={48} className="text-violet-500" />
            </div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] mb-4 text-violet-400">AI Verification Strength</p>
            
            <div className="flex items-center gap-4 mb-4">
                <div className="relative w-16 h-16 flex items-center justify-center">
                    <svg className="w-full h-full transform -rotate-90">
                        <circle
                            cx="32"
                            cy="32"
                            r="28"
                            stroke="currentColor"
                            strokeWidth="4"
                            fill="transparent"
                            className="text-white/5"
                        />
                        <motion.circle
                            initial={{ strokeDashoffset: 175 }}
                            animate={{ strokeDashoffset: 175 - (175 * (stats?.aiVerificationConfidence || 0.5)) }}
                            cx="32"
                            cy="32"
                            r="28"
                            stroke="currentColor"
                            strokeWidth="4"
                            strokeDasharray="175"
                            fill="transparent"
                            className="text-violet-500"
                        />
                    </svg>
                    <span className="absolute text-xs font-black text-white">
                        {Math.round((stats?.aiVerificationConfidence || 0.5) * 100)}%
                    </span>
                </div>
                <div>
                    <h5 className="text-sm font-black text-white tracking-tight">Pattern Confidence</h5>
                    <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">ML Sync active</p>
                </div>
            </div>
            
            <p className="text-[10px] text-slate-400 leading-relaxed italic">
                AI has learned your behavioral patterns to ensure high-accuracy automated attendance marking.
            </p>
          </motion.div>

          {/* Quick Actions */}
          <div className="space-y-4">
            <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] pl-2 text-center md:text-left">Quick Actions</h4>
            <div className="grid grid-cols-1 gap-4">
                {quickActions.map((action) => (
                <Link key={action.title} href={action.href}>
                    <motion.div
                    whileHover={{ x: 6 }}
                    className="glass-card p-5 group flex items-center justify-between hover:border-violet-500/30 transition-all border-white/5"
                    >
                    <div className="flex items-center gap-4">
                        <div className={`p-3 rounded-2xl bg-${action.color}-500/10 border border-${action.color}-500/20 text-${action.color}-400 group-hover:scale-110 transition-transform`}>
                        {action.icon}
                        </div>
                        <div>
                        <h5 className="font-bold text-white text-sm tracking-tight">{action.title}</h5>
                        <p className="text-[10px] text-slate-500 italic">{action.desc}</p>
                        </div>
                    </div>
                    <ArrowRight size={14} className="text-slate-700 group-hover:text-white transition-colors" />
                    </motion.div>
                </Link>
                ))}
            </div>
          </div>

          {/* Hall Pass Status */}
          {stats?.recentHallPass ? (
            <motion.div 
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className={`p-6 rounded-[2rem] border overflow-hidden relative ${
                    stats.recentHallPass.status === 'PENDING' ? 'bg-amber-500/5 border-amber-500/20' : 
                    stats.recentHallPass.status === 'APPROVED' ? 'bg-emerald-500/5 border-emerald-500/20' : 
                    'bg-rose-500/5 border-rose-500/20'
                }`}
            >
                <div className="absolute top-0 right-0 p-4 opacity-10">
                    <Shield size={48} className={
                         stats.recentHallPass.status === 'PENDING' ? 'text-amber-500' : 
                         stats.recentHallPass.status === 'APPROVED' ? 'text-emerald-500' : 
                         'text-rose-500'
                    } />
                </div>
                <p className="text-[10px] font-black uppercase tracking-[0.2em] mb-4 text-slate-500">Hall Pass Status</p>
                <div className="flex items-center gap-3 mb-3">
                    <div className={`w-2 h-2 rounded-full animate-pulse ${
                         stats.recentHallPass.status === 'PENDING' ? 'bg-amber-500' : 
                         stats.recentHallPass.status === 'APPROVED' ? 'bg-emerald-500' : 
                         'bg-rose-500'
                    }`} />
                    <p className={`font-black text-sm tracking-tight ${
                         stats.recentHallPass.status === 'PENDING' ? 'text-amber-500' : 
                         stats.recentHallPass.status === 'APPROVED' ? 'text-emerald-500' : 
                         'text-rose-500'
                    }`}>{stats.recentHallPass.status}</p>
                </div>
                <p className="text-xs text-white font-bold leading-tight mb-2">{stats.recentHallPass.reason}</p>
                <p className="text-[10px] text-slate-500 font-medium">Requested {stats.recentHallPass.requestedMinutes} mins</p>
                
                {stats.recentHallPass.facultyNotes && (
                    <div className="mt-4 p-3 rounded-2xl bg-white/[0.03] border border-white/5">
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">Faculty Note</p>
                        <p className="text-[10px] text-slate-300 italic">"{stats.recentHallPass.facultyNotes}"</p>
                    </div>
                )}
            </motion.div>
          ) : (
            <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="p-6 rounded-[2rem] border border-white/5 bg-white/[0.02] flex flex-col items-center text-center py-10"
            >
                <div className="p-4 bg-slate-800/50 rounded-2xl text-slate-600 mb-4">
                    <Activity size={32} />
                </div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">No Active Pass</p>
                <p className="text-[10px] text-slate-600 max-w-[150px] mx-auto leading-relaxed italic">
                    {stats?.activeSession 
                        ? "Request an exit pass to leave the classroom geofence." 
                        : "Digital Hall Pass is disabled when no class is in session."}
                </p>
            </motion.div>
          )}

          {/* System Notice */}
          <Card glass className="p-6 border-violet-500/10 bg-violet-500/[0.01] rounded-[2rem]">
            <div className="flex gap-4">
              <div className="p-3 bg-violet-600/10 rounded-2xl text-violet-500 shrink-0 h-fit">
                <AlertCircle size={20} />
              </div>
              <div>
                <p className="text-xs font-black text-violet-400 uppercase tracking-widest mb-1">Live Sync Active</p>
                <p className="text-[10px] text-slate-500 leading-relaxed font-medium">Your mobile device is successfully paired with the classroom sensor network. Attendance is being tracked automatically.</p>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};
