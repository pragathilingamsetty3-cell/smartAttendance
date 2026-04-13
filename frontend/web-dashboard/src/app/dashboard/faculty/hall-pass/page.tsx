'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Activity, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  User, 
  History,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { attendanceService } from '@/services/attendance.service';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { HallPassStatusDTO } from '@/types/attendance';

export default function HallPassHub() {
  const [pendingRequests, setPendingRequests] = useState<HallPassStatusDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'pending' | 'history'>('pending');
  const [history, setHistory] = useState<HallPassStatusDTO[]>([]);
  const [processingId, setProcessingId] = useState<string | null>(null);

  const fetchPending = async () => {
    try {
      setLoading(true);
      const data = await attendanceService.getPendingHallPasses() as any;
      setPendingRequests(data.pendingRequests || []);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch pending requests');
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async () => {
    try {
      // Using the seeded test session ID for demonstration
      const sessionId = '00000000-0000-0000-0000-000000000001';
      const data = await attendanceService.getHallPassHistory(sessionId) as any;
      setHistory(data.history || []);
    } catch (err: any) {
      console.error('Failed to fetch history:', err);
    }
  };

  useEffect(() => {
    fetchPending();
    fetchHistory();
    const interval = setInterval(() => {
      fetchPending();
      if (activeTab === 'history') fetchHistory();
    }, 15000); // Poll every 15s
    return () => clearInterval(interval);
  }, [activeTab]);

  const handleApprove = async (studentId: string, sessionId: string) => {
    try {
      setProcessingId(studentId);
      await attendanceService.approveHallPass({
        studentId,
        sessionId,
        approvedMinutes: 10, // Default 10 mins
      });
      await Promise.all([fetchPending(), fetchHistory()]);
    } catch (err: any) {
      alert(err.message || 'Failed to approve');
    } finally {
      setProcessingId(null);
    }
  };

  const handleDeny = async (studentId: string, sessionId: string) => {
    try {
      setProcessingId(studentId);
      await attendanceService.denyHallPass({
        studentId,
        sessionId,
        reason: 'Class in progress / Important topic'
      });
      await Promise.all([fetchPending(), fetchHistory()]);
    } catch (err: any) {
      alert(err.message || 'Failed to deny');
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="p-8 space-y-8 text-white">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight flex items-center gap-3">
            <Activity className="text-violet-500" /> Hall Pass Hub
          </h1>
          <p className="text-slate-400 mt-1">Real-time student exit request management</p>
        </div>
        
        <div className="flex bg-[#0F0F16] p-1 rounded-xl border border-white/5">
          <button
            onClick={() => setActiveTab('pending')}
            className={`px-6 py-2 rounded-lg text-sm font-bold transition-all ${
              activeTab === 'pending' 
                ? 'bg-violet-600 text-white shadow-lg shadow-violet-600/20' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Pending ({pendingRequests.length})
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`px-6 py-2 rounded-lg text-sm font-bold transition-all ${
              activeTab === 'history' 
                ? 'bg-violet-600 text-white shadow-lg shadow-violet-600/20' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            History
          </button>
        </div>
      </div>

      <AnimatePresence mode="wait">
        {loading && pendingRequests.length === 0 && activeTab === 'pending' ? (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col items-center justify-center py-20"
          >
            <Loading size="lg" text="Syncing pending requests..." />
          </motion.div>
        ) : activeTab === 'pending' ? (
          <motion.div
            key="pending"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
          >
            {pendingRequests.length === 0 ? (
              <div className="col-span-full py-20 text-center glass-card border-dashed border-white/10">
                <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-500/50 mb-4" />
                <h3 className="text-xl font-bold text-white">All Clear</h3>
                <p className="text-slate-400">No pending hall pass requests at the moment.</p>
              </div>
            ) : (
              pendingRequests.map((req) => (
                <motion.div 
                   key={req.requestId}
                   layoutId={req.requestId}
                   className="glass-card p-6 relative overflow-hidden group"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-violet-600/10 border border-violet-500/20 flex items-center justify-center text-violet-400">
                        <User size={20} />
                      </div>
                      <div>
                        <h4 className="text-white font-bold">{req.studentName || 'Student'}</h4>
                        <p className="text-[10px] text-slate-400 uppercase tracking-widest">{req.studentId.substring(0, 8) || 'REG-ID'}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 px-2 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-500 text-[10px] font-bold">
                      <Clock size={10} />
                      {req.requestedMinutes}m
                    </div>
                  </div>

                  <div className="space-y-3 mb-6">
                    <div className="flex justify-between text-xs text-slate-400">
                      <span>Request Time</span>
                      <span className="text-white">{new Date(req.requestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                    <div className="text-xs text-slate-400 bg-white/5 p-2 rounded-lg italic">
                      "Reason: {req.reason}"
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <Button 
                      variant="primary" 
                      className="bg-emerald-600 hover:bg-emerald-700 h-10"
                      onClick={() => handleApprove(req.studentId, req.sessionId)}
                      disabled={processingId === req.studentId}
                    >
                      {processingId === req.studentId ? <RefreshCw className="animate-spin" size={16} /> : <CheckCircle2 size={16} className="mr-2" />}
                      Approve
                    </Button>
                    <Button 
                      variant="glass" 
                      className="border-white/10 hover:bg-red-500/10 hover:text-red-500 h-10"
                      onClick={() => handleDeny(req.studentId, req.sessionId)}
                      disabled={processingId === req.studentId}
                    >
                      <XCircle size={16} className="mr-2" />
                      Deny
                    </Button>
                  </div>
                </motion.div>
              ))
            )}
          </motion.div>
        ) : (
          <motion.div
            key="history"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="glass-card overflow-hidden"
          >
             <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-white/5 border-b border-white/5 text-[10px] uppercase tracking-[0.2em] text-slate-400">
                      <th className="px-6 py-4 font-bold">Student</th>
                      <th className="px-6 py-4 font-bold">Reason</th>
                      <th className="px-6 py-4 font-bold">Status</th>
                      <th className="px-6 py-4 font-bold">Duration</th>
                      <th className="px-6 py-4 font-bold">Processed At</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {history.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-12 text-center text-slate-500 uppercase tracking-widest text-[10px]">No historical records found</td>
                      </tr>
                    ) : (
                      history.map((record) => (
                        <tr key={record.requestId} className="hover:bg-white/[0.02] transition-colors">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold">
                                {record.studentName?.substring(0, 2).toUpperCase() || 'JD'}
                              </div>
                              <div>
                                <p className="text-sm font-bold">{record.studentName}</p>
                                <p className="text-[10px] text-slate-500">{record.studentId.substring(0, 8)}</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-xs font-medium text-slate-300 max-w-xs truncate">
                            {record.reason}
                          </td>
                          <td className="px-6 py-4">
                              <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                                record.status === 'APPROVED' 
                                  ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' 
                                  : 'bg-red-500/10 text-red-500 border-red-500/20'
                              }`}>
                                {record.status}
                              </span>
                          </td>
                          <td className="px-6 py-4 text-xs font-mono">{record.requestedMinutes}m</td>
                          <td className="px-6 py-4 text-xs text-slate-500">
                            {record.processedAt ? new Date(record.processedAt).toLocaleTimeString() : 'N/A'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
             </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
