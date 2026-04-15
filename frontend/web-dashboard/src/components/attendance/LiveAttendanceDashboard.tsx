'use client';

import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Users,
  Activity,
  Wifi,
  WifiOff,
  CheckCircle,
  AlertTriangle,
  LogOut,
} from 'lucide-react';
import { attendanceService } from '@/services/attendance.service';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { AttendanceSession, AttendanceRecord } from '@/types/index';
import { SectionReportActions } from './SectionReportActions';
import { useRealtimePresence } from '@/hooks/useRealtimePresence';

interface LiveAttendanceDashboardProps {
  sessionId: string;
  facultyId: string;
}

const containerVars = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } },
};

const itemVars = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0 },
};

export const LiveAttendanceDashboard: React.FC<LiveAttendanceDashboardProps> = ({
  sessionId,
  facultyId,
}) => {
  const [loading, setLoading] = useState(true);
  const [session, setSession] = useState<AttendanceSession | null>(null);
  const [staticRecords, setStaticRecords] = useState<AttendanceRecord[]>([]);
  const [firestoreEnabled, setFirestoreEnabled] = useState(true);

  // ── Real-time Firestore presence (replaces polling interval) ──────────────
  const {
    presenceMap,
    presentCount,
    driftedStudents,
    walkoutStudents,
    isConnected,
    error: presenceError,
  } = useRealtimePresence({ sessionId, enabled: firestoreEnabled });

  useEffect(() => {
    const init = async () => {
      try {
        const [sessionData, records] = await Promise.all([
          attendanceService.getSession(sessionId),
          attendanceService.getAttendanceRecords(sessionId),
        ]);
        // @ts-expect-error type override for strict mode
        setSession(sessionData);
        // @ts-expect-error omega clearance
        setStaticRecords(records ?? []);
      } catch (err) {
        console.error('[LiveAttendanceDashboard] Init error:', err);
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [sessionId]);

  const totalStudents = staticRecords.length;
  const absentCount = Math.max(0, totalStudents - presentCount);

  if (loading) {
    return (
      <div className="space-y-4 py-8">
        <Skeleton className="h-20 w-full" />
        <div className="grid grid-cols-4 gap-4">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <motion.div variants={containerVars} initial="hidden" animate="show" className="space-y-6">

      {/* ── Status Bar ───────────────────────────────────────────────────────── */}
      <motion.div variants={itemVars} className="glass-panel p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className={`p-2 ${isConnected ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-500/20 text-slate-400'} rounded-lg`}>
              <Activity className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Live Attendance System</h3>
              <p className="text-slate-400 text-sm">
                {session?.roomId || 'Loading'} •{' '}
                {isConnected ? (
                  <span className="text-emerald-400">Firestore Live</span>
                ) : (
                  <span className="text-amber-400">Connecting…</span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {presenceError && (
              <span className="text-xs text-red-400 bg-red-500/10 border border-red-500/20 px-3 py-1 rounded-lg">
                ⚠ {presenceError}
              </span>
            )}
            <div className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg border ${
              isConnected
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
            }`}>
              {isConnected ? <><Wifi size={16} /><span className="text-sm">Live</span></> : <><WifiOff size={16} /><span className="text-sm">Offline</span></>}
            </div>
            <Button
              variant={firestoreEnabled ? 'danger' : 'primary'}
              size="sm"
              onClick={() => setFirestoreEnabled((v) => !v)}
            >
              {firestoreEnabled ? 'Pause Watch' : 'Resume Watch'}
            </Button>
          </div>
        </div>
      </motion.div>

      {/* ── Reports ─────────────────────────────────────────────────────────── */}
      {session?.section?.id && (
        <motion.div variants={itemVars}>
          <SectionReportActions sectionId={session.section.id} />
        </motion.div>
      )}

      {/* ── Stats Grid ──────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Headcount</p>
            <Users className="h-5 w-5 text-secondary opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{totalStudents}</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden border-emerald-500/20">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Present</p>
            <CheckCircle className="h-5 w-5 text-emerald-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{presentCount}</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div
              className="bg-emerald-500 h-1 rounded-full transition-all duration-500"
              style={{ width: `${totalStudents ? (presentCount / totalStudents) * 100 : 0}%` }}
            />
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Drifted</p>
            <AlertTriangle className="h-5 w-5 text-amber-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{driftedStudents.length}</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div
              className="bg-amber-500 h-1 rounded-full transition-all duration-500"
              style={{ width: `${totalStudents ? (driftedStudents.length / totalStudents) * 100 : 0}%` }}
            />
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Walkouts</p>
            <LogOut className="h-5 w-5 text-red-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{walkoutStudents.length}</p>
        </motion.div>
      </div>

      {/* ── Live Stream Table ────────────────────────────────────────────────── */}
      <motion.div variants={itemVars} className="glass-panel p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-white">Live Identity Stream</h3>
          <span className="text-xs text-slate-500 font-mono">
            {presenceMap.size} active pings
          </span>
        </div>

        <div className="space-y-3 max-h-[500px] overflow-y-auto pr-2">
          {Array.from(presenceMap.values()).map((p) => (
            <motion.div
              layout
              key={p.studentId}
              className="p-4 bg-obsidian-800/80 rounded-xl border border-white/5 hover:border-white/10 hover:bg-white/[0.03] transition-colors flex justify-between items-center"
            >
              <div className="flex items-center space-x-4">
                <div className={`p-2.5 rounded-xl border ${
                  p.status === 'PRESENT'
                    ? 'bg-emerald-500/10 border-emerald-500/20'
                    : p.status === 'DRIFTED'
                    ? 'bg-amber-500/10 border-amber-500/20'
                    : 'bg-red-500/10 border-red-500/20'
                }`}>
                  {p.status === 'PRESENT' ? (
                    <Wifi className="h-5 w-5 text-emerald-400" />
                  ) : p.status === 'DRIFTED' ? (
                    <AlertTriangle className="h-5 w-5 text-amber-400" />
                  ) : (
                    <WifiOff className="h-5 w-5 text-red-400" />
                  )}
                </div>
                <div>
                  <p className="text-slate-200 font-medium font-mono text-sm">
                    {p.studentId.substring(0, 12)}…
                  </p>
                  <p className="text-slate-500 text-xs">
                    ±{p.accuracy.toFixed(0)}m • {p.timestamp.toLocaleTimeString()}
                  </p>
                </div>
              </div>

              <span className={`px-2.5 py-1 rounded-lg text-xs font-medium border ${
                p.status === 'PRESENT'
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  : p.status === 'DRIFTED'
                  ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                  : 'bg-red-500/10 text-red-400 border-red-500/20'
              }`}>
                {p.status}
              </span>
            </motion.div>
          ))}

          {presenceMap.size === 0 && (
            <p className="text-slate-500 py-8 text-center border border-white/5 border-dashed rounded-xl">
              {isConnected ? 'No active stream pings in the last 5 minutes.' : 'Waiting for Firestore connection…'}
            </p>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
};

export default LiveAttendanceDashboard;
