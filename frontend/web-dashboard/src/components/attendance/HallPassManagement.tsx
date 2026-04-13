'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Ticket, Clock, User, CheckCircle, XCircle, AlertTriangle, MapPin, Eye } from 'lucide-react';
import { attendanceService } from '@/services/attendance.service';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { safeParseInt } from '@/utils/numberUtils';
import { HallPassRequestDTO, HallPassStatusDTO, HallPassApprovalRequest, HallPassDenialRequest } from '@/types/index';

interface HallPassManagementProps {
  facultyId: string;
  sessionId: string;
}

const containerVars = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVars = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0 }
};

export const HallPassManagement: React.FC<HallPassManagementProps> = ({ facultyId, sessionId }) => {
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'requests' | 'active' | 'history'>('requests');
  const [hallPassRequests, setHallPassRequests] = useState<HallPassStatusDTO[]>([]);
  const [activePasses, setActivePasses] = useState<HallPassStatusDTO[]>([]);
  const [showRequestForm, setShowRequestForm] = useState(false);
  
  const [requestForm, setRequestForm] = useState<HallPassRequestDTO>({ studentId: '', sessionId: sessionId, requestedMinutes: 15, reason: '' });

  useEffect(() => {
    fetchHallPassData();
    const interval = setInterval(fetchHallPassData, 30000);
    return () => clearInterval(interval);
  }, [sessionId]);

  const fetchHallPassData = async () => {
    try {
      const requests = await attendanceService.getHallPassStatus(sessionId);
      // Wait, getHallPassStatus was returning a single object in legacy mock? Assuming array or single object cast
      const reqArray = Array.isArray(requests) ? requests : [requests];
      setHallPassRequests(reqArray.filter(req => req.status === 'PENDING'));
      setActivePasses(reqArray.filter(req => req.status === 'APPROVED'));
    } catch (error) {}
  };

  const handleRequestHallPass = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await attendanceService.requestHallPass(requestForm);
      setRequestForm({ studentId: '', sessionId: sessionId, requestedMinutes: 15, reason: '' });
      setShowRequestForm(false);
      await fetchHallPassData();
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  const handleApproveHallPass = async (request: HallPassStatusDTO) => {
    try {
      await attendanceService.approveHallPass({ studentId: request.studentId, sessionId: sessionId, approvedMinutes: request.requestedMinutes });
      await fetchHallPassData();
    } catch (error) {}
  };

  const handleDenyHallPass = async (request: HallPassStatusDTO, reason: string) => {
    try {
      await attendanceService.denyHallPass({ studentId: request.studentId, sessionId: sessionId, reason: reason });
      await fetchHallPassData();
    } catch (error) {}
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'DENIED': return 'bg-accent/10 text-accent border-accent/20';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'EXPIRED': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'APPROVED': return <CheckCircle size={16} />;
      case 'DENIED': return <XCircle size={16} />;
      case 'PENDING': return <Clock size={16} />;
      case 'EXPIRED': return <AlertTriangle size={16} />;
      default: return <Clock size={16} />;
    }
  };

  const getTimeRemaining = (processedAt?: string, approvedMinutes?: number) => {
    if (!processedAt || !approvedMinutes) return 'N/A';
    const remaining = new Date(new Date(processedAt).getTime() + approvedMinutes * 60000).getTime() - new Date().getTime();
    if (remaining <= 0) return 'Expired';
    return `${Math.floor(remaining / 60000)}m ${Math.floor((remaining % 60000) / 1000)}s`;
  };

  return (
    <motion.div variants={containerVars} initial="hidden" animate="show" className="space-y-6">
      
      <motion.div variants={itemVars} className="glass-panel p-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-primary/20 rounded-xl neon-border">
              <Ticket className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h3 className="text-xl font-bold text-white tracking-tight">Hall Pass Terminal</h3>
              <p className="text-slate-400 text-sm">Approve and monitor biological movements</p>
            </div>
          </div>
          <Button variant="primary" onClick={() => setShowRequestForm(true)}>
            <Ticket className="h-4 w-4 mr-2" /> Issue Pass
          </Button>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Pending Requests</p>
            <Clock className="h-5 w-5 text-yellow-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">{hallPassRequests.length}</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Active Passes</p>
            <CheckCircle className="h-5 w-5 text-emerald-400 opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white">{activePasses.length}</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Total Absence Time</p>
            <User className="h-5 w-5 text-primary opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">
            {activePasses.reduce((sum, pass) => sum + (pass.requestedMinutes || 0), 0)} min
          </p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex justify-between mb-2">
            <p className="text-slate-400 text-sm font-medium">Currently Away</p>
            <MapPin className="h-5 w-5 text-accent opacity-80" />
          </div>
          <p className="text-3xl font-bold text-white relative z-10">
            {activePasses.filter(pass => {
              const rem = getTimeRemaining(pass.processedAt, pass.requestedMinutes);
              return rem !== 'Expired' && rem !== 'N/A';
            }).length}
          </p>
        </motion.div>
      </div>

      <motion.div variants={itemVars} className="flex space-x-2 p-1.5 bg-obsidian-800/50 rounded-xl border border-white/5 w-fit">
        <button onClick={() => setActiveTab('requests')} className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center ${activeTab === 'requests' ? 'bg-primary text-white shadow-lg' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
          <Clock size={16} className="mr-2" /> Pending ({hallPassRequests.length})
        </button>
        <button onClick={() => setActiveTab('active')} className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center ${activeTab === 'active' ? 'bg-primary text-white shadow-lg' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
          <CheckCircle size={16} className="mr-2" /> Cleared ({activePasses.length})
        </button>
        <button onClick={() => setActiveTab('history')} className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center ${activeTab === 'history' ? 'bg-primary text-white shadow-lg' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
          <Eye size={16} className="mr-2" /> Logs
        </button>
      </motion.div>

      <AnimatePresence>
        {showRequestForm && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="glass-panel w-full max-w-md shadow-2xl">
              <div className="p-6 border-b border-white/10 flex items-center justify-between">
                <h3 className="text-xl font-bold text-white tracking-tight">Manual Hall Pass Grant</h3>
                <button onClick={() => setShowRequestForm(false)} className="text-slate-400 hover:text-white transition">×</button>
              </div>
              <div className="p-6">
                <form onSubmit={handleRequestHallPass} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Student Node ID</label>
                    <input type="text" value={requestForm.studentId} onChange={e => setRequestForm(p => ({...p, studentId: e.target.value}))} className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white outline-none focus:border-primary focus:ring-1 focus:ring-primary backdrop-blur-md" required />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Duration Extent</label>
                    <select 
                      value={requestForm.requestedMinutes} 
                      onChange={(e) => setRequestForm(p => ({...p, requestedMinutes: safeParseInt(e.target.value, 15)}))} 
                      className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white outline-none focus:border-primary focus:ring-1 focus:ring-primary backdrop-blur-md" 
                      required
                    >
                      <option value={5} className="bg-obsidian-900">5 Minutes</option>
                      <option value={10} className="bg-obsidian-900">10 Minutes</option>
                      <option value={15} className="bg-obsidian-900">15 Minutes</option>
                      <option value={30} className="bg-obsidian-900">30 Minutes</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Cause Vector</label>
                    <textarea value={requestForm.reason} onChange={e => setRequestForm(p => ({...p, reason: e.target.value}))} className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white outline-none focus:border-primary focus:ring-1 focus:ring-primary backdrop-blur-md" rows={3} required />
                  </div>
                  <div className="flex justify-end space-x-3 pt-2">
                    <Button type="button" variant="ghost" onClick={() => setShowRequestForm(false)} disabled={loading}>Abort</Button>
                    <Button type="submit" variant="primary" disabled={loading}>{loading ? 'Transmitting...' : 'Dispatch Grant'}</Button>
                  </div>
                </form>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <motion.div variants={itemVars}>
        {activeTab === 'requests' && (
          <div className="space-y-4">
            {hallPassRequests.length === 0 ? (
              <div className="glass-panel py-12 text-center text-slate-400">No requests trapped in queue.</div>
            ) : (hallPassRequests.map(req => (
              <div key={req.requestId} className="glass-card p-5 flex flex-col md:flex-row md:items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3 mb-2">
                    <div className={`p-2 rounded-lg border ${getStatusColor(req.status)}`}>{getStatusIcon(req.status)}</div>
                    <div>
                      <h4 className="text-white font-bold">{req.studentName}</h4>
                      <p className="text-xs text-slate-500 font-mono">{req.studentId}</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
                    <div className="bg-white/5 p-3 rounded-xl border border-white/5">
                      <p className="text-xs text-slate-500 font-bold uppercase mb-1">Time</p>
                      <p className="text-white text-sm">{req.requestedMinutes} Min</p>
                    </div>
                    <div className="bg-white/5 p-3 rounded-xl border border-white/5">
                      <p className="text-xs text-slate-500 font-bold uppercase mb-1">Init</p>
                      <p className="text-white text-sm">{new Date(req.requestedAt).toLocaleTimeString()}</p>
                    </div>
                    <div className="bg-white/5 p-3 rounded-xl border border-white/5 col-span-2">
                      <p className="text-xs text-slate-500 font-bold uppercase mb-1">Reason</p>
                      <p className="text-slate-300 text-sm">{req.reason}</p>
                    </div>
                  </div>
                </div>
                <div className="flex md:flex-col space-x-3 md:space-x-0 md:space-y-2 mt-4 md:mt-0 md:ml-6">
                  <Button variant="primary" size="sm" onClick={() => handleApproveHallPass(req)}>Approve</Button>
                  <Button variant="danger" size="sm" onClick={() => handleDenyHallPass(req, prompt('Enter denial reason:') || 'Denied')}>Reject</Button>
                </div>
              </div>
            )))}
          </div>
        )}

        {activeTab === 'active' && (
          <div className="space-y-4">
            {activePasses.length === 0 ? (
              <div className="glass-panel py-12 text-center text-slate-400">Zero active hall passes. Secure environment.</div>
            ) : (activePasses.map(pass => (
              <div key={pass.requestId} className="glass-card p-5">
                <div className="flex items-center space-x-3 mb-4">
                  <div className={`p-2 rounded-lg border ${getStatusColor(pass.status)}`}>{getStatusIcon(pass.status)}</div>
                  <div>
                    <h4 className="text-white font-bold">{pass.studentName}</h4>
                    <p className="text-xs text-emerald-400 font-medium">Cleared for Transit</p>
                  </div>
                </div>
                <div className="flex space-x-6">
                  <div className="bg-white/5 p-4 rounded-xl border border-white/5 flex-1">
                    <p className="text-xs text-slate-500 font-bold uppercase mb-1">Terminal Countdown</p>
                    <p className="text-2xl font-bold text-white tracking-widest">{getTimeRemaining(pass.processedAt, pass.requestedMinutes)}</p>
                  </div>
                  <div className="flex-1 space-y-2 text-sm text-slate-300 py-2">
                    <div className="flex justify-between"><span className="text-slate-500">Duration Limit</span><span className="text-white font-mono">{pass.requestedMinutes}M</span></div>
                    <div className="flex justify-between"><span className="text-slate-500">Initiated At</span><span className="text-white font-mono">{pass.processedAt ? new Date(pass.processedAt).toLocaleTimeString() : 'N/A'}</span></div>
                  </div>
                </div>
              </div>
            )))}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="glass-panel py-12 text-center text-slate-400 flex flex-col items-center">
            <Eye className="h-10 w-10 text-slate-600 mb-3" />
            No history indexed yet.
          </div>
        )}
      </motion.div>
    </motion.div>
  );
};

export default HallPassManagement;
