'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Map, Activity, AlertTriangle, TrendingUp, Users, Target, Zap, Eye, Settings } from 'lucide-react';
import { safeParseInt } from '@/utils/numberUtils';
import { aiAnalyticsService } from '@/services/aiAnalytics.service';
import { SpatialAnalysisResponse, GPSDriftAnalysisResponse, AIAlert, MovementPattern, HeatmapPoint } from '@/types/ai-analytics';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';

interface SpatialBehaviorDashboardProps {
  sessionId: string;
  departmentId?: string;
  sectionId?: string;
}

export const SpatialBehaviorDashboard: React.FC<SpatialBehaviorDashboardProps> = ({
  sessionId,
  departmentId,
  sectionId
}) => {
  const [loading, setLoading] = useState(true);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [heatmapData, setHeatmapData] = useState<HeatmapPoint[]>([]);
  const [movementPatterns, setMovementPatterns] = useState<MovementPattern[]>([]);
  const [alerts, setAlerts] = useState<AIAlert[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [analysisSettings, setAnalysisSettings] = useState({
    sensitivity: 'medium',
    updateInterval: 5000,
    showAnomalies: true,
    showPaths: true,
    showHeatmap: true
  });
  const [patternFilter, setPatternFilter] = useState<string>('ALL');

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number | null>(null);

  useEffect(() => {
    fetchSpatialData();
    startRealTimeUpdates();

    return () => {
      stopRealTimeUpdates();
    };
  }, [sessionId, analysisSettings]);

  const fetchSpatialData = async () => {
    try {
      setLoading(true);
      
      // Fetch movement patterns (would be enhanced with real API)
      const patterns = await fetchMovementPatterns();
      setMovementPatterns(patterns);
      
      // Generate heatmap data based on real student physical locations
      const coordinates = patterns.map(p => ({
        x: p.longitude ? ((p.longitude % 0.001) * 400000) % 380 + 10 : Math.random() * 380 + 10,
        y: p.latitude ? ((p.latitude % 0.001) * 400000) % 280 + 10 : Math.random() * 280 + 10,
        intensity: p.status === 'WALK_OUT' ? 0.9 : (p.accuracy ? (1 - (p.accuracy / 100)) : (p.confidence || 0.5)),
        studentId: p.studentId
      }));
      setHeatmapData(coordinates);

      // Fetch alerts with filters
      const alertData = await aiAnalyticsService.getActiveAlerts(departmentId, sectionId);
      
      if (sessionId) {
        setAlerts(alertData.filter(alert => alert.sessionId === sessionId));
      } else {
        setAlerts(alertData); // Global view: show all filtered alerts
      }
    } catch (error) {
      console.error('Failed to fetch spatial data:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchMovementPatterns = async (): Promise<MovementPattern[]> => {
    try {
      if (sessionId) {
        return await aiAnalyticsService.getSpatialBehavior(sessionId);
      } else {
        // Global Campus/Department View
        return await aiAnalyticsService.getFilteredSpatialBehavior(departmentId, sectionId);
      }
    } catch (error) {
      console.error('Failed to fetch movement patterns:', error);
      return [];
    }
  };

  const startRealTimeUpdates = () => {
    const interval = setInterval(() => {
      fetchSpatialData();
    }, analysisSettings.updateInterval) as unknown as number;

    animationRef.current = interval;
  };

  const stopRealTimeUpdates = () => {
    if (animationRef.current) {
      clearInterval(animationRef.current);
      animationRef.current = null;
    }
  };

  const runSpatialAnalysis = async () => {
    setIsAnalyzing(true);
    try {
      // Use actual students from the movement patterns data instead of hardcoded IDs
      const studentIds = movementPatterns.map(p => p.studentId);
      
      if (studentIds.length === 0) {
        console.warn('No active students found in session to analyze');
        setIsAnalyzing(false);
        return;
      }

      for (const studentId of studentIds) {
        await aiAnalyticsService.spatialAnalysis(studentId, sessionId || '');
      }
      
      // Refresh data after analysis
      await fetchSpatialData();
    } catch (error) {
      console.error('Spatial analysis failed:', error);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const drawHeatmap = () => {
    const canvas = canvasRef.current;
    if (!canvas || !analysisSettings.showHeatmap) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw room boundary
    ctx.strokeStyle = '#374151';
    ctx.lineWidth = 2;
    ctx.strokeRect(10, 10, canvas.width - 20, canvas.height - 20);

    // Draw heatmap points
    heatmapData.forEach(point => {
      const intensity = point.intensity;
      const radius = Math.max(10, intensity * 30);
      
      // Create gradient for heat effect
      const gradient = ctx.createRadialGradient(point.x, point.y, 0, point.x, point.y, radius);
      
      if (intensity > 0.7) {
        gradient.addColorStop(0, 'rgba(239, 68, 68, 0.8)'); // Red
        gradient.addColorStop(1, 'rgba(239, 68, 68, 0)');
      } else if (intensity > 0.4) {
        gradient.addColorStop(0, 'rgba(251, 191, 36, 0.8)'); // Yellow
        gradient.addColorStop(1, 'rgba(251, 191, 36, 0)');
      } else {
        gradient.addColorStop(0, 'rgba(34, 197, 94, 0.8)'); // Green
        gradient.addColorStop(1, 'rgba(34, 197, 94, 0)');
      }
      
      ctx.fillStyle = gradient;
      ctx.fillRect(point.x - radius, point.y - radius, radius * 2, radius * 2);
    });

    // Draw movement paths if enabled
    if (analysisSettings.showPaths) {
      ctx.strokeStyle = 'rgba(59, 130, 246, 0.5)';
      ctx.lineWidth = 1;
      ctx.setLineDash([5, 5]);
      
      // Draw sample paths (would come from real data)
      movementPatterns.forEach(pattern => {
        if (pattern.pattern !== 'STATIONARY' && pattern.latitude && pattern.longitude) {
          const x = ((pattern.longitude % 0.001) * 400000) % 380 + 10;
          const y = ((pattern.latitude % 0.001) * 400000) % 280 + 10;
          
          ctx.beginPath();
          ctx.moveTo(x - 10, y - 10);
          ctx.lineTo(x, y);
          ctx.stroke();
        }
      });
      
      ctx.setLineDash([]);
    }
  };

  useEffect(() => {
    drawHeatmap();
  }, [heatmapData, analysisSettings]);

  const getPatternIcon = (pattern: string) => {
    switch (pattern) {
      case 'STATIONARY':
        return <Users className="h-4 w-4 text-green-400" />;
      case 'WALKING':
        return <Activity className="h-4 w-4 text-blue-400" />;
      case 'RUNNING':
        return <Zap className="h-4 w-4 text-yellow-400" />;
      case 'MOVING':
        return <Activity className="h-4 w-4 text-purple-400" />;
      case 'ERRATIC':
        return <AlertTriangle className="h-4 w-4 text-red-400" />;
      default:
        return <Activity className="h-4 w-4 text-gray-400" />;
    }
  };

  const getPatternColor = (pattern: string) => {
    switch (pattern) {
      case 'STATIONARY':
        return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'WALKING':
        return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      case 'RUNNING':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'MOVING':
        return 'bg-purple-500/20 text-purple-400 border-purple-500/30';
      case 'ERRATIC':
        return 'bg-red-500/20 text-red-400 border-red-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  const getAlertSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'HIGH':
        return 'bg-orange-500/20 text-orange-400 border-orange-500/30';
      case 'MEDIUM':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'LOW':
        return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loading size="lg" text="Loading spatial behavior data..." />
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
              <div className="p-2 bg-purple-500/20 rounded-lg">
                <Map className="h-5 w-5 text-purple-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">
                  {sessionId ? 'Spatial Behavior Analysis' : 'Campus Spatial Intelligence'}
                </h3>
                <p className="text-gray-400 text-sm">
                  {sessionId ? 'AI-powered movement pattern detection for this session' : 'Aggregated campus-wide movement and anomaly tracking'}
                </p>
              </div>
            </div>
            
            <div className="flex items-center space-x-3">
              <Button
                variant="primary"
                onClick={runSpatialAnalysis}
                disabled={isAnalyzing}
              >
                <Target className="h-4 w-4 mr-2" />
                {isAnalyzing ? 'Analyzing...' : 'Run Analysis'}
              </Button>
              
              <Button
                variant="glass"
                onClick={() => setShowSettings(!showSettings)}
              >
                <Settings className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Settings Panel */}
      {showSettings && (
        <Card glass>
          <CardHeader>
            <h4 className="text-white font-medium">Analysis Settings</h4>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Sensitivity
                </label>
                <select
                  value={analysisSettings.sensitivity}
                  onChange={(e) => setAnalysisSettings(prev => ({ ...prev, sensitivity: e.target.value }))}
                  className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-purple-500 focus:ring-2 focus:ring-purple-500/20"
                >
                  <option value="low">Low</option>
                  <option value="medium">Medium</option>
                  <option value="high">High</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Update Interval (ms)
                </label>
                <input
                  type="number"
                  value={analysisSettings.updateInterval}
                  onChange={(e) => {
                    setAnalysisSettings(prev => ({ ...prev, updateInterval: safeParseInt(e.target.value, 5000) }));
                  }}
                  className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-purple-500 focus:ring-2 focus:ring-purple-500/20"
                />
              </div>
              
              <div className="space-y-2">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={analysisSettings.showAnomalies}
                    onChange={(e) => setAnalysisSettings(prev => ({ ...prev, showAnomalies: e.target.checked }))}
                    className="rounded border-gray-600 bg-gray-800 text-purple-500 focus:ring-purple-500"
                  />
                  <span className="text-sm text-gray-300">Show Anomalies</span>
                </label>
                
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={analysisSettings.showPaths}
                    onChange={(e) => setAnalysisSettings(prev => ({ ...prev, showPaths: e.target.checked }))}
                    className="rounded border-gray-600 bg-gray-800 text-purple-500 focus:ring-purple-500"
                  />
                  <span className="text-sm text-gray-300">Show Paths</span>
                </label>
                
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={analysisSettings.showHeatmap}
                    onChange={(e) => setAnalysisSettings(prev => ({ ...prev, showHeatmap: e.target.checked }))}
                    className="rounded border-gray-600 bg-gray-800 text-purple-500 focus:ring-purple-500"
                  />
                  <span className="text-sm text-gray-300">Show Heatmap</span>
                </label>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Heatmap Visualization */}
        <Card glass>
          <CardHeader>
            <div className="flex items-center justify-between">
              <h4 className="text-white font-medium">Room Heatmap</h4>
              <div className="flex items-center space-x-2">
                <div className="flex items-center space-x-1">
                  <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  <span className="text-xs text-gray-400">Low</span>
                </div>
                <div className="flex items-center space-x-1">
                  <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                  <span className="text-xs text-gray-400">Medium</span>
                </div>
                <div className="flex items-center space-x-1">
                  <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                  <span className="text-xs text-gray-400">High</span>
                </div>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <canvas
              ref={canvasRef}
              width={400}
              height={300}
              className="w-full bg-gray-800/50 rounded-lg border border-gray-700"
            />
            
            <div className="mt-4 text-sm text-gray-400">
              <p>• Intensity represents student concentration</p>
              <p>• Red zones indicate high activity areas</p>
              <p>• Blue zones show low movement areas</p>
            </div>
          </CardContent>
        </Card>

        {/* Movement Patterns */}
        <Card glass>
          <CardHeader>
            <div className="flex items-center justify-between">
              <h4 className="text-white font-medium">Movement Patterns</h4>
              <select
                value={patternFilter}
                onChange={(e) => setPatternFilter(e.target.value)}
                className="px-2 py-1 bg-gray-800 border border-gray-700 rounded text-xs text-white outline-none focus:border-purple-500"
              >
                <option value="ALL">All Patterns</option>
                <option value="STATIONARY">Stationary Only</option>
                <option value="MOVING">Moving / Walking</option>
                <option value="ERRATIC">Walkouts / Erratic</option>
              </select>
            </div>
            <p className="text-gray-400 text-sm">Real-time student behavior analysis</p>
          </CardHeader>
          <CardContent>
            <div className="space-y-3 max-h-80 overflow-y-auto pr-2 custom-scrollbar">
              {movementPatterns
                .filter(p => {
                  if (patternFilter === 'ALL') return true;
                  if (patternFilter === 'MOVING') return p.pattern === 'WALKING' || p.pattern === 'RUNNING' || p.pattern === 'MOVING';
                  return p.pattern === patternFilter;
                })
                .map((pattern) => (
                <div
                  key={pattern.studentId}
                  className="p-3 bg-gray-800/30 rounded-lg border border-gray-700 hover:border-gray-600 transition-colors cursor-pointer"
                  onClick={() => setSelectedStudent(pattern.studentId)}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      {getPatternIcon(pattern.pattern)}
                      <span className="text-white font-medium">{pattern.studentName}</span>
                    </div>
                    <div className={`px-2 py-1 rounded-full text-xs font-medium border ${getPatternColor(pattern.pattern)}`}>
                      {pattern.pattern}
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-3 gap-2 text-xs">
                    <div>
                      <span className="text-gray-400">Speed:</span>
                      <span className="text-white ml-1">{pattern.speed.toFixed(1)} m/s</span>
                    </div>
                    <div>
                      <span className="text-gray-400">Distance:</span>
                      <span className="text-white ml-1">{pattern.distance}m</span>
                    </div>
                    <div>
                      <span className="text-gray-400">Confidence:</span>
                      <span className="text-white ml-1">{Math.round(pattern.confidence * 100)}%</span>
                    </div>
                  </div>
                  
                  {pattern.anomalies > 0 && (
                    <div className="mt-2 flex items-center space-x-1">
                      <AlertTriangle className="h-3 w-3 text-yellow-400" />
                      <span className="text-yellow-400 text-xs">{pattern.anomalies} anomalies detected</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* AI Alerts */}
      <Card glass>
        <CardHeader>
          <div className="flex items-center justify-between">
            <h4 className="text-white font-medium">AI Alerts</h4>
            <span className="text-gray-400 text-sm">
              {alerts.length} active alerts
            </span>
          </div>
        </CardHeader>
        <CardContent>
          {alerts.length === 0 ? (
            <div className="text-center py-8">
              <Eye className="h-12 w-12 text-gray-500 mx-auto mb-4" />
              <p className="text-gray-400">No anomalies detected</p>
            </div>
          ) : (
            <div className="space-y-3 max-h-[500px] overflow-y-auto pr-2 custom-scrollbar">
              {alerts.map((alert) => (
                <div
                  key={alert.id}
                  className={`p-4 rounded-lg border ${getAlertSeverityColor(alert.severity)}`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-2">
                        <AlertTriangle className="h-4 w-4" />
                        <span className="font-medium">{alert.type.replace('_', ' ')}</span>
                        <span className="text-xs opacity-75">
                          {new Date(alert.timestamp).toLocaleTimeString()}
                        </span>
                      </div>
                      <p className="text-sm opacity-90">{alert.message}</p>
                      <p className="text-xs mt-1 opacity-75">
                        Student ID: {alert.studentId} • Confidence: {Math.round(alert.confidence * 100)}%
                      </p>
                    </div>
                    
                    {!alert.acknowledged && (
                      <Button
                        variant="glass"
                        size="sm"
                        onClick={() => {
                          setAlerts(prev => prev.map(a => a.id === alert.id ? { ...a, acknowledged: true } : a));
                        }}
                      >
                        Acknowledge
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default SpatialBehaviorDashboard;
