'use client';

import React, { useState } from 'react';
import { useAuth } from '@/stores/authContext';

import { SessionManagement } from '@/components/attendance/SessionManagement';
import { LiveAttendanceDashboard } from '@/components/attendance/LiveAttendanceDashboard';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Activity, Users, Calendar, Clock, Eye } from 'lucide-react';
import { AttendanceSession } from '@/types/attendance';
import { Loading } from '@/components/ui/Loading';

type ViewMode = 'sessions' | 'live' | 'history';

function AttendanceContent() {
  const { user, getUserRole, isLoading } = useAuth();
  const userRole = getUserRole();
  const [viewMode, setViewMode] = useState<ViewMode>('sessions');
  const [selectedSession, setSelectedSession] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loading size="lg" text="Authenticating..." />
      </div>
    );
  }

  const handleSessionCreated = (session: AttendanceSession) => {
    console.log('Session created:', session);
  };

  const renderContent = () => {
    switch (viewMode) {
      case 'live':
        return (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold gradient-text flex items-center">
                  <Activity className="h-6 w-6 mr-3" />
                  Live Attendance
                </h2>
                <p className="text-gray-400">
                  Real-time attendance monitoring
                </p>
              </div>
              
              {selectedSession && (
                <Button
                  variant="glass"
                  onClick={() => setViewMode('sessions')}
                >
                  ← Back to Sessions
                </Button>
              )}
            </div>

            {selectedSession ? (
              <LiveAttendanceDashboard
                sessionId={selectedSession}
                facultyId={user?.id || ''}
              />
            ) : (
              <Card glass>
                <CardContent className="text-center py-12">
                  <Activity className="h-16 w-16 text-gray-500 mx-auto mb-4" />
                  <h4 className="text-xl font-semibold text-white mb-2">No Session Selected</h4>
                  <p className="text-gray-400 mb-4">
                    Select a session to view live attendance
                  </p>
                  <Button
                    variant="primary"
                    onClick={() => setViewMode('sessions')}
                  >
                    <Eye className="h-4 w-4 mr-2" />
                    View Sessions
                  </Button>
                </CardContent>
              </Card>
            )}
          </div>
        );

      case 'history':
        return (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold gradient-text flex items-center">
                  <Calendar className="h-6 w-6 mr-3" />
                  Attendance History
                </h2>
                <p className="text-gray-400">
                  View past attendance records
                </p>
              </div>
            </div>

            <Card glass>
              <CardContent className="text-center py-12">
                <Calendar className="h-16 w-16 text-gray-500 mx-auto mb-4" />
                <h4 className="text-xl font-semibold text-white mb-2">Coming Soon</h4>
                <p className="text-gray-400">
                  Attendance history and analytics will be available soon
                </p>
              </CardContent>
            </Card>
          </div>
        );

      default:
        return (
          <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-bold gradient-text flex items-center">
                  <Activity className="h-6 w-6 mr-3" />
                  Attendance Management
                </h1>
                <p className="text-gray-400">
                  Manage attendance sessions and monitor real-time data
                </p>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <Card glass>
                <CardContent className="p-6">
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-blue-500/20 rounded-lg">
                      <Clock className="h-5 w-5 text-blue-400" />
                    </div>
                    <div>
                      <h3 className="text-white font-medium">Active Sessions</h3>
                      <p className="text-gray-400 text-sm">Manage current sessions</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card glass>
                <CardContent className="p-6">
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-green-500/20 rounded-lg">
                      <Activity className="h-5 w-5 text-green-400" />
                    </div>
                    <div>
                      <h3 className="text-white font-medium">Live Monitoring</h3>
                      <p className="text-gray-400 text-sm">Real-time tracking</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card glass>
                <CardContent className="p-6">
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-purple-500/20 rounded-lg">
                      <Calendar className="h-5 w-5 text-purple-400" />
                    </div>
                    <div>
                      <h3 className="text-white font-medium">History</h3>
                      <p className="text-gray-400 text-sm">Past records</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Session Management */}
            <SessionManagement
              facultyId={user?.id || ''}
              onSessionCreated={handleSessionCreated}
            />
          </div>
        );
    }
  };

  // Role-based access control
  if (!userRole || !['ADMIN', 'SUPER_ADMIN', 'FACULTY', 'STUDENT'].includes(userRole)) {
    return (
      <div className="text-center py-12">
        <Activity className="h-16 w-16 text-gray-500 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-white mb-2">Access Denied</h2>
        <p className="text-gray-400">
          You don't have permission to access attendance management.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Navigation Tabs */}
      <div className="flex space-x-1 p-1 bg-gray-800/30 rounded-lg border border-gray-700">
        <Button
          variant={viewMode === 'sessions' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('sessions')}
          className="flex-1"
        >
          <Clock className="h-4 w-4 mr-2" />
          Sessions
        </Button>
        
        <Button
          variant={viewMode === 'live' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('live')}
          className="flex-1"
        >
          <Activity className="h-4 w-4 mr-2" />
          Live
        </Button>
        
        <Button
          variant={viewMode === 'history' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setViewMode('history')}
          className="flex-1"
        >
          <Calendar className="h-4 w-4 mr-2" />
          History
        </Button>
      </div>

      {/* Content */}
      {renderContent()}
    </div>
  );
}

export default function AttendancePage() {
  return <AttendanceContent />;
}

