'use client';

import React, { useState } from 'react';
import { useAuth } from '@/stores/authContext';
import dynamic from 'next/dynamic';
const AIAnalyticsDashboard = dynamic(
  () => import('@/components/ai/AIAnalyticsDashboard').then(mod => mod.AIAnalyticsDashboard),
  { 
    ssr: false, 
    loading: () => <div className="h-[400px] w-full bg-white/5 animate-pulse rounded-xl flex items-center justify-center text-gray-500">Loading AI Insights...</div>
  }
);
import { aiAnalyticsService } from '@/services/aiAnalytics.service';
import { userManagementService } from '@/services/userManagement.service';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Brain, Activity, AlertTriangle, BarChart3, Search, Users, ShieldAlert, UserMinus, X, User } from 'lucide-react';
import { Loading } from '@/components/ui/Loading';
import { DropdownDTO } from '@/types/user-management';

function AnalyticsContent() {
  const { hasRole, isLoading } = useAuth();
  const [selectedDepartment, setSelectedDepartment] = useState<string>('');
  const [selectedSection, setSelectedSection] = useState<string>('');
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [deptSearchTerm, setDeptSearchTerm] = useState<string>('');
  const [stats, setStats] = useState<{
    accuracy: number;
    absentCount: number;
    predictionsToday: number;
    activeModels: number;
    totalStudents: number;
    liveVerifications: number;
    anomaliesDetected: number;
  }>({
    accuracy: 94.2,
    absentCount: 0,
    predictionsToday: 0,
    activeModels: 0,
    totalStudents: 0,
    liveVerifications: 0,
    anomaliesDetected: 0
  });

  const [showAnomalyModal, setShowAnomalyModal] = useState(false);
  const [anomalyList, setAnomalyList] = useState<any[]>([]);

  // Fetch Departments on Mount
  React.useEffect(() => {
    const fetchDepts = async () => {
      try {
        const depts = await userManagementService.getDepartments();
        setDepartments(depts);
      } catch (error) {
        console.error('Failed to fetch departments:', error);
      }
    };
    fetchDepts();
  }, []);

  // Fetch Sections when Department changes
  React.useEffect(() => {
    const fetchSections = async () => {
      if (!selectedDepartment) {
        setSections([]);
        setSelectedSection('');
        return;
      }
      try {
        const data = await userManagementService.getSections(selectedDepartment);
        setSections(data);
      } catch (error) {
        console.error('Failed to fetch sections:', error);
      }
    };
    fetchSections();
  }, [selectedDepartment]);

  React.useEffect(() => {
    const fetchQuickStats = async () => {
      try {
        const [dashboard, metrics, alerts] = await Promise.all([
          aiAnalyticsService.getAnalyticsDashboard(selectedDepartment, selectedSection),
          aiAnalyticsService.getModelMetrics(),
          aiAnalyticsService.getActiveAlerts(selectedDepartment, selectedSection)
        ]);

        setStats({
          accuracy: metrics?.accuracy ? Math.round(metrics.accuracy * 1000) / 10 : 94.2,
          absentCount: dashboard?.totalAbsences || 0,
          predictionsToday: dashboard?.totalPredictions || 0,
          activeModels: dashboard?.activeSessions?.length || 0,
          totalStudents: dashboard?.totalStudents || 0,
          liveVerifications: dashboard?.liveVerifications || 0,
          anomaliesDetected: dashboard?.anomaliesDetected || 0
        });
        setAnomalyList(alerts || []);
      } catch (error) {
        console.error('Failed to fetch AI quick stats:', error);
      }
    };

    fetchQuickStats();
    const interval = setInterval(fetchQuickStats, 60000); // Refresh every minute
    return () => clearInterval(interval);
  }, [selectedDepartment, selectedSection]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loading size="lg" text="Authenticating..." />
      </div>
    );
  }

  // Only ADMIN and SUPER_ADMIN can access AI analytics
  if (!hasRole(['ADMIN', 'SUPER_ADMIN'])) {
    return (
      <div className="text-center py-12">
        <Brain className="h-16 w-16 text-gray-500 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-white mb-2">Access Denied</h2>
        <p className="text-gray-400">
          You don't have permission to access AI analytics.
        </p>
      </div>
    );
  }

  // Filter departments for search
  const filteredDepartments = departments.filter(dept => 
    dept.label.toLowerCase().includes(deptSearchTerm.toLowerCase()) ||
    dept.code?.toLowerCase().includes(deptSearchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold gradient-text flex items-center">
            <Brain className="h-6 w-6 mr-3" />
            AI Analytics
          </h1>
          <p className="text-gray-400">
            Advanced machine learning insights and predictive analytics
          </p>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6">
        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-blue-500/20 rounded-lg">
                <Users className="h-5 w-5 text-blue-400" />
              </div>
              <span className="text-2xl font-bold text-white">{stats.totalStudents}</span>
            </div>
            <p className="text-gray-400 text-sm">Total Students</p>
            <div className="mt-2 text-xs text-blue-400">
              Registered in system
            </div>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-green-500/20 rounded-lg">
                <Activity className="h-5 w-5 text-green-400" />
              </div>
              <span className="text-2xl font-bold text-white">{stats.liveVerifications.toLocaleString()}</span>
            </div>
            <p className="text-gray-400 text-sm">Live Verifications</p>
            <div className="mt-2 text-xs text-green-400">
              Present & Late Verified
            </div>
          </CardContent>
        </Card>

        <Card glass 
          className="cursor-pointer hover:border-red-500/50 transition-colors"
          onClick={() => setShowAnomalyModal(true)}
        >
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-red-500/20 rounded-lg">
                <ShieldAlert className="h-5 w-5 text-red-400" />
              </div>
              <span className="text-2xl font-bold text-white">{stats.anomaliesDetected}</span>
            </div>
            <p className="text-gray-400 text-sm">Security Anomalies</p>
            <div className="mt-2 text-xs text-red-400">
              Fraud & Spoofing List
            </div>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-purple-500/20 rounded-lg">
                <Brain className="h-5 w-5 text-purple-400" />
              </div>
              <span className="text-2xl font-bold text-white">{stats.predictionsToday.toLocaleString()}</span>
            </div>
            <p className="text-gray-400 text-sm">Predictions Today</p>
            <div className="mt-2 text-xs text-purple-400">
              AI behavioral analysis
            </div>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-orange-500/20 rounded-lg">
                <UserMinus className="h-5 w-5 text-orange-400" />
              </div>
              <span className="text-2xl font-bold text-white">{stats.absentCount}</span>
            </div>
            <p className="text-gray-400 text-sm">Absent Count</p>
            <div className="mt-2 text-xs text-orange-400">
              No-shows & Security Failures
            </div>
          </CardContent>
        </Card>
      </div>


      {/* Filters */}
      <Card glass>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 items-end">
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-300">
                Department Search
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-500" />
                <input
                  type="text"
                  placeholder="Search departments..."
                  value={deptSearchTerm}
                  onChange={(e) => setDeptSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-purple-500 focus:ring-2 focus:ring-purple-500/20 outline-none"
                />
              </div>
              <select
                value={selectedDepartment}
                onChange={(e) => setSelectedDepartment(e.target.value)}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-purple-500 focus:ring-2 focus:ring-purple-500/20 outline-none"
              >
                <option value="">All Departments {filteredDepartments.length < departments.length ? `(${filteredDepartments.length} found)` : ""}</option>
                {filteredDepartments.map(dept => (
                  <option key={dept.id} value={dept.id}>
                    {dept.label} {dept.code ? `(${dept.code})` : ""}
                  </option>
                ))}
              </select>
            </div>
            
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-300">
                Section
              </label>
              <select
                value={selectedSection}
                onChange={(e) => setSelectedSection(e.target.value)}
                disabled={!selectedDepartment}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-purple-500 focus:ring-2 focus:ring-purple-500/20 outline-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <option value="">{selectedDepartment ? 'All Sections' : 'Select Department First'}</option>
                {sections.map(sec => (
                  <option key={sec.id} value={sec.id}>
                    {sec.label}
                  </option>
                ))}
              </select>
            </div>
            
            <div className="flex space-x-3">
              <Button
                variant="glass"
                className="w-full"
                onClick={() => {
                  setSelectedDepartment('');
                  setSelectedSection('');
                  setDeptSearchTerm('');
                }}
              >
                Reset Filters
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* AI Analytics Dashboard */}
      <AIAnalyticsDashboard
        departmentId={selectedDepartment || undefined}
        sectionId={selectedSection || undefined}
      />

      {/* Security Anomalies Modal - Fraud List */}
      {showAnomalyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <Card className="w-full max-w-2xl bg-gray-900 border-gray-800 shadow-2xl animate-in zoom-in duration-200">
            <CardContent className="p-0">
              <div className="flex items-center justify-between p-6 border-b border-gray-800">
                <div className="flex items-center space-x-3 text-red-400">
                  <ShieldAlert className="h-6 w-6" />
                  <h2 className="text-xl font-bold text-white">Security Fraud Audit</h2>
                </div>
                <button 
                  onClick={() => setShowAnomalyModal(false)}
                  className="p-2 hover:bg-gray-800 rounded-full transition-colors"
                >
                  <X className="h-5 w-5 text-gray-400" />
                </button>
              </div>
              
              <div className="p-6">
                <p className="text-gray-400 mb-6 text-sm">
                  The following students have been flagged for hardware mismatch, biometric verification failure, or unauthorized spatial behavior.
                </p>
                
                <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                  {anomalyList.length === 0 ? (
                    <div className="text-center py-12 bg-gray-800/50 rounded-xl border border-dashed border-gray-700">
                      <ShieldAlert className="h-12 w-12 text-gray-600 mx-auto mb-3 opacity-20" />
                      <p className="text-gray-500">No active security anomalies detected.</p>
                    </div>
                  ) : (
                    anomalyList.map((alert) => (
                      <div key={alert.id} className="flex items-center justify-between p-4 bg-red-500/5 border border-red-500/20 rounded-xl group hover:bg-red-500/10 transition-all">
                        <div className="flex items-center space-x-4">
                          <div className="h-10 w-10 bg-red-500/20 rounded-full flex items-center justify-center">
                            <User className="h-5 w-5 text-red-400" />
                          </div>
                          <div>
                            <h4 className="font-semibold text-white">{alert.studentName || alert.studentId || 'System'}</h4>
                            <div className="flex items-center space-x-2 text-xs text-gray-400">
                              <span className="bg-gray-800 px-2 py-0.5 rounded text-gray-300">
                                {alert.registrationNumber || 'N/A'}
                              </span>
                              <span>•</span>
                              <span className="text-red-400/80">{alert.type}</span>
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-xs font-medium text-red-400 mb-1">
                            {Math.round((alert.confidence || 0.95) * 100)}% Confidence
                          </div>
                          <div className="text-[10px] text-gray-500">
                            {new Date(alert.timestamp).toLocaleTimeString()}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
              
              <div className="p-6 bg-gray-800/50 border-t border-gray-800 rounded-b-xl flex justify-end">
                <Button variant="glass" onClick={() => setShowAnomalyModal(false)}>
                  Close Audit
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

export default function AnalyticsPage() {
  return <AnalyticsContent />;
}

