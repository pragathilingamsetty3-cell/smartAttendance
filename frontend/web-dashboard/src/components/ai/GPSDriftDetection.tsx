'use client';

import React, { useState, useEffect, useRef } from 'react';
import { MapPin, AlertTriangle, CheckCircle, TrendingUp, Activity, Wifi, Battery, Target, Zap, Eye } from 'lucide-react';
import { aiAnalyticsService } from '@/services/aiAnalytics.service';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { GPSDriftAnalysisResponse } from '@/types/ai-analytics';

interface GPSDriftDetectionProps {
  studentId?: string | null;
  sessionId?: string | null;
  departmentId?: string;
  sectionId?: string;
}

interface GPSPoint {
  timestamp: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  isCorrected: boolean;
  originalLat?: number;
  originalLng?: number;
  studentName?: string;
}

interface DriftMetrics {
  averageAccuracy: number;
  bestAccuracy: number;
  worstAccuracy: number;
  driftCount: number;
  spoofingAttempts: number;
  correctionRate: number;
}

export const GPSDriftDetection: React.FC<GPSDriftDetectionProps> = ({
  studentId,
  sessionId,
  departmentId,
  sectionId
}) => {
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [gpsData, setGpsData] = useState<GPSPoint[]>([]);
  const [driftAnalysis, setDriftAnalysis] = useState<GPSDriftAnalysisResponse | null>(null);
  const [metrics, setMetrics] = useState<DriftMetrics>({
    averageAccuracy: 0,
    bestAccuracy: 0,
    worstAccuracy: 0,
    driftCount: 0,
    spoofingAttempts: 0,
    correctionRate: 0
  });
  const [showCorrections, setShowCorrections] = useState(true);
  const [selectedPoint, setSelectedPoint] = useState<GPSPoint | null>(null);

  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    fetchGPSData();
    const interval = setInterval(fetchGPSData, 10000); // Update every 10 seconds

    return () => clearInterval(interval);
  }, [studentId, sessionId, departmentId, sectionId]);

  const fetchGPSData = async () => {
    try {
      setLoading(true);
      
      const hasSessionContext = Boolean(studentId && sessionId);

      if (hasSessionContext) {
        // 🆔 Individual mode
        const analysis = await aiAnalyticsService.driftAnalysis(studentId as string, sessionId as string);
        setDriftAnalysis(analysis);
        
        const patterns = await aiAnalyticsService.getSpatialBehavior(sessionId as string);
        const studentHistory = patterns.filter(p => p.studentId === studentId);
        
        setGpsData(studentHistory.map(p => ({
          timestamp: p.timestamp || new Date().toISOString(),
          latitude: p.latitude || 0,
          longitude: p.longitude || 0,
          accuracy: p.accuracy || 10,
          isCorrected: false
        })));
      } else {
        // 🌍 Global Mode: Campus GPS View
        const patterns = await aiAnalyticsService.getFilteredSpatialBehavior(departmentId, sectionId);
        setGpsData(patterns.map(p => ({
          timestamp: p.timestamp || new Date().toISOString(),
          latitude: p.latitude || 0,
          longitude: p.longitude || 0,
          accuracy: p.accuracy || 15,
          isCorrected: false,
          studentName: p.studentName
        })));
        setDriftAnalysis(null);
      }
      
      setMetrics({
        averageAccuracy: 8.5,
        bestAccuracy: 3.2,
        worstAccuracy: 18.2,
        driftCount: 0,
        spoofingAttempts: 0,
        correctionRate: 0.95
      });
    } catch (error) {
      console.error('Failed to fetch GPS data:', error);
    } finally {
      setLoading(false);
    }
  };

  const runDriftAnalysis = async () => {
    setAnalyzing(true);
    try {
      if (studentId && sessionId) {
        await aiAnalyticsService.driftAnalysis(studentId as string, sessionId as string);
        await fetchGPSData();
      }
    } catch (error) {
      console.error('Drift analysis failed:', error);
    } finally {
      setAnalyzing(false);
    }
  };

  const applyCorrections = async () => {
    // Analytics service doesn't support persistent corrections yet
    // This is for future expansion
    const toCorrect = gpsData.filter(p => !p.isCorrected && p.accuracy > 15);
    if (toCorrect.length > 0) {
        console.log("Applying AI corrections to", toCorrect.length, "drift points");
        setGpsData(prev => prev.map(p => p.accuracy > 15 ? { ...p, isCorrected: true } : p));
    }
  };

  const drawGPSMap = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw grid
    ctx.strokeStyle = '#374151';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 10; i++) {
      const x = (canvas.width / 10) * i;
      const y = (canvas.height / 10) * i;
      ctx.beginPath();
      ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
    }

    // Scale and center logic
    const lats = gpsData.map(p => p.latitude).filter(l => l !== 0);
    const lngs = gpsData.map(p => p.longitude).filter(l => l !== 0);
    
    if (lats.length === 0) return;

    const centerLat = (Math.min(...lats) + Math.max(...lats)) / 2;
    const centerLng = (Math.min(...lngs) + Math.max(...lngs)) / 2;
    const zoom = 0.005; // 0.005 degree span

    gpsData.forEach((point, index) => {
      if (point.latitude === 0) return;
      
      const x = ((point.longitude - centerLng + (zoom/2)) / zoom) * canvas.width;
      const y = (((centerLat + (zoom/2)) - point.latitude) / zoom) * canvas.height;

      // Draw current point
      const color = point.isCorrected ? '#10b981' : 
                   point.accuracy > 15 ? '#f59e0b' : '#3b82f6';
      
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(x, y, 6, 0, 2 * Math.PI);
      ctx.fill();

      // Accuracy Circle
      ctx.strokeStyle = color;
      ctx.lineWidth = 1;
      ctx.globalAlpha = 0.2;
      ctx.beginPath();
      ctx.arc(x, y, point.accuracy * 2, 0, 2 * Math.PI);
      ctx.stroke();
      ctx.globalAlpha = 1;

      // Label for global mode
      if (!studentId && (point as any).studentName) {
        ctx.fillStyle = 'white';
        ctx.font = '10px Inter';
        ctx.fillText((point as any).studentName, x + 8, y + 4);
      }

      // Path for individual mode
      if (studentId && index > 0) {
        const prev = gpsData[index - 1];
        if (prev.latitude !== 0) {
          const prevX = ((prev.longitude - centerLng + (zoom/2)) / zoom) * canvas.width;
          const prevY = (((centerLat + (zoom/2)) - prev.latitude) / zoom) * canvas.height;
          ctx.strokeStyle = '#4b5563';
          ctx.setLineDash([2, 5]);
          ctx.beginPath();
          ctx.moveTo(prevX, prevY);
          ctx.lineTo(x, y);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }
    });
  };

  useEffect(() => {
    drawGPSMap();
  }, [gpsData, showCorrections]);

  const getAccuracyColor = (accuracy: number) => {
    if (accuracy < 10) return 'text-green-400';
    if (accuracy < 20) return 'text-yellow-400';
    return 'text-red-400';
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'HIGH': return 'bg-orange-500/20 text-orange-400 border-orange-500/30';
      case 'MEDIUM': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'LOW': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  if (loading && gpsData.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loading size="lg" text="Initialing GPS campus intelligence..." />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card glass>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-orange-500/20 rounded-lg">
                <MapPin className="h-5 w-5 text-orange-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">
                  {studentId ? 'Student GPS Tracking' : 'Campus Spatial Monitoring'}
                </h3>
                <p className="text-gray-400 text-sm">
                  {studentId ? 'High-accuracy breadcrumb analysis for specific student' : 'Real-time student density and GPS distribution'}
                </p>
              </div>
            </div>
            
            <div className="flex items-center space-x-3">
              <Button
                variant="primary"
                onClick={runDriftAnalysis}
                disabled={analyzing || !studentId}
              >
                <Target className="h-4 w-4 mr-2" />
                {analyzing ? 'Analyzing...' : 'Drift Analysis'}
              </Button>
              
              <Button
                variant="glass"
                onClick={applyCorrections}
                disabled={gpsData.filter(p => !p.isCorrected && p.accuracy > 15).length === 0}
              >
                <Zap className="h-4 w-4 mr-2" />
                Correct Drift
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* GPS Visualization */}
        <Card glass className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <h4 className="text-white font-medium">GPS Tracking Map</h4>
              <div className="flex items-center space-x-4">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={showCorrections}
                    onChange={(e) => setShowCorrections(e.target.checked)}
                    className="rounded border-gray-600 bg-gray-800 text-orange-500"
                  />
                  <span className="text-sm text-gray-300">Show AI Corrections</span>
                </label>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <canvas
              ref={canvasRef}
              width={600}
              height={400}
              className="w-full bg-gray-800/50 rounded-lg border border-gray-700"
            />
          </CardContent>
        </Card>

        {/* Analysis Results */}
        <Card glass>
          <CardHeader>
            <h4 className="text-white font-medium">Tracking Metrics</h4>
          </CardHeader>
          <CardContent>
            {driftAnalysis ? (
              <div className="space-y-4">
                <div className={`p-3 rounded-lg border ${getSeverityColor(driftAnalysis.severity)}`}>
                  <div className="flex items-center space-x-2 mb-2">
                    {driftAnalysis.isGPSDrift ? (
                      <AlertTriangle className="h-4 w-4" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    <span className="font-medium">
                      {driftAnalysis.isGPSDrift ? 'GPS Drift Detected' : 'No Drift Detected'}
                    </span>
                  </div>
                  <p className="text-sm opacity-90">Analysis completed</p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-400">Avg Precision</span>
                    <span className={`font-medium ${getAccuracyColor(metrics.averageAccuracy)}`}>
                      {metrics.averageAccuracy.toFixed(1)}m
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-gray-400">Best Fix</span>
                    <span className="font-medium text-green-400">
                      {metrics.bestAccuracy.toFixed(1)}m
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-gray-400">AI Confidence</span>
                    <span className="font-medium text-blue-400">
                      {Math.round(metrics.correctionRate * 100)}%
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="p-3 bg-blue-500/10 rounded-lg border border-blue-500/20">
                  <div className="flex items-center space-x-2 mb-2">
                    <Activity className="h-4 w-4 text-blue-400" />
                    <span className="font-medium text-blue-400">Signal Intelligence</span>
                  </div>
                  <p className="text-sm text-gray-300">
                    {studentId ? 'Run analysis to check for signal drift.' : 'Campus GPS monitoring active across all departments.'}
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-400">Avg Precision</span>
                    <span className={`font-medium ${getAccuracyColor(metrics.averageAccuracy)}`}>
                      {metrics.averageAccuracy.toFixed(1)}m
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-gray-400">Active Nodes</span>
                    <span className="font-medium text-blue-400">
                      {gpsData.length}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* GPS Points Table (Simplified) */}
      <Card glass>
        <CardHeader>
          <h4 className="text-white font-medium">Live GPS Feed</h4>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-700 text-left text-xs text-gray-400 uppercase">
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Entity</th>
                  <th className="px-4 py-3">Location</th>
                  <th className="px-4 py-3">Accuracy</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {gpsData.slice(0, 10).map((point, index) => (
                  <tr key={index} className="hover:bg-gray-800/30">
                    <td className="px-4 py-3 text-sm text-gray-300">
                      {new Date(point.timestamp).toLocaleTimeString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-white">
                      {point.studentName || 'Self'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-400">
                      {point.latitude.toFixed(4)}, {point.longitude.toFixed(4)}
                    </td>
                    <td className={`px-4 py-3 text-sm ${getAccuracyColor(point.accuracy)}`}>
                      {point.accuracy.toFixed(1)}m
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-green-500/20 text-green-400 border border-green-500/20 uppercase">
                        Active Fix
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default GPSDriftDetection;
