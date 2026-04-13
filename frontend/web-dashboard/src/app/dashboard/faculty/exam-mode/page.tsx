'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Shield, 
  Scan, 
  Users, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle,
  ArrowRight,
  Fingerprint,
  CalendarDays,
  MapPin,
  Clock
} from 'lucide-react';
import { examService, ExamBarcodeScanRequest } from '@/services/exam.service';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';

export default function ExamMode() {
  const [sessions, setSessions] = useState<any[]>([]);
  const [selectedSession, setSelectedSession] = useState<any | null>(null);
  const [barcode, setBarcode] = useState('');
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const [lastScanResult, setLastScanResult] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchExamSessions = async () => {
      try {
        setLoading(true);
        const data = await examService.getTodaySessions() as any;
        setSessions(data.sessions || []);
      } catch (err: any) {
        setError(err.message || 'Failed to load exam sessions');
      } finally {
        setLoading(false);
      }
    };
    fetchExamSessions();
  }, []);

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!barcode.trim() || !selectedSession) return;

    try {
      setScanning(true);
      const result = await examService.scanBarcode({
        barcode: barcode.trim(),
        sessionId: selectedSession.sessionId,
        deviceId: 'BROWSER-FACULTY-' + selectedSession.sessionId
      });
      setLastScanResult(result);
      setBarcode('');
      
      // Update session counts locally
      if (selectedSession) {
        setSelectedSession({
          ...selectedSession,
          scannedStudents: (selectedSession.scannedStudents || 0) + 1
        });
      }
    } catch (err: any) {
      setLastScanResult({ error: err.message || 'Scan failed' });
    } finally {
      setScanning(false);
    }
  };

  if (loading) return <div className="p-20 text-center"><Loading size="lg" text="Securely loading exam schedules..." /></div>;

  return (
    <div className="p-8 space-y-8 max-w-6xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight flex items-center gap-3">
            <Shield className="text-amber-500" /> Exam Security Mode
          </h1>
          <p className="text-slate-400 mt-1 uppercase text-[10px] font-bold tracking-[0.2em]">Zero-Trust Identity Verification Interface</p>
        </div>
        
        {selectedSession && (
          <Button 
            variant="glass" 
            className="border-white/10 text-slate-400"
            onClick={() => { setSelectedSession(null); setLastScanResult(null); }}
          >
            Switch Session
          </Button>
        )}
      </div>

      <AnimatePresence mode="wait">
        {!selectedSession ? (
          <motion.div
            key="session-selector"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
          >
            {sessions.length === 0 ? (
              <div className="col-span-full py-20 text-center glass-card border-dashed">
                <CalendarDays className="mx-auto h-12 w-12 text-slate-700 mb-4" />
                <h3 className="text-xl font-bold text-slate-500">No Exams Scheduled Today</h3>
              </div>
            ) : (
              sessions.map(session => (
                <Card 
                  key={session.sessionId} 
                  glass 
                  className="hover:border-amber-500/30 transition-all cursor-pointer group"
                  onClick={() => setSelectedSession(session)}
                >
                  <CardContent className="p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div className="bg-amber-500/10 p-2 rounded-lg text-amber-500">
                        <Fingerprint size={24} />
                      </div>
                      <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${session.isActive ? 'bg-emerald-500/10 text-emerald-500' : 'bg-slate-500/10 text-slate-400'}`}>
                        {session.isActive ? 'ACTIVE' : 'UPCOMING'}
                      </span>
                    </div>
                    <h3 className="text-lg font-bold text-white mb-1">{session.subject}</h3>
                    <p className="text-xs text-slate-400 mb-4">{session.sessionId}</p>
                    
                    <div className="grid grid-cols-2 gap-4 mb-6">
                      <div className="flex items-center gap-2 text-slate-400">
                         <MapPin size={14} className="text-amber-500/50" />
                         <span className="text-[10px] font-bold uppercase tracking-wider">{session.room || 'TBD'}</span>
                      </div>
                      <div className="flex items-center gap-2 text-slate-400">
                         <Clock size={14} className="text-amber-500/50" />
                         <span className="text-[10px] font-bold uppercase tracking-wider">{session.startTime || 'TBD'}</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-4 border-t border-white/5">
                       <span className="text-xs text-slate-500">Identity Scan Readiness</span>
                       <ArrowRight size={16} className="text-amber-500 group-hover:translate-x-1 transition-transform" />
                    </div>
                  </CardContent>
                </Card>
              ))
            )}
          </motion.div>
        ) : (
          <motion.div
            key="scanner-ui"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="grid grid-cols-1 lg:grid-cols-3 gap-8"
          >
            {/* Left: Verification Stats */}
            <div className="space-y-6">
                <Card glass className="p-6 border-amber-500/20 bg-amber-500/[0.02]">
                    <h3 className="text-sm font-bold text-white uppercase tracking-widest mb-6">Security Stats</h3>
                    <div className="space-y-6">
                        <div>
                            <div className="flex justify-between items-end mb-2">
                                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Verification Progress</span>
                                <span className="text-lg font-bold text-white">{selectedSession.scannedStudents || 0} / {selectedSession.totalStudents || 1}</span>
                            </div>
                            <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden border border-white/5">
                                <motion.div 
                                    className="h-full bg-gradient-to-r from-amber-600 to-amber-400"
                                    initial={{ width: 0 }}
                                    animate={{ width: `${((selectedSession.scannedStudents || 0) / (selectedSession.totalStudents || 1)) * 100}%` }}
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="p-3 bg-white/5 rounded-xl border border-white/5">
                                <p className="text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">Expected</p>
                                <p className="text-xl font-bold text-white">{selectedSession.totalStudents || 0}</p>
                            </div>
                            <div className="p-3 bg-white/5 rounded-xl border border-white/5">
                                <p className="text-[9px] font-bold text-slate-500 uppercase tracking-wider mb-1">Authenticated</p>
                                <p className="text-xl font-bold text-emerald-500">{selectedSession.scannedStudents || 0}</p>
                            </div>
                        </div>
                    </div>
                </Card>

                <Card glass className="p-6">
                    <h3 className="text-sm font-bold text-white uppercase tracking-widest mb-4">Current Session</h3>
                    <div className="space-y-2 text-xs">
                        <div className="flex justify-between font-medium"><span className="text-slate-500">Subject:</span> <span className="text-white">{selectedSession.subject}</span></div>
                        <div className="flex justify-between font-medium"><span className="text-slate-500">Examiner:</span> <span className="text-white">FACULTY ASSIGNED</span></div>
                        <div className="flex justify-between font-medium"><span className="text-slate-500">Location:</span> <span className="text-white">{selectedSession.room}</span></div>
                    </div>
                </Card>
            </div>

            {/* Center: Scanning Interface */}
            <div className="lg:col-span-2 space-y-6">
                <Card glass className="p-8 border-white/10 relative overflow-hidden h-[400px] flex flex-col items-center justify-center">
                    {/* Retro Grid Background */}
                    <div className="absolute inset-0 opacity-[0.03] pointer-events-none bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:20px_20px]" />
                    
                    {/* Animated Scan Line */}
                    <motion.div 
                        animate={{ top: ['10%', '90%', '10%'] }}
                        transition={{ duration: 4, repeat: Infinity, ease: 'linear' }}
                        className="absolute left-0 right-0 h-0.5 bg-amber-500/40 shadow-[0_0_15px_rgba(245,158,11,0.5)] z-10"
                    />

                    <div className="relative z-20 w-full max-w-md text-center">
                        <div className="w-20 h-20 bg-white/5 border border-white/10 rounded-3xl mx-auto mb-8 flex items-center justify-center text-amber-500 animate-pulse">
                            <Scan size={40} />
                        </div>
                        
                        <h2 className="text-2xl font-bold text-white mb-2">Initiate Identity Scan</h2>
                        <p className="text-slate-400 text-sm mb-8">Scan student barcode or manually enter Registration ID</p>

                        <form onSubmit={handleScan} className="flex gap-4">
                            <input 
                                type="text" 
                                placeholder="REG-ID / BARCODE-NUM"
                                className="flex-1 bg-white/5 border border-white/10 rounded-xl px-6 py-4 text-white font-mono text-lg outline-none focus:border-amber-500/50 transition-all placeholder:text-slate-700"
                                value={barcode}
                                onChange={(e) => setBarcode(e.target.value)}
                                autoFocus
                            />
                            <Button 
                                type="submit" 
                                className="h-auto px-8 bg-amber-600 hover:bg-amber-700 text-white shadow-xl shadow-amber-600/20"
                                disabled={scanning || !barcode.trim()}
                            >
                                {scanning ? <Loading size="sm" /> : 'VERIFY'}
                            </Button>
                        </form>
                    </div>

                    {/* Corner Borders */}
                    <div className="absolute top-4 left-4 w-8 h-8 border-t-2 border-l-2 border-amber-500/30 rounded-tl-xl" />
                    <div className="absolute top-4 right-4 w-8 h-8 border-t-2 border-r-2 border-amber-500/30 rounded-tr-xl" />
                    <div className="absolute bottom-4 left-4 w-8 h-8 border-b-2 border-l-2 border-amber-500/30 rounded-bl-xl" />
                    <div className="absolute bottom-4 right-4 w-8 h-8 border-b-2 border-r-2 border-amber-500/30 rounded-br-xl" />
                </Card>

                {/* Last Result Notification */}
                <AnimatePresence>
                    {lastScanResult && (
                        <motion.div 
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0 }}
                            className={`p-6 rounded-2xl border flex items-center gap-6 shadow-2xl ${
                                lastScanResult.error 
                                ? 'bg-red-500/10 border-red-500/20 text-red-400' 
                                : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                            }`}
                        >
                            <div className={`p-3 rounded-xl ${lastScanResult.error ? 'bg-red-500/20' : 'bg-emerald-500/20'}`}>
                                {lastScanResult.error ? <XCircle size={32} /> : <CheckCircle2 size={32} />}
                            </div>
                            <div className="flex-1">
                                <h4 className="font-bold text-lg">{lastScanResult.error ? 'Verification Denied' : 'Identity Verified'}</h4>
                                <p className="text-sm opacity-80">
                                    {lastScanResult.error 
                                        ? lastScanResult.error 
                                        : `Student ${lastScanResult.studentName || lastScanResult.registrationNumber} authenticated at ${new Date().toLocaleTimeString()}`
                                    }
                                </p>
                            </div>
                            {!lastScanResult.error && (
                                <div className="text-right">
                                    <p className="text-[10px] font-bold tracking-widest opacity-60 uppercase">Security Node</p>
                                    <p className="text-xs font-mono font-bold tracking-tighter">RSA-ENC-VERIFIED</p>
                                </div>
                            )}
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
