'use client';

import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Activity, 
  Clock, 
  MapPin, 
  MessageSquare, 
  CheckCircle2, 
  ArrowLeft,
  AlertCircle
} from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/authStore';
import apiClient from '@/lib/apiClient';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { StudentDashboardStatsDTO } from '@/types';
import { attendanceService } from '@/services/attendance.service';

export default function StudentHallPassRequest() {
  const router = useRouter();
  const { user } = useAuthStore();
  const [stats, setStats] = useState<StudentDashboardStatsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [requesting, setRequesting] = useState(false);
  const [success, setSuccess] = useState(false);
  
  // Form State
  const [minutes, setMinutes] = useState(15);
  const [reason, setReason] = useState('');

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await apiClient.get<StudentDashboardStatsDTO>('/api/v1/student/dashboard/stats');
        setStats(response.data);
      } catch (err) {
        console.error('Failed to fetch stats:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stats?.activeSession || !user) return;

    try {
      setRequesting(true);
      await attendanceService.requestHallPass({
        studentId: user.id,
        sessionId: stats.activeSession.id,
        requestedMinutes: minutes,
        reason: reason || 'Not specified'
      });
      setSuccess(true);
      setTimeout(() => {
        router.push('/dashboard');
      }, 2000);
    } catch (err) {
      console.error('Request failed:', err);
      alert('Failed to submit request. Please try again.');
    } finally {
      setRequesting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="w-8 h-8 border-4 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (success) {
    return (
      <div className="flex flex-col items-center justify-center h-[80vh] space-y-6 text-center">
        <motion.div 
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="w-24 h-24 bg-emerald-500/10 border-2 border-emerald-500/20 rounded-full flex items-center justify-center text-emerald-500"
        >
          <CheckCircle2 size={48} />
        </motion.div>
        <div>
          <h2 className="text-2xl font-bold text-white mb-2">Request Submitted</h2>
          <p className="text-slate-400">Your faculty has been notified. Redirecting to dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-8">
      {/* Back Button */}
      <Link href="/dashboard" className="inline-flex items-center gap-2 text-slate-400 hover:text-white transition-colors group">
        <ArrowLeft size={18} className="group-hover:-translate-x-1 transition-transform" />
        <span className="font-bold text-sm tracking-tight">Back to Dashboard</span>
      </Link>

      <div className="space-y-2">
        <h1 className="text-3xl font-black text-white tracking-tight flex items-center gap-3">
          <Activity className="text-violet-500" /> Exit Request
        </h1>
        <p className="text-slate-400 font-medium">Digital Hall Pass for active classroom session</p>
      </div>

      {!stats?.activeSession ? (
        <Card glass className="border-amber-500/20 bg-amber-500/5 p-8 rounded-[2rem] text-center">
            <AlertCircle className="mx-auto text-amber-500 mb-4" size={32} />
            <h3 className="text-lg font-bold text-white mb-2">No Active Session</h3>
            <p className="text-slate-400 text-sm max-w-md mx-auto">
                You can only request a hall pass when you are verified in an ongoing class. 
                Please ensure you are within the classroom geofence.
            </p>
            <Button variant="glass" className="mt-6" onClick={() => router.push('/dashboard')}>
                Return to Dashboard
            </Button>
        </Card>
      ) : (
        <motion.form 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            onSubmit={handleSubmit} 
            className="space-y-6"
        >
          {/* Active Session Context */}
          <div className="glass-card p-6 bg-violet-600/[0.03] border-violet-500/20 rounded-[2rem]">
            <p className="text-[10px] font-black text-violet-400 uppercase tracking-[0.2em] mb-3">Currently In</p>
            <div className="flex items-center gap-4">
                <div className="p-3 bg-violet-600/10 rounded-2xl text-violet-400">
                    <Clock size={24} />
                </div>
                <div>
                    <h4 className="text-lg font-bold text-white">{stats.activeSession.subject}</h4>
                    <p className="text-xs text-slate-400">{stats.activeSession.room?.name} • {stats.activeSession.startTime} - {stats.activeSession.endTime}</p>
                </div>
            </div>
          </div>

          <div className="space-y-6 glass-card p-8 rounded-[2.5rem]">
            {/* Duration Selection */}
            <div className="space-y-3">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest pl-1">Requested Duration</label>
                <div className="grid grid-cols-3 gap-4">
                    {[10, 15, 20].map((m) => (
                        <button
                            key={m}
                            type="button"
                            onClick={() => setMinutes(m)}
                            className={`p-4 rounded-2xl font-bold transition-all border ${
                                minutes === m 
                                ? 'bg-violet-600 border-violet-500 text-white shadow-lg shadow-violet-600/20' 
                                : 'bg-white/5 border-white/10 text-slate-400 hover:border-white/20'
                            }`}
                        >
                            {m}m
                        </button>
                    ))}
                </div>
            </div>

            {/* Reason Input */}
            <div className="space-y-3">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest pl-1">Reason for Exit</label>
                <div className="relative">
                    <div className="absolute top-4 left-4 text-slate-500">
                        <MessageSquare size={18} />
                    </div>
                    <textarea
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="e.g., Washroom visit, Water break..."
                        rows={4}
                        className="w-full bg-[#0F0F16] border border-white/10 rounded-3xl p-4 pl-12 text-sm text-white focus:outline-none focus:ring-2 focus:ring-violet-500/50 transition-all placeholder:text-slate-600"
                        required
                    />
                </div>
            </div>

            <Button 
                type="submit" 
                loading={requesting}
                variant="primary" 
                className="w-full h-14 rounded-2xl text-base font-bold shadow-xl shadow-violet-600/30"
            >
                Submit Request
            </Button>
            
            <p className="text-[10px] text-center text-slate-500 leading-relaxed max-w-xs mx-auto">
                By submitting, your faculty will receive a live notification. Exit only after digital approval.
            </p>
          </div>
        </motion.form>
      )}
    </div>
  );
}
