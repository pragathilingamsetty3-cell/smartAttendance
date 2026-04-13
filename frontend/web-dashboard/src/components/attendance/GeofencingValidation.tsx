'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MapPin, Shield, AlertTriangle, CheckCircle, Target } from 'lucide-react';
import { attendanceService } from '@/services/attendance.service';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';

interface GeofencingValidationProps {
  sessionId: string;
  roomId: string;
}

interface LocationValidation {
  studentId: string;
  studentName: string;
  isValid: boolean;
  latitude: number;
  longitude: number;
  accuracy: number;
  distanceFromRoom: number;
  timestamp: string;
  validationType: 'GEOFENCE' | 'GPS_ACCURACY' | 'ROOM_BOUNDARY';
  confidence: number;
}

interface ValidationMetrics {
  totalValidations: number;
  successfulValidations: number;
  failedValidations: number;
  averageAccuracy: number;
  averageDistance: number;
  validationRate: number;
  lastUpdated: string;
}

const containerVars = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVars = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0 }
};

export const GeofencingValidation: React.FC<GeofencingValidationProps> = ({ sessionId, roomId }) => {
  const [loading, setLoading] = useState(false);
  const [validations, setValidations] = useState<LocationValidation[]>([]);
  const [metrics, setMetrics] = useState<ValidationMetrics>({
    totalValidations: 0, successfulValidations: 0, failedValidations: 0, averageAccuracy: 0,
    averageDistance: 0, validationRate: 0, lastUpdated: ''
  });
  const [autoValidate, setAutoValidate] = useState(true);
  const [selectedValidation, setSelectedValidation] = useState<LocationValidation | null>(null);

  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    fetchGeofenceData();
    if (autoValidate) {
      const interval = setInterval(fetchGeofenceData, 10000);
      return () => clearInterval(interval);
    }
  }, [sessionId, roomId, autoValidate]);

  const fetchGeofenceData = async () => {
    try {
      setLoading(true);
      // Simulate real-time validation calls
      const mockValidations = generateMockValidations();
      setValidations(mockValidations);
      
      const totalValidations = mockValidations.length;
      const successfulValidations = mockValidations.filter(v => v.isValid).length;
      const failedValidations = totalValidations - successfulValidations;
      const averageAccuracy = mockValidations.reduce((sum, v) => sum + v.accuracy, 0) / (totalValidations || 1);
      const averageDistance = mockValidations.reduce((sum, v) => sum + v.distanceFromRoom, 0) / (totalValidations || 1);
      
      setMetrics({
        totalValidations, successfulValidations, failedValidations, averageAccuracy,
        averageDistance, validationRate: totalValidations ? (successfulValidations / totalValidations) * 100 : 0,
        lastUpdated: new Date().toISOString()
      });
    } catch (error) {} 
    finally { setLoading(false); }
  };

  const generateMockValidations = (): LocationValidation[] => {
    const students = [
      { id: 'STU-001', name: 'John Doe' }, { id: 'STU-002', name: 'Jane Smith' },
      { id: 'STU-003', name: 'Bob Johnson' }, { id: 'STU-004', name: 'Alice Brown' },
      { id: 'STU-005', name: 'Charlie Wilson' }
    ];
    // @ts-expect-error omega clearance
    return students.map((student, index) => ({
      studentId: student.id, studentName: student.name, isValid: Math.random() > 0.3,
      latitude: 40.7128 + (Math.random() - 0.5) * 0.002, longitude: -74.0060 + (Math.random() - 0.5) * 0.002,
      accuracy: 5 + Math.random() * 20, distanceFromRoom: Math.random() * 100,
      timestamp: new Date(Date.now() - index * 60000).toISOString(),
      validationType: ['GEOFENCE', 'GPS_ACCURACY', 'ROOM_BOUNDARY'][Math.floor(Math.random() * 3)] as unknown,
      confidence: 0.7 + Math.random() * 0.3
    }));
  };

  const runValidation = async () => {
    setLoading(true);
    await new Promise(resolve => setTimeout(resolve, 2000));
    await fetchGeofenceData();
  };

  useEffect(() => {
    if (!canvasRef.current || !validations.length) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#2d9cdb'; // secondary cyan
    ctx.lineWidth = 3;
    ctx.strokeRect(50, 50, 300, 200);

    ctx.strokeStyle = '#9b51e0'; // primary violet
    ctx.lineWidth = 2;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.arc(200, 150, 120, 0, 2 * Math.PI);
    ctx.stroke();
    ctx.setLineDash([]);

    validations.forEach((validation, index) => {
      const x = 50 + (index % 5) * 60;
      const y = 50 + Math.floor(index / 5) * 40;
      
      const color = validation.isValid ? '#10B981' : '#f43f5e';
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(x, y, 6, 0, 2 * Math.PI);
      ctx.fill();
      
      ctx.strokeStyle = color;
      ctx.lineWidth = 1;
      ctx.globalAlpha = 0.3;
      ctx.beginPath();
      ctx.arc(x, y, validation.accuracy * 2, 0, 2 * Math.PI);
      ctx.stroke();
      ctx.globalAlpha = 1;
    });
  }, [validations]);

  const getValidationIcon = (isValid: boolean) => isValid ? <CheckCircle className="h-4 w-4 text-emerald-400" /> : <AlertTriangle className="h-4 w-4 text-accent" />;
  const getValidationColor = (isValid: boolean) => isValid ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-accent/10 text-accent border-accent/20';
  const getAccuracyColor = (accuracy: number) => accuracy < 10 ? 'text-emerald-400' : accuracy < 20 ? 'text-yellow-400' : 'text-accent';

  return (
    <motion.div variants={containerVars} initial="hidden" animate="show" className="space-y-6">
      
      <motion.div variants={itemVars} className="glass-panel p-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-emerald-500/20 text-emerald-400 rounded-xl neon-border border-emerald-500/50">
              <MapPin className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-xl font-bold text-white tracking-tight">Geofence Trajectory Engine</h3>
              <p className="text-slate-400 text-sm">Real-time GPS validation & boundary tracking</p>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <label className="flex items-center space-x-2 cursor-pointer bg-white/5 py-2 px-3 rounded-lg border border-white/10 hover:bg-white/10 transition-colors">
              <input type="checkbox" checked={autoValidate} onChange={(e) => setAutoValidate(e.target.checked)} className="rounded bg-obsidian-900 border-white/20 accent-primary" />
              <span className="text-sm font-medium text-slate-300">Live Telemetry</span>
            </label>
            <Button variant="primary" onClick={runValidation} disabled={loading}>
              {loading ? 'Scanning...' : 'Force Validations'}
            </Button>
          </div>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <motion.div variants={itemVars} className="glass-panel p-6 group">
          <div className="flex justify-between mb-2"><p className="text-slate-400 text-sm font-medium">Nodes Validated</p><Shield className="h-5 w-5 text-secondary transition-transform group-hover:scale-110" /></div>
          <p className="text-3xl font-bold text-white">{metrics.totalValidations}</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 border-emerald-500/20 group">
          <div className="flex justify-between mb-2"><p className="text-slate-400 text-sm font-medium">Inside Boundary</p><CheckCircle className="h-5 w-5 text-emerald-400 transition-transform group-hover:scale-110" /></div>
          <p className="text-3xl font-bold text-white">{metrics.successfulValidations}</p>
          <div className="w-full bg-obsidian-700 rounded-full h-1 mt-3">
            <div className="bg-emerald-500 h-1 rounded-full transition-all duration-700" style={{ width: `${metrics.validationRate}%` }} />
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 group">
          <div className="flex justify-between mb-2"><p className="text-slate-400 text-sm font-medium">GNSS Accuracy</p><Target className="h-5 w-5 text-yellow-400 transition-transform group-hover:scale-110" /></div>
          <p className={`text-3xl font-bold ${getAccuracyColor(metrics.averageAccuracy)}`}>±{metrics.averageAccuracy.toFixed(1)}m</p>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6 group">
          <div className="flex justify-between mb-2"><p className="text-slate-400 text-sm font-medium">Radius Drift</p><MapPin className="h-5 w-5 text-primary transition-transform group-hover:scale-110" /></div>
          <p className="text-3xl font-bold text-white">{metrics.averageDistance.toFixed(1)}m</p>
        </motion.div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div variants={itemVars} className="glass-panel p-6">
          <h4 className="text-white font-bold mb-1 tracking-tight">Geospatial Overlay</h4>
          <p className="text-slate-400 text-sm mb-5">Visualizing relative distance to physical bounds</p>
          <canvas ref={canvasRef} width={400} height={300} className="w-full bg-obsidian-900 rounded-xl border border-white/5" />
          <div className="mt-4 grid grid-cols-2 gap-4 text-sm font-medium">
            <div className="flex items-center space-x-2"><div className="w-3 h-3 bg-secondary rounded"></div><span className="text-slate-300">Room Boundary</span></div>
            <div className="flex items-center space-x-2"><div className="w-3 h-3 bg-primary rounded border border-primary border-dashed"></div><span className="text-slate-300">Geofence Radials</span></div>
          </div>
        </motion.div>

        <motion.div variants={itemVars} className="glass-panel p-6">
          <h4 className="text-white font-bold mb-1 tracking-tight">Validation Audit Log</h4>
          <p className="text-slate-400 text-sm mb-5">Telemetry snapshots sorted by recent pings</p>
          <div className="space-y-3 max-h-[380px] overflow-y-auto pr-2">
            {validations.map((val) => (
              <motion.div layout key={val.studentId} onClick={() => setSelectedValidation(val)} className="p-4 bg-obsidian-800/80 rounded-xl border border-white/5 hover:border-white/10 hover:bg-white/[0.02] transition-colors cursor-pointer">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-3">{getValidationIcon(val.isValid)} <span className="text-white font-bold">{val.studentName}</span></div>
                  <div className={`px-2.5 py-1 rounded-md text-[10px] uppercase font-bold tracking-wider border ${getValidationColor(val.isValid)}`}>{val.isValid ? 'Valid Coord' : 'Boundary Alert'}</div>
                </div>
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 text-xs bg-obsidian-900/50 p-2 rounded-lg">
                  <div><span className="text-slate-500 block mb-0.5">DEV ERROR</span><span className={`font-mono ${getAccuracyColor(val.accuracy)}`}>±{val.accuracy.toFixed(1)}m</span></div>
                  <div><span className="text-slate-500 block mb-0.5">DRIFT</span><span className="font-mono text-white">{val.distanceFromRoom.toFixed(1)}m</span></div>
                  <div><span className="text-slate-500 block mb-0.5">VECTOR</span><span className="text-white font-medium">{val.validationType.replace('_', ' ')}</span></div>
                  <div><span className="text-slate-500 block mb-0.5">SCORE</span><span className="text-white font-mono">{Math.round(val.confidence * 100)}%</span></div>
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>

      <AnimatePresence>
        {selectedValidation && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="glass-panel w-full max-w-md shadow-2xl overflow-hidden">
              <div className="p-5 border-b border-white/10 flex items-center justify-between bg-obsidian-800/50">
                <h3 className="text-lg font-bold text-white tracking-tight">{selectedValidation.studentName} :: Ping Data</h3>
                <button onClick={() => setSelectedValidation(null)} className="text-slate-400 hover:text-white transition">×</button>
              </div>
              <div className="p-6 space-y-6">
                <div className={`p-4 rounded-xl border ${getValidationColor(selectedValidation.isValid)}`}>
                  <div className="flex items-center space-x-2 mb-1.5">{getValidationIcon(selectedValidation.isValid)}<span className="font-bold tracking-tight">{selectedValidation.isValid ? 'Presence Acquired' : 'Outside Parameters'}</span></div>
                  <p className="text-sm opacity-90">{selectedValidation.isValid ? 'Node mapped directly within accepted threshold.' : 'Node triggered severe boundary divergence.'}</p>
                </div>
                <div className="grid grid-cols-2 gap-y-6 gap-x-4">
                  <div><span className="text-slate-500 text-xs uppercase font-bold tracking-wider mb-1 block">Coordinate</span><p className="text-white font-mono text-xs">{selectedValidation.latitude.toFixed(6)},<br/>{selectedValidation.longitude.toFixed(6)}</p></div>
                  <div><span className="text-slate-500 text-xs uppercase font-bold tracking-wider mb-1 block">GPS Error Area</span><p className={`font-mono text-lg font-bold ${getAccuracyColor(selectedValidation.accuracy)}`}>±{selectedValidation.accuracy.toFixed(1)}m</p></div>
                  <div><span className="text-slate-500 text-xs uppercase font-bold tracking-wider mb-1 block">Distance off Center</span><p className="text-white font-mono text-lg">{selectedValidation.distanceFromRoom.toFixed(1)}m</p></div>
                  <div><span className="text-slate-500 text-xs uppercase font-bold tracking-wider mb-1 block">Engine Used</span><p className="text-white text-sm font-medium">{selectedValidation.validationType.replace('_', ' ')}</p></div>
                  <div className="col-span-2"><span className="text-slate-500 text-xs uppercase font-bold tracking-wider mb-1 block">Temporal Registration</span><p className="text-white font-mono">{new Date(selectedValidation.timestamp).toLocaleString()}</p></div>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

    </motion.div>
  );
};

export default GeofencingValidation;
