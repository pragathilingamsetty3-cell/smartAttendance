'use client';

import React, { useState, useEffect } from 'react';
import { Brain, Activity, AlertTriangle, TrendingUp, Users, Map, Target, Zap, Eye, Settings, BarChart3, PieChart, Bot, MapPin, UserX } from 'lucide-react';
import { aiAnalyticsService } from '@/services/aiAnalytics.service';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { AIAnalyticsDashboard as AIAnalyticsDashboardType, AIModelMetrics, AIAlert } from '@/types/ai-analytics';
import { AnalyticsDataDTO } from '@/types/index';
import apiClient from '@/lib/apiClient';
import { AnimatedCounter } from '@/components/ui/AnimatedCounter';
import { AttendanceTrend } from '@/components/analytics/AttendanceTrend';
import { AnomalyChart } from '@/components/analytics/AnomalyChart';
import { PulseSkeleton } from '@/components/analytics/PulseSkeleton';
import SpatialBehaviorDashboard from './SpatialBehaviorDashboard';
import GPSDriftDetection from './GPSDriftDetection';
import WalkOutPrediction from './WalkOutPrediction';
import WeeklyAIInsights from './WeeklyAIInsights';
import AIAssistantChat from './AIAssistantChat';

const getFallbackAnalyticsData = (): AnalyticsDataDTO => ({
  totalStudents: 1250,
  liveVerifications: 842,
  securityAnomalies: 3,
  totalAbsences: 405,
  velocityTrend: [
    { time: '08:00', value: 120 }, { time: '09:00', value: 340 },
    { time: '10:00', value: 310 }, { time: '11:00', value: 450 },
    { time: '12:00', value: 410 }, { time: '13:00', value: 500 }
  ],
  anomalyBreakdown: [
    { type: 'Geofence Breach', count: 12 },
    { type: 'Device Spoofing', count: 4 },
    { type: 'Time Anomaly', count: 7 },
    { type: 'Biometric Mismatch', count: 2 }
  ]
});

type ViewMode = 'overview' | 'spatial' | 'gps' | 'walkout' | 'profiles' | 'models' | 'assistant';

interface AIAnalyticsDashboardProps {
  departmentId?: string;
  sectionId?: string;
}

export const AIAnalyticsDashboard: React.FC<AIAnalyticsDashboardProps> = ({
  departmentId,
  sectionId
}) => {
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>('overview');
  const [dashboardData, setDashboardData] = useState<AIAnalyticsDashboardType | null>(null);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsDataDTO | null>(null);
  const [modelMetrics, setModelMetrics] = useState<AIModelMetrics | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);
  const [alerts, setAlerts] = useState<AIAlert[]>([]);
  const [selectedSession, setSelectedSession] = useState<string | null>(null);
  const [initialLoad, setInitialLoad] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [showSettings, setShowSettings] = useState(false);

  useEffect(() => {
    fetchDashboardData();
    if (autoRefresh) {
      const interval = setInterval(fetchDashboardData, 30000); // 30s
      return () => clearInterval(interval);
    }
  }, [departmentId, sectionId, autoRefresh]);

  // Sync selected student when session changes
  useEffect(() => {
    const syncSessionStudents = async () => {
      if (selectedSession) {
        try {
          const students = await aiAnalyticsService.getSpatialBehavior(selectedSession);
          if (students && students.length > 0) {
            setSelectedStudentId(students[0].studentId);
          }
        } catch (e) {
          console.error("Failed to sync session students:", e);
        }
      }
    };
    syncSessionStudents();
  }, [selectedSession]);

  const fetchDashboardData = async () => {
    try {
      if (initialLoad) setLoading(true);
      
      const dashboard = await aiAnalyticsService.getAnalyticsDashboard(departmentId, sectionId).catch(() => null);
      
      if (dashboard) {
        setDashboardData(dashboard);
        setAnalyticsData({
          totalStudents: dashboard.totalStudents || 0,
          liveVerifications: dashboard.liveVerifications || 0,
          securityAnomalies: dashboard.anomaliesDetected || 0,
          totalAbsences: dashboard.totalAbsences || 0,
          velocityTrend: dashboard.velocityTrend || [
            { time: '08:00', value: 0 }, { time: '10:00', value: dashboard.liveVerifications || 0 },
            { time: '12:00', value: 0 }, { time: '14:00', value: 0 }
          ],
          anomalyBreakdown: dashboard.anomalyBreakdown || [{ type: 'Security', count: dashboard.anomaliesDetected || 0 }]
        });
        
        // Macroscopic View Strategy
        if (!departmentId) {
          setSelectedSession(null);
        }
      }
      
      const metrics = await aiAnalyticsService.getModelMetrics().catch(() => null);
      if (metrics) setModelMetrics(metrics);
      
      const alertData = await aiAnalyticsService.getActiveAlerts(departmentId, sectionId).catch(() => []);
      setAlerts(alertData);
    } catch (error) {
      console.error('Failed to fetch AI dashboard data:', error);
    } finally {
      if (initialLoad) {
        setLoading(false);
        setInitialLoad(false);
      }
    }
  };

  const retrainModel = async (modelType: 'ATTENDANCE_PREDICTION' | 'WALK_OUT_DETECTION' | 'BEHAVIOR_ANALYSIS') => {
    try {
      // Backend handles autonomous retraining; UI mock only
      await new Promise(resolve => setTimeout(resolve, 1500));
      await fetchDashboardData();
    } catch (error) {
      console.error('Model retraining failed:', error);
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

  const getModelStatusColor = (accuracy: number) => {
    if (accuracy >= 0.9) return 'text-green-400';
    if (accuracy >= 0.8) return 'text-blue-400';
    if (accuracy >= 0.7) return 'text-yellow-400';
    return 'text-red-400';
  };

  if (loading) {
    return (
      <div className="py-12">
        <PulseSkeleton />
      </div>
    );
  }

  const renderContent = () => {
    switch (viewMode) {
      case 'spatial':
        return (
          <SpatialBehaviorDashboard
            sessionId={selectedSession || ''}
            departmentId={departmentId}
            sectionId={sectionId}
          />
        );

      case 'gps':
        return (
          <GPSDriftDetection
            studentId={selectedStudentId}
            sessionId={selectedSession}
            departmentId={departmentId}
            sectionId={sectionId}
          />
        );

      case 'walkout':
        if (!selectedSession) {
          return (
             <Card glass>
              <CardContent className="text-center py-24">
                <Brain className="h-16 w-16 text-gray-600 mx-auto mb-4" />
                <h3 className="text-xl font-medium text-white mb-2">Walk-out Prediction System</h3>
                <p className="text-gray-400">Select an active session to begin predictive risk analysis.</p>
              </CardContent>
            </Card>
          );
        }
        return (
          <WalkOutPrediction
            sessionId={selectedSession}
            departmentId={departmentId}
            sectionId={sectionId}
          />
        );

      case 'profiles':
        return (
          <Card glass>
            <CardContent className="text-center py-12">
              <Users className="h-16 w-16 text-gray-500 mx-auto mb-4" />
              <h4 className="text-xl font-semibold text-white mb-2">Student AI Profiles</h4>
              <p className="text-gray-400">
                Individual student behavior analysis coming soon
              </p>
            </CardContent>
          </Card>
        );

      case 'models':
        return (
          <Card glass>
            <CardContent className="text-center py-12">
              <BarChart3 className="h-16 w-16 text-gray-500 mx-auto mb-4" />
              <h4 className="text-xl font-semibold text-white mb-2">AI Model Performance</h4>
              <p className="text-gray-400">
                Model metrics and performance monitoring coming soon
              </p>
            </CardContent>
          </Card>
        );

      case 'assistant':
        return (
          <AIAssistantChat />
        );

      default:
        return (
          <div className="space-y-6">
            {/* AI Weekly Insights */}
            <WeeklyAIInsights />

            {/* Overview Header */}
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold gradient-text flex items-center">
                  <Brain className="h-6 w-6 mr-3" />
                  AI Analytics Overview
                </h2>
                <p className="text-gray-400">
                  Real-time AI insights and predictive analytics
                </p>
              </div>

              <div className="flex items-center space-x-3">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={autoRefresh}
                    onChange={(e) => setAutoRefresh(e.target.checked)}
                    className="rounded border-gray-600 bg-gray-800 text-purple-500"
                  />
                  <span className="text-sm text-gray-300">Auto Refresh</span>
                </label>
                
                <Button
                  variant="glass"
                  onClick={() => setShowSettings(!showSettings)}
                >
                  <Settings className="h-4 w-4" />
                </Button>
              </div>
            </div>

            {/* Real-Time KPI Grid & Advanced Charts under Suspense */}
            <React.Suspense fallback={<PulseSkeleton />}>
              {analyticsData && (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <Card glass className="border-blue-500/20">
                      <CardContent className="p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div className="p-2 bg-blue-500/20 rounded-lg">
                            <Users className="h-5 w-5 text-blue-400" />
                          </div>
                          <span className="text-3xl font-bold text-white">
                            <AnimatedCounter value={analyticsData.totalStudents} />
                          </span>
                        </div>
                        <p className="text-gray-400 text-sm font-medium">Total Students</p>
                      </CardContent>
                    </Card>

                    <Card glass className="border-emerald-500/20">
                      <CardContent className="p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div className="p-2 bg-emerald-500/20 rounded-lg">
                            <Activity className="h-5 w-5 text-emerald-400" />
                          </div>
                          <span className="text-3xl font-bold text-white">
                            <AnimatedCounter value={analyticsData.liveVerifications} />
                          </span>
                        </div>
                        <p className="text-gray-400 text-sm font-medium">Live Verifications</p>
                      </CardContent>
                    </Card>

                    <Card glass className="border-red-500/20">
                      <CardContent className="p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div className="p-2 bg-red-500/20 rounded-lg">
                            <UserX className="h-5 w-5 text-red-400" />
                          </div>
                          <span className="text-3xl font-bold text-white">
                            <AnimatedCounter value={analyticsData.totalAbsences} />
                          </span>
                        </div>
                        <p className="text-gray-400 text-sm font-medium">Absentees</p>
                      </CardContent>
                    </Card>

                    <Card glass className="border-[#ff007a]/20">
                      <CardContent className="p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div className="p-2 bg-[#ff007a]/20 rounded-lg shadow-[0_0_10px_rgba(255,0,122,0.5)]">
                            <AlertTriangle className="h-5 w-5 text-[#ff007a]" />
                          </div>
                          <span className="text-3xl font-bold text-[#ff007a] drop-shadow-[0_0_8px_rgba(255,0,122,0.8)]">
                            <AnimatedCounter value={analyticsData.securityAnomalies} />
                          </span>
                        </div>
                        <p className="text-gray-400 text-sm font-medium">Security Anomalies</p>
                      </CardContent>
                    </Card>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    <div className="min-h-[350px] w-full">
                      <AttendanceTrend data={analyticsData.velocityTrend} />
                    </div>
                    <div className="min-h-[350px] w-full">
                      <AnomalyChart data={analyticsData.anomalyBreakdown} />
                    </div>
                  </div>
                </>
              )}
            </React.Suspense>

            {/* AI Features Status */}
            <Card glass>
              <CardHeader>
                <h4 className="text-white font-medium">AI Features Status</h4>
                <p className="text-gray-400 text-sm">Real-time AI processing capabilities</p>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                    <div className="flex items-center space-x-3 mb-2">
                      <Map className="h-5 w-5 text-green-400" />
                      <span className="text-white font-medium">Spatial Analysis</span>
                    </div>
                    <p className="text-sm text-green-400">Active</p>
                    <p className="text-xs text-gray-400 mt-1">Real-time behavior tracking</p>
                  </div>

                  <div className="p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                    <div className="flex items-center space-x-3 mb-2">
                      <Target className="h-5 w-5 text-green-400" />
                      <span className="text-white font-medium">GPS Drift Detection</span>
                    </div>
                    <p className="text-sm text-green-400">Active</p>
                    <p className="text-xs text-gray-400 mt-1">Accuracy monitoring</p>
                  </div>

                  <div className="p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                    <div className="flex items-center space-x-3 mb-2">
                      <Brain className="h-5 w-5 text-green-400" />
                      <span className="text-white font-medium">Walk-Out Prediction</span>
                    </div>
                    <p className="text-sm text-green-400">Active</p>
                    <p className="text-xs text-gray-400 mt-1">Early warning system</p>
                  </div>

                  <div className="p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                    <div className="flex items-center space-x-3 mb-2">
                      <Activity className="h-5 w-5 text-yellow-400" />
                      <span className="text-white font-medium">Anomaly Detection</span>
                    </div>
                    <p className="text-sm text-yellow-400">Learning</p>
                    <p className="text-xs text-gray-400 mt-1">Pattern recognition</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Model Performance */}
            {modelMetrics && (
              <Card glass>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <h4 className="text-white font-medium">AI Model Performance</h4>
                    <div className="flex space-x-2">
                      <Button
                        variant="glass"
                        size="sm"
                        onClick={() => retrainModel('ATTENDANCE_PREDICTION')}
                      >
                        <Zap className="h-3 w-3 mr-1" />
                        Retrain
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <div className="text-center">
                      <div className={`text-3xl font-bold ${getModelStatusColor(modelMetrics.accuracy)}`}>
                        {Math.round(modelMetrics.accuracy * 100)}%
                      </div>
                      <p className="text-gray-400 text-sm mt-1">Accuracy</p>
                    </div>
                    
                    <div className="text-center">
                      <div className={`text-3xl font-bold ${getModelStatusColor(modelMetrics.precision)}`}>
                        {Math.round(modelMetrics.precision * 100)}%
                      </div>
                      <p className="text-gray-400 text-sm mt-1">Precision</p>
                    </div>
                    
                    <div className="text-center">
                      <div className={`text-3xl font-bold ${getModelStatusColor(modelMetrics.recall)}`}>
                        {Math.round(modelMetrics.recall * 100)}%
                      </div>
                      <p className="text-gray-400 text-sm mt-1">Recall</p>
                    </div>
                    
                    <div className="text-center">
                      <div className={`text-3xl font-bold ${getModelStatusColor(modelMetrics.f1Score)}`}>
                        {Math.round(modelMetrics.f1Score * 100)}%
                      </div>
                      <p className="text-gray-400 text-sm mt-1">F1 Score</p>
                    </div>
                  </div>
                  
                  <div className="mt-4 pt-4 border-t border-gray-700">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-400">Model: {modelMetrics.modelName}</span>
                      <span className="text-gray-400">Version: {modelMetrics.version}</span>
                      <span className="text-gray-400">
                        {modelMetrics.totalPredictions?.toLocaleString() || '0'} predictions
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Recent Alerts */}
            <Card glass>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h4 className="text-white font-medium">Recent AI Alerts</h4>
                  <span className="text-gray-400 text-sm">
                    {alerts.length} active alerts
                  </span>
                </div>
              </CardHeader>
              <CardContent>
                {alerts.length === 0 ? (
                  <div className="text-center py-8">
                    <Eye className="h-12 w-12 text-gray-500 mx-auto mb-4" />
                    <p className="text-gray-400">No active alerts</p>
                  </div>
                ) : (
                  <div className="space-y-3 max-h-[450px] overflow-y-auto pr-2 custom-scrollbar">
                    {alerts.map((alert) => (
                      <div
                        key={alert.id}
                        className={`p-3 rounded-lg border ${getAlertSeverityColor(alert.severity)}`}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center space-x-2 mb-1">
                              <AlertTriangle className="h-4 w-4" />
                              <span className="font-medium">{alert.type.replace('_', ' ')}</span>
                              <span className="text-xs opacity-75">
                                {alert.timestamp ? new Date(alert.timestamp).toLocaleTimeString() : 'Unknown'}
                              </span>
                            </div>
                            <p className="text-sm opacity-90">{alert.message}</p>
                            <p className="text-xs opacity-75 mt-1">
                              Student: {alert.studentId} • Confidence: {Math.round(alert.confidence * 100)}%
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
    }
  };

  return (
    <div className="space-y-6">
      {/* Navigation Tabs */}
      <div className="flex space-x-1 p-1 bg-gray-800/30 rounded-lg border border-gray-700">
        <Button
          variant={viewMode === 'overview' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('overview')}
          className="flex-1"
        >
          <BarChart3 className="h-4 w-4 mr-2" />
          Overview
        </Button>
        
        <Button
          variant={viewMode === 'spatial' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('spatial')}
          className="flex-1"
        >
          <Map className="h-4 w-4 mr-2" />
          Spatial
        </Button>
        
        <Button
          variant={viewMode === 'gps' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('gps')}
          className="flex-1"
        >
          <Target className="h-4 w-4 mr-2" />
          GPS
        </Button>
        
        <Button
          variant={viewMode === 'walkout' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('walkout')}
          className="flex-1"
        >
          <Brain className="h-4 w-4 mr-2" />
          Walk-Out
        </Button>
        
        <Button
          variant={viewMode === 'profiles' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('profiles')}
          className="flex-1"
        >
          <Users className="h-4 w-4 mr-2" />
          Profiles
        </Button>
        
        <Button
          variant={viewMode === 'models' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('models')}
          className="flex-1"
        >
          <Activity className="h-4 w-4 mr-2" />
          Models
        </Button>

        <Button
          variant={viewMode === 'assistant' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('assistant')}
          className="flex-1"
        >
          <Bot className="h-4 w-4 mr-2" />
          AI Assistant
        </Button>
      </div>

      {/* Content */}
      {renderContent()}
    </div>
  );
};

export default AIAnalyticsDashboard;
