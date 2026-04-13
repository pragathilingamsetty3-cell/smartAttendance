'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Users, MapPin, Activity, Wifi, WifiOff, AlertTriangle, CheckCircle, XCircle, Timer } from 'lucide-react';
// Assume attendanceService works, or we swap to apiClient. Keeping their service intact for minimal logic breakage.
import { attendanceService } from '@/services/attendance.service';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { AttendanceSession, AttendanceRecord, EnhancedHeartbeatPing } from '@/types/index';
import { SectionReportActions } from './SectionReportActions';

interface LiveAttendanceDashboardProps {
  sessionId: string;
  facultyId: string;
}

interface StudentStatus {
  studentId: string;
  studentName: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  lastHeartbeat?: string;
  location?: { latitude: number; longitude: number; accuracy?: number; };
  deviceStatus: { isOnline: boolean; batteryLevel?: number; isCharging?: boolean; lastSeen: string; };
}

const containerVars = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVars = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0 }
};

export const LiveAttendanceDashboard: React.FC<LiveAttendanceDashboardProps> = ({
  sessionId,
  facultyId
}) => {
  const [loading, setLoading] = useState(true);
  const [session, setSession] = useState<AttendanceSession | null>(null);
  const [studentStatuses, setStudentStatuses] = useState<StudentStatus[]>([]);
  const [realTimeStats, setRealTimeStats] = useState({
    totalStudents: 0, presentCount: 0, absentCount: 0, lateCount: 0, onlineCount: 0, offlineCount: 0, averageBatteryLevel: 0
  });
  const [selectedStudent, setSelectedStudent] = useState<StudentStatus | null>(null);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    fetchSessionData();
    startRealTimeUpdates();
    return () => stopRealTimeUpdates();
  }, [sessionId]);

  const fetchSessionData = async () => {
    try {
      const [sessionData, records] = await Promise.all([
        attendanceService.getSession(sessionId),
        attendanceService.getAttendanceRecords(sessionId)
      ]);
      // @ts-expect-error type override for strict mode
      setSession(sessionData);
      // @ts-expect-error omega clearance
      updateStudentStatuses(records);
    } catch (error) {
      console.error('Failed to fetch session data:', error);
    } finally {
      setLoading(false);
    }
  };

  const startRealTimeUpdates = () => {
    setIsMonitoring(true);
    attendanceService.subscribeToSessionUpdates(sessionId, (data) => {
      // @ts-expect-error omega clearance
      if (data.attendanceRecords) {
        // @ts-expect-error omega clearance
        updateStudentStatuses(data.attendanceRecords);
      }
    });
    intervalRef.current = setInterval(async () => {
      try {
        const records = await attendanceService.getAttendanceRecords(sessionId);
        // @ts-expect-error omega clearance
        updateStudentStatuses(records);
      } catch (error) {}
    }, 5000);
  };

  const stopRealTimeUpdates = () => {
    setIsMonitoring(false);
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    attendanceService.unsubscribeFromSessionUpdates(sessionId);
  };

  const updateStudentStatuses = (records: AttendanceRecord[]) => {
    const statuses: StudentStatus[] = records.map(record => ({
      studentId: record.studentId,
      studentName: `Student ${record.studentId.substring(0, 5)}`,
      status: record.status,
      lastHeartbeat: record.timestamp,
      location: record.location,
      deviceStatus: {
        isOnline: isRecentlyActive(record.timestamp),
        lastSeen: record.timestamp,
        ...record.deviceInfo
      }
    }));
    setStudentStatuses(statuses);
    
    // Stats calc
    const batteries = statuses.map(s => s.deviceStatus.batteryLevel).filter(b => b !== undefined) as number[];
    setRealTimeStats({
      totalStudents: statuses.length,
      presentCount: statuses.filter(s => s.status === 'PRESENT').length,
      absentCount: statuses.filter(s => s.status === 'ABSENT').length,
      lateCount: statuses.filter(s => s.status === 'LATE').length,
      onlineCount: statuses.filter(s => s.deviceStatus.isOnline).length,
      offlineCount: statuses.filter(s => !s.deviceStatus.isOnline).length,
      averageBatteryLevel: batteries.length ? Math.round(batteries.reduce((a, b) => a + b, 0) / batteries.length) : 0
    });
  };

  const isRecentlyActive = (timestamp: string) => {
    return (new Date().getTime() - new Date(timestamp).getTime()) / (1000 * 60) <= 5;
  };

  if (loading) {
    return (
      <div className="space-y-4 py-8">
        <Skeleton className="h-20 w-full" />
        <div className="grid grid-cols-4 gap-4"><Skeleton className="h-32 w-full" /><Skeleton className="h-32 w-full" /><Skeleton className="h-32 w-full" /><Skeleton className="h-32 w-full" /></div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <motion.div variants={containerVars} initial="hidden" animate="show" className="space-y-6">
      <motion.div variants={itemVars} className="glass-panel p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className={`p-2 ${isMonitoring ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-500/20 text-slate-400'} rounded-lg`}>
              <Activity className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Live Attendance System</h3>
              <p className="text-slate-400 text-sm">{session?.roomId || 'Loading'} • {isMonitoring ? 'Monitoring Active' : 'Offline'}</p>
            </div>
          </div>
          
          <div className="flex items-center space-x-3">
            <div className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg border ${
              isMonitoring ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
            }`}>
              {isMonitoring ? <><Wifi size={16} /><span className="text-sm">Live</span></> : <><WifiOff size={16} /><span className="text-sm">Offline</span></>}
            </div>
            <Button variant={isMonitoring ? 'danger' : 'primary'} size="sm" onClick={isMonitoring ? stopRealTimeUpdates : startRealTimeUpdates}>
              {isMonitoring ? 'Stop Watch' : 'Begin Watch'}
            </Button>
          </div>
        </div>
      </motion.div>

      {/* 📊 REPORTING & EXPORTS */}
      {session?.section?.id && (
        <motion.div variants={itemVars}>
          <SectionReportActions sectionId={session.section.id} />
        </motion.div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Headcount</p>
            <Users className="h-5 w-5 text-secondary opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">{realTimeStats.totalStudents}</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden border-emerald-500/20">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Present</p>
            <CheckCircle className="h-5 w-5 text-emerald-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{realTimeStats.presentCount}</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div className="bg-emerald-500 h-1 rounded-full" style={{ width: `${realTimeStats.totalStudents ? (realTimeStats.presentCount / realTimeStats.totalStudents) * 100 : 0}%` }}/>
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Online Sensors</p>
            <Wifi className="h-5 w-5 text-primary opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">{realTimeStats.onlineCount}</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div className="bg-primary h-1 rounded-full" style={{ width: `${realTimeStats.totalStudents ? (realTimeStats.onlineCount / realTimeStats.totalStudents) * 100 : 0}%` }}/>
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Avg Target Battery</p>
            <Activity className="h-5 w-5 text-accent opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">{realTimeStats.averageBatteryLevel}%</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div className="bg-accent h-1 rounded-full" style={{ width: `${realTimeStats.averageBatteryLevel}%` }}/>
          </div>
        </motion.div>
      </div>

      <motion.div variants={itemVars} className="glass-panel p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Identity Stream</h3>
        <div className="space-y-3 max-h-[500px] overflow-y-auto pr-2">
          {studentStatuses.map((student) => (
            <motion.div
              layout
              key={student.studentId}
              onClick={() => setSelectedStudent(student)}
              className="p-4 bg-obsidian-800/80 rounded-xl border border-white/5 hover:border-white/10 hover:bg-white/[0.03] transition-colors cursor-pointer flex justify-between items-center"
            >
              <div className="flex items-center space-x-4">
                <div className={`p-2.5 rounded-xl border ${student.deviceStatus.isOnline ? 'bg-emerald-500/10 border-emerald-500/20' : 'bg-accent/10 border-accent/20'}`}>
                  {student.deviceStatus.isOnline ? <Wifi className="h-5 w-5 text-emerald-400" /> : <WifiOff className="h-5 w-5 text-accent" />}
                </div>
                <div>
                  <p className="text-slate-200 font-medium">{student.studentName}</p>
                  <p className="text-slate-500 text-xs font-mono">{student.studentId.substring(0,8)}</p>
                </div>
              </div>
              <div className="flex space-x-4 items-center">
                
                {student.deviceStatus.batteryLevel != null && (
                  <div className="flex items-center space-x-1.5 opacity-70">
                    <div className={`w-2 h-2 rounded-full ${student.deviceStatus.batteryLevel > 20 ? 'bg-emerald-400' : 'bg-accent animate-pulse'}`} />
                    <span className="text-slate-400 text-xs">{student.deviceStatus.batteryLevel}%</span>
                  </div>
                )}
                
                <span className={`px-2.5 py-1 rounded-lg text-xs font-medium border ${
                   student.status === 'PRESENT' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 
                   student.status === 'ABSENT' ? 'bg-accent/10 text-accent border-accent/20' : 
                   'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
                }`}>
                  {student.status}
                </span>
              </div>
            </motion.div>
          ))}
          {studentStatuses.length === 0 && <p className="text-slate-500 py-8 text-center border border-white/5 border-dashed rounded-xl">No active streams connected.</p>}
        </div>
      </motion.div>
    </motion.div>
  );
};

export default LiveAttendanceDashboard;
