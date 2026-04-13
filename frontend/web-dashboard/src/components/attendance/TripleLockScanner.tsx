'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield, MapPin, Fingerprint, ScanFace, CheckCircle2, AlertTriangle, Activity, Terminal } from 'lucide-react';

export interface ScanPing {
  id: string;
  studentId: string;
  timestamp: string;
  locks: {
    geo: 'PENDING' | 'PASS' | 'FAIL';
    device: 'PENDING' | 'PASS' | 'FAIL';
    bio: 'PENDING' | 'PASS' | 'FAIL';
  };
  status: 'SCANNING' | 'CLEARED' | 'REJECTED';
}

interface TripleLockScannerProps {
  liveData?: ScanPing | null;
}

// Audio Helper for Sensory Feedback
const playTone = (type: 'success' | 'warning') => {
  try {
    const audio = new Audio(`/sounds/${type}.mp3`);
    audio.volume = type === 'success' ? 0.3 : 0.6;
    audio.play().catch(() => { /* Silent failure if DOM blocked */ });
  } catch (e) {}
};

export const TripleLockScanner: React.FC<TripleLockScannerProps> = ({ liveData }) => {
  const [queue, setQueue] = useState<ScanPing[]>([]);
  const [activeScan, setActiveScan] = useState<ScanPing | null>(null);
  const [isSimulating, setIsSimulating] = useState(!liveData);
  const [soundEnabled, setSoundEnabled] = useState(false); // User must toggle to bypass browser block

  // 1. Simulation Engine Pipeline
  useEffect(() => {
    if (liveData) {
      processPing(liveData);
      setIsSimulating(false);
      return;
    }

    if (!isSimulating) return;

    const interval = setInterval(() => {
      // Simulate random ping
      const newId = Math.random().toString(36).substring(7);
      const isFailed = Math.random() > 0.8; // 20% chance of failure (e.g. GPS Drift or bad Bio)

      const ping: ScanPing = {
        id: newId,
        studentId: `STU-${Math.floor(Math.random() * 9000) + 1000}`,
        timestamp: new Date().toISOString(),
        locks: { geo: 'PENDING', device: 'PENDING', bio: 'PENDING' },
        status: 'SCANNING'
      };

      simulateLockSequence(ping, isFailed);
    }, 4500);

    return () => clearInterval(interval);
  }, [liveData, isSimulating]);

  const processPing = (ping: ScanPing) => {
    // Logic for processing actual prop pushes 
    setActiveScan(ping);
    addToQueue(ping);
  };

  const simulateLockSequence = async (ping: ScanPing, willFail: boolean) => {
    setActiveScan(ping);
    
    // Helper to simulate delay
    const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

    const pushUpdate = (updates: Partial<ScanPing>) => {
      const updated = { ...ping, ...updates };
      ping = updated;
      setActiveScan(updated);
    };

    // Lock 1: Geofence
    await delay(600);
    const geoFail = willFail && Math.random() > 0.6;
    pushUpdate({ locks: { ...ping.locks, geo: geoFail ? 'FAIL' : 'PASS' } });
    if (geoFail) { triggerFail(ping); return; }
    if (soundEnabled) playTone('success');

    // Lock 2: Device Fingerprint
    await delay(800);
    const devFail = willFail && Math.random() > 0.6;
    pushUpdate({ locks: { ...ping.locks, device: devFail ? 'FAIL' : 'PASS' } });
    if (devFail) { triggerFail(ping); return; }
    if (soundEnabled) playTone('success');

    // Lock 3: Biometric
    await delay(1200);
    const bioFail = willFail; // if we haven't failed yet, and we must fail, do it here
    pushUpdate({ locks: { ...ping.locks, bio: bioFail ? 'FAIL' : 'PASS' }, status: bioFail ? 'REJECTED' : 'CLEARED' });
    
    if (bioFail) {
      triggerFail(ping);
    } else {
      if (soundEnabled) playTone('success');
      addToQueue(ping);
    }
    
    await delay(1000);
    setActiveScan(null);
  };

  const triggerFail = (ping: ScanPing) => {
    ping.status = 'REJECTED';
    addToQueue(ping);
    if (soundEnabled) playTone('warning');
  };

  const addToQueue = (completedPing: ScanPing) => {
    setQueue(prev => {
      const q = [completedPing, ...prev];
      return q.slice(0, 10); // Keep last 10
    });
  };

  const getLockColor = (status: string) => {
    switch (status) {
      case 'PASS': return 'text-emerald-400 border-emerald-500/50 shadow-[0_0_15px_rgba(16,185,129,0.3)] bg-emerald-500/10';
      case 'FAIL': return 'text-accent border-accent/50 shadow-[0_0_15px_rgba(244,63,94,0.3)] bg-accent/10';
      default: return 'text-slate-500 border-slate-700 bg-obsidian-900/50';
    }
  };

  const getLockIcon = (status: string, DefaultIcon: React.ElementType) => {
    if (status === 'PASS') return <CheckCircle2 className="h-6 w-6 text-emerald-400" />;
    if (status === 'FAIL') return <AlertTriangle className="h-6 w-6 text-accent" />;
    return <DefaultIcon className="h-6 w-6 opacity-30" />;
  };

  return (
    <div className="w-full flex flex-col lg:flex-row gap-8">
      
      {/* LEFT: Scanning Engine (60%) */}
      <div className="w-full lg:w-[60%] flex flex-col space-y-6">
        
        {/* Header Controls */}
        <div className="glass-panel p-6 flex justify-between items-center">
          <div>
            <h2 className="text-2xl font-bold text-white flex items-center tracking-tight">
              <Shield className="h-6 w-6 text-primary mr-3" />
              Triple-Lock Guardian
            </h2>
            <p className="text-slate-400 text-sm mt-1">Identity validation via Hardware, Bio, and Geo constraints.</p>
          </div>
          <div className="flex space-x-4 items-center">
            <label className="flex items-center space-x-2 cursor-pointer bg-white/5 py-1.5 px-3 rounded-lg border border-white/10 hover:bg-white/10 transition-colors">
              <input type="checkbox" checked={soundEnabled} onChange={(e) => setSoundEnabled(e.target.checked)} className="rounded bg-obsidian-900 border-white/20 accent-primary" />
              <span className="text-xs font-medium text-slate-300">Audio FX</span>
            </label>
            <div className={`px-3 py-1.5 rounded-full border text-xs font-bold tracking-widest uppercase ${isSimulating ? 'bg-primary/20 text-primary border-primary/30 animate-pulse' : 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'}`}>
              {isSimulating ? 'Simulation' : 'Live Mode'}
            </div>
          </div>
        </div>

        {/* Central Scan Ring UI */}
        <div className="glass-panel p-8 relative overflow-hidden flex-1 min-h-[450px] flex items-center justify-center border-t border-t-primary/20 bg-gradient-to-br from-obsidian-900 via-[#150D2B] to-[#0A0614]">
          
          {/* Background Pulse Ambience */}
          <motion.div 
            animate={{ scale: activeScan ? [1, 1.2, 1] : 1, opacity: activeScan ? [0.1, 0.3, 0.1] : 0.05 }} 
            transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
            className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-primary/30 rounded-full blur-[100px] pointer-events-none" 
          />

          {activeScan ? (
            <motion.div 
              key="scanning"
              initial={{ opacity: 0, scale: 0.9 }} 
              animate={{ opacity: 1, scale: 1 }} 
              exit={{ opacity: 0, scale: 1.1 }}
              className="relative z-10 w-full max-w-sm"
            >
              <div className="text-center mb-8">
                <span className="text-primary tracking-[0.2em] text-xs font-bold uppercase block mb-2 animate-pulse">Acquiring Target</span>
                <h3 className="text-4xl font-mono font-bold text-white tracking-tight drop-shadow-[0_0_10px_rgba(255,255,255,0.3)]">{activeScan.studentId}</h3>
              </div>

              <div className="space-y-4">
                {/* Lock 1 */}
                <motion.div layout className={`flex items-center justify-between p-4 rounded-xl border backdrop-blur-md transition-all duration-300 ${getLockColor(activeScan.locks.geo)}`}>
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-obsidian-900/50 rounded-lg">{getLockIcon(activeScan.locks.geo, MapPin)}</div>
                    <div>
                      <h4 className="text-white font-bold text-sm uppercase tracking-wide">1. GNSS Bounds</h4>
                      <p className="text-xs opacity-70">Spatial boundary verification</p>
                    </div>
                  </div>
                  {activeScan.locks.geo === 'PENDING' && <Activity className="h-5 w-5 animate-spin text-primary" />}
                </motion.div>

                {/* Lock 2 */}
                <motion.div layout className={`flex items-center justify-between p-4 rounded-xl border backdrop-blur-md transition-all duration-300 ${getLockColor(activeScan.locks.device)}`}>
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-obsidian-900/50 rounded-lg">{getLockIcon(activeScan.locks.device, Fingerprint)}</div>
                    <div>
                      <h4 className="text-white font-bold text-sm uppercase tracking-wide">2. Hardware Key</h4>
                      <p className="text-xs opacity-70">Device fingerprint match</p>
                    </div>
                  </div>
                  {activeScan.locks.device === 'PENDING' && <Activity className="h-5 w-5 animate-spin text-secondary" />}
                </motion.div>

                {/* Lock 3 */}
                <motion.div layout className={`flex items-center justify-between p-4 rounded-xl border backdrop-blur-md transition-all duration-300 ${getLockColor(activeScan.locks.bio)}`}>
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-obsidian-900/50 rounded-lg">{getLockIcon(activeScan.locks.bio, ScanFace)}</div>
                    <div>
                      <h4 className="text-white font-bold text-sm uppercase tracking-wide">3. Biometric Matrix</h4>
                      <p className="text-xs opacity-70">Facial geometry match</p>
                    </div>
                  </div>
                  {activeScan.locks.bio === 'PENDING' && <Activity className="h-5 w-5 animate-spin text-primary" />}
                </motion.div>
              </div>

              {/* Status Header */}
              {activeScan.status !== 'SCANNING' && (
                <motion.div 
                  initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                  className={`mt-6 text-center py-3 rounded-lg border ${activeScan.status === 'CLEARED' ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400' : 'bg-accent/20 border-accent/40 text-accent'}`}
                >
                  <span className="font-bold tracking-widest uppercase">{activeScan.status === 'CLEARED' ? 'Identity Verified' : 'Access Denied'}</span>
                </motion.div>
              )}
            </motion.div>
          ) : (
            <motion.div 
              key="idle"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              className="flex flex-col items-center text-center z-10"
            >
              <div className="relative">
                <div className="absolute inset-0 border-4 border-slate-700 rounded-full animate-[spin_4s_linear_infinite] border-t-primary w-24 h-24" />
                <div className="w-24 h-24 bg-obsidian-800 rounded-full flex items-center justify-center border border-white/5">
                  <Shield className="h-10 w-10 text-slate-500" />
                </div>
              </div>
              <p className="mt-8 text-slate-400 font-mono tracking-widest text-sm uppercase">Awaiting Target Vector...</p>
            </motion.div>
          )}
        </div>
      </div>

      {/* RIGHT: Terminal Log (40%) */}
      <div className="w-full lg:w-[40%] flex flex-col h-full">
        <div className="glass-panel flex-1 flex flex-col overflow-hidden max-h-[80vh] border-t border-t-secondary/20">
          
          <div className="p-4 border-b border-white/5 bg-obsidian-900/80 flex items-center justify-between z-10">
            <h3 className="text-white font-bold tracking-widest text-sm flex items-center uppercase">
              <Terminal className="h-4 w-4 mr-2 text-secondary" />
              Event Stream
            </h3>
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
            <AnimatePresence initial={false}>
              {queue.map((ping) => (
                <motion.div
                  key={ping.id}
                  layout
                  initial={{ opacity: 0, x: 20, scale: 0.95 }}
                  animate={{ opacity: 1, x: 0, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  transition={{ type: "spring", stiffness: 300, damping: 25 }}
                  className="bg-obsidian-800 border border-white/5 p-4 rounded-xl flex items-center justify-between hover:bg-white/[0.03] transition-colors"
                >
                  <div className="flex items-center space-x-4">
                    <div className={`p-2 rounded-full flex-shrink-0 ${ping.status === 'CLEARED' ? 'bg-emerald-500/10 text-emerald-400 shadow-[0_0_10px_rgba(16,185,129,0.2)]' : 'bg-accent/10 text-accent shadow-[0_0_10px_rgba(244,63,94,0.2)]'}`}>
                      {ping.status === 'CLEARED' ? <CheckCircle2 className="h-5 w-5" /> : <AlertTriangle className="h-5 w-5" />}
                    </div>
                    <div>
                      <p className="text-white font-mono font-bold">{ping.studentId}</p>
                      <div className="flex space-x-2 mt-1">
                        <span className="text-[10px] uppercase tracking-widest text-slate-500">{new Date(ping.timestamp).toLocaleTimeString()}</span>
                      </div>
                    </div>
                  </div>
                  
                  {/* Miniature Locks Indicator */}
                  <div className="flex space-x-1">
                    <div className={`w-2 h-2 rounded-full ${ping.locks.geo === 'PASS' ? 'bg-emerald-400' : 'bg-accent'}`} title="Geo" />
                    <div className={`w-2 h-2 rounded-full ${ping.locks.device === 'PASS' ? 'bg-emerald-400' : 'bg-accent'}`} title="Device" />
                    <div className={`w-2 h-2 rounded-full ${ping.locks.bio === 'PASS' ? 'bg-emerald-400' : 'bg-accent'}`} title="Bio" />
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>

            {queue.length === 0 && (
              <div className="h-full flex flex-col items-center justify-center text-slate-500 space-y-4 py-20">
                <Terminal className="h-8 w-8 opacity-50" />
                <p className="text-sm font-mono text-center">Log empty. Waiting for packets.</p>
              </div>
            )}
          </div>
        </div>
      </div>

    </div>
  );
};
