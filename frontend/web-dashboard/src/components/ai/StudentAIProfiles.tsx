'use client';

import React, { useState, useEffect } from 'react';
import { User, Brain, Activity, TrendingUp, AlertTriangle, Eye, Settings, Target, Clock, MapPin, CheckCircle } from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { StudentAIProfile } from '@/types/ai-analytics';

interface StudentAIProfilesProps {
  departmentId?: string;
  sectionId?: string;
}

interface BehaviorPattern {
  type: string;
  frequency: number;
  confidence: number;
  contexts: string[];
  trend: 'IMPROVING' | 'DECLINING' | 'STABLE';
}

interface AnomalyRecord {
  type: string;
  frequency: number;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  description: string;
  lastOccurrence: string;
}

interface AttendancePattern {
  dayOfWeek: string;
  timeSlot: string;
  attendanceProbability: number;
  variance: number;
  factors: string[];
}

export const StudentAIProfiles: React.FC<StudentAIProfilesProps> = ({
  departmentId,
  sectionId
}) => {
  const [loading, setLoading] = useState(true);
  const [profiles, setProfiles] = useState<StudentAIProfile[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<StudentAIProfile | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'patterns' | 'anomalies' | 'insights'>('overview');
  const [showDetails, setShowDetails] = useState(false);

  useEffect(() => {
    fetchStudentProfiles();
  }, [departmentId, sectionId]);

  const fetchStudentProfiles = async () => {
    try {
      setLoading(true);
      
      // Simulate fetching student AI profiles
      const mockProfiles: StudentAIProfile[] = generateMockProfiles();
      setProfiles(mockProfiles);
    } catch (error) {
      console.error('Failed to fetch student AI profiles:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateMockProfiles = (): StudentAIProfile[] => {
    return [
      {
        studentId: '1',
        behaviorPattern: 'CONSISTENT',
        attendanceReliability: 0.95,
        movementPatterns: {
          typicalPathStability: 0.88,
          deviationFrequency: 0.12,
          riskScore: 0.15
        },
        predictionAccuracy: {
          walkOutPrediction: 0.92,
          attendancePrediction: 0.95,
          anomalyDetection: 0.89
        },
        lastAnalyzed: new Date().toISOString()
      },
      {
        studentId: '2',
        behaviorPattern: 'ERRATIC',
        attendanceReliability: 0.78,
        movementPatterns: {
          typicalPathStability: 0.65,
          deviationFrequency: 0.35,
          riskScore: 0.42
        },
        predictionAccuracy: {
          walkOutPrediction: 0.85,
          attendancePrediction: 0.78,
          anomalyDetection: 0.82
        },
        lastAnalyzed: new Date().toISOString()
      },
      {
        studentId: '3',
        behaviorPattern: 'SUSPICIOUS',
        attendanceReliability: 0.65,
        movementPatterns: {
          typicalPathStability: 0.45,
          deviationFrequency: 0.55,
          riskScore: 0.78
        },
        predictionAccuracy: {
          walkOutPrediction: 0.91,
          attendancePrediction: 0.65,
          anomalyDetection: 0.94
        },
        lastAnalyzed: new Date().toISOString()
      },
      {
        studentId: '4',
        behaviorPattern: 'CONSISTENT',
        attendanceReliability: 0.92,
        movementPatterns: {
          typicalPathStability: 0.91,
          deviationFrequency: 0.09,
          riskScore: 0.08
        },
        predictionAccuracy: {
          walkOutPrediction: 0.88,
          attendancePrediction: 0.92,
          anomalyDetection: 0.91
        },
        lastAnalyzed: new Date().toISOString()
      },
      {
        studentId: '5',
        behaviorPattern: 'ERRATIC',
        attendanceReliability: 0.83,
        movementPatterns: {
          typicalPathStability: 0.72,
          deviationFrequency: 0.28,
          riskScore: 0.31
        },
        predictionAccuracy: {
          walkOutPrediction: 0.87,
          attendancePrediction: 0.83,
          anomalyDetection: 0.86
        },
        lastAnalyzed: new Date().toISOString()
      }
    ];
  };

  const getBehaviorPatternColor = (pattern: string) => {
    switch (pattern) {
      case 'CONSISTENT':
        return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'ERRATIC':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'SUSPICIOUS':
        return 'bg-red-500/20 text-red-400 border-red-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  const getBehaviorPatternIcon = (pattern: string) => {
    switch (pattern) {
      case 'CONSISTENT':
        return <CheckCircle className="h-4 w-4 text-green-400" />;
      case 'ERRATIC':
        return <AlertTriangle className="h-4 w-4 text-yellow-400" />;
      case 'SUSPICIOUS':
        return <AlertTriangle className="h-4 w-4 text-red-400" />;
      default:
        return <Activity className="h-4 w-4 text-gray-400" />;
    }
  };

  const getRiskColor = (risk: number) => {
    if (risk < 0.3) return 'text-green-400';
    if (risk < 0.6) return 'text-yellow-400';
    return 'text-red-400';
  };

  const getAccuracyColor = (accuracy: number) => {
    if (accuracy >= 0.9) return 'text-green-400';
    if (accuracy >= 0.8) return 'text-blue-400';
    if (accuracy >= 0.7) return 'text-yellow-400';
    return 'text-red-400';
  };

  const generateBehaviorPatterns = (studentId: string): BehaviorPattern[] => {
    return [
      {
        type: 'PUNCTUALITY',
        frequency: 0.85,
        confidence: 0.92,
        contexts: ['Morning Classes', 'Lab Sessions'],
        trend: 'STABLE'
      },
      {
        type: 'MOVEMENT_STABILITY',
        frequency: 0.78,
        confidence: 0.88,
        contexts: ['Classroom Movement', 'Break Times'],
        trend: 'IMPROVING'
      },
      {
        type: 'SOCIAL_INTERACTION',
        frequency: 0.65,
        confidence: 0.75,
        contexts: ['Group Work', 'Discussions'],
        trend: 'DECLINING'
      }
    ];
  };

  const generateAnomalies = (studentId: string): AnomalyRecord[] => {
    return [
      {
        type: 'UNUSUAL_MOVEMENT_PATTERN',
        frequency: 3,
        severity: 'MEDIUM',
        description: 'Detected irregular movement during class time',
        lastOccurrence: new Date(Date.now() - 86400000).toISOString()
      },
      {
        type: 'ATTENDANCE_ANOMALY',
        frequency: 1,
        severity: 'LOW',
        description: 'Late arrival pattern detected',
        lastOccurrence: new Date(Date.now() - 172800000).toISOString()
      }
    ];
  };

  const generateAttendancePatterns = (studentId: string): AttendancePattern[] => {
    return [
      {
        dayOfWeek: 'Monday',
        timeSlot: '9:00 AM',
        attendanceProbability: 0.95,
        variance: 0.05,
        factors: ['Consistent Schedule', 'Motivation']
      },
      {
        dayOfWeek: 'Wednesday',
        timeSlot: '2:00 PM',
        attendanceProbability: 0.88,
        variance: 0.12,
        factors: ['Lab Session', 'Weather Dependency']
      }
    ];
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loading size="lg" text="Loading student AI profiles..." />
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
                <Brain className="h-5 w-5 text-purple-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Student AI Profiles</h3>
                <p className="text-gray-400 text-sm">
                  Individual behavior patterns and AI-driven insights
                </p>
              </div>
            </div>
            
            <div className="flex items-center space-x-3">
              <Button
                variant="glass"
                onClick={() => fetchStudentProfiles()}
              >
                <Activity className="h-4 w-4 mr-2" />
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Overview Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-green-500/20 rounded-lg">
                <User className="h-5 w-5 text-green-400" />
              </div>
              <span className="text-2xl font-bold text-white">
                {profiles.filter(p => p.behaviorPattern === 'CONSISTENT').length}
              </span>
            </div>
            <p className="text-gray-400 text-sm">Consistent Behavior</p>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-yellow-500/20 rounded-lg">
                <AlertTriangle className="h-5 w-5 text-yellow-400" />
              </div>
              <span className="text-2xl font-bold text-white">
                {profiles.filter(p => p.behaviorPattern === 'ERRATIC').length}
              </span>
            </div>
            <p className="text-gray-400 text-sm">Erratic Behavior</p>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-red-500/20 rounded-lg">
                <Target className="h-5 w-5 text-red-400" />
              </div>
              <span className="text-2xl font-bold text-white">
                {profiles.filter(p => p.behaviorPattern === 'SUSPICIOUS').length}
              </span>
            </div>
            <p className="text-gray-400 text-sm">Suspicious Behavior</p>
          </CardContent>
        </Card>

        <Card glass>
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-2 bg-blue-500/20 rounded-lg">
                <TrendingUp className="h-5 w-5 text-blue-400" />
              </div>
              <span className="text-2xl font-bold text-white">
                {Math.round(profiles.reduce((sum, p) => sum + p.attendanceReliability, 0) / profiles.length * 100)}%
              </span>
            </div>
            <p className="text-gray-400 text-sm">Avg Reliability</p>
          </CardContent>
        </Card>
      </div>

      {/* Student Profiles List */}
      <Card glass>
        <CardHeader>
          <h4 className="text-white font-medium">Student Profiles</h4>
          <p className="text-gray-400 text-sm">AI-analyzed behavior patterns and risk assessments</p>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {profiles.map((profile) => (
              <div
                key={profile.studentId}
                className="p-4 bg-gray-800/30 rounded-lg border border-gray-700 hover:border-gray-600 transition-colors cursor-pointer"
                onClick={() => setSelectedStudent(profile)}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-gray-700 rounded-lg">
                      <User className="h-4 w-4 text-gray-400" />
                    </div>
                    <div>
                      <h5 className="text-white font-medium">Student {profile.studentId}</h5>
                      <p className="text-sm text-gray-400">ID: {profile.studentId}</p>
                    </div>
                  </div>
                  
                  <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm font-medium border ${getBehaviorPatternColor(profile.behaviorPattern)}`}>
                    {getBehaviorPatternIcon(profile.behaviorPattern)}
                    <span>{profile.behaviorPattern}</span>
                  </div>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                  <div>
                    <span className="text-gray-400">Attendance Reliability:</span>
                    <p className={`font-medium ${getAccuracyColor(profile.attendanceReliability)}`}>
                      {Math.round(profile.attendanceReliability * 100)}%
                    </p>
                  </div>
                  
                  <div>
                    <span className="text-gray-400">Risk Score:</span>
                    <p className={`font-medium ${getRiskColor(profile.movementPatterns.riskScore)}`}>
                      {Math.round(profile.movementPatterns.riskScore * 100)}%
                    </p>
                  </div>
                  
                  <div>
                    <span className="text-gray-400">Path Stability:</span>
                    <p className={`font-medium ${getAccuracyColor(profile.movementPatterns.typicalPathStability)}`}>
                      {Math.round(profile.movementPatterns.typicalPathStability * 100)}%
                    </p>
                  </div>
                </div>
                
                <div className="mt-3 flex items-center justify-between text-xs text-gray-500">
                  <span>Last analyzed: {new Date(profile.lastAnalyzed).toLocaleDateString()}</span>
                  <Button variant="glass" size="sm">
                    <Eye className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Student Details Modal */}
      {selectedStudent && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <Card glass className="w-full max-w-4xl max-h-[80vh] overflow-y-auto">
            <CardHeader>
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-white">
                  AI Profile - Student {selectedStudent.studentId}
                </h3>
                <Button
                  variant="glass"
                  size="sm"
                  onClick={() => setSelectedStudent(null)}
                >
                  ×
                </Button>
              </div>
            </CardHeader>
            
            <CardContent>
              {/* Tab Navigation */}
              <div className="flex space-x-1 p-1 bg-gray-800/30 rounded-lg border border-gray-700 mb-6">
                <Button
                  variant={activeTab === 'overview' ? 'primary' : 'glass'}
                  size="sm"
                  onClick={() => setActiveTab('overview')}
                  className="flex-1"
                >
                  Overview
                </Button>
                
                <Button
                  variant={activeTab === 'patterns' ? 'primary' : 'glass'}
                  size="sm"
                  onClick={() => setActiveTab('patterns')}
                  className="flex-1"
                >
                  Patterns
                </Button>
                
                <Button
                  variant={activeTab === 'anomalies' ? 'primary' : 'glass'}
                  size="sm"
                  onClick={() => setActiveTab('anomalies')}
                  className="flex-1"
                >
                  Anomalies
                </Button>
                
                <Button
                  variant={activeTab === 'insights' ? 'primary' : 'glass'}
                  size="sm"
                  onClick={() => setActiveTab('insights')}
                  className="flex-1"
                >
                  Insights
                </Button>
              </div>

              {/* Tab Content */}
              {activeTab === 'overview' && (
                <div className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="p-4 bg-gray-800/30 rounded-lg">
                      <h4 className="text-white font-medium mb-4">Behavior Analysis</h4>
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Pattern Type:</span>
                          <div className={`px-2 py-1 rounded-full text-xs font-medium border ${getBehaviorPatternColor(selectedStudent.behaviorPattern)}`}>
                            {selectedStudent.behaviorPattern}
                          </div>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Path Stability:</span>
                          <span className={getAccuracyColor(selectedStudent.movementPatterns.typicalPathStability)}>
                            {Math.round(selectedStudent.movementPatterns.typicalPathStability * 100)}%
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Deviation Frequency:</span>
                          <span className="text-white">
                            {Math.round(selectedStudent.movementPatterns.deviationFrequency * 100)}%
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Risk Score:</span>
                          <span className={getRiskColor(selectedStudent.movementPatterns.riskScore)}>
                            {Math.round(selectedStudent.movementPatterns.riskScore * 100)}%
                          </span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="p-4 bg-gray-800/30 rounded-lg">
                      <h4 className="text-white font-medium mb-4">Prediction Accuracy</h4>
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Walk-Out Prediction:</span>
                          <span className={getAccuracyColor(selectedStudent.predictionAccuracy.walkOutPrediction)}>
                            {Math.round(selectedStudent.predictionAccuracy.walkOutPrediction * 100)}%
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Attendance Prediction:</span>
                          <span className={getAccuracyColor(selectedStudent.predictionAccuracy.attendancePrediction)}>
                            {Math.round(selectedStudent.predictionAccuracy.attendancePrediction * 100)}%
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-gray-400">Anomaly Detection:</span>
                          <span className={getAccuracyColor(selectedStudent.predictionAccuracy.anomalyDetection)}>
                            {Math.round(selectedStudent.predictionAccuracy.anomalyDetection * 100)}%
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'patterns' && (
                <div className="space-y-4">
                  <h4 className="text-white font-medium">Behavior Patterns</h4>
                  {generateBehaviorPatterns(selectedStudent.studentId).map((pattern, index) => (
                    <div key={index} className="p-4 bg-gray-800/30 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <h5 className="text-white font-medium">{pattern.type.replace('_', ' ')}</h5>
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          pattern.trend === 'IMPROVING' ? 'bg-green-500/20 text-green-400' :
                          pattern.trend === 'DECLINING' ? 'bg-red-500/20 text-red-400' :
                          'bg-gray-500/20 text-gray-400'
                        }`}>
                          {pattern.trend}
                        </span>
                      </div>
                      <div className="grid grid-cols-3 gap-4 text-sm">
                        <div>
                          <span className="text-gray-400">Frequency:</span>
                          <p className="text-white">{Math.round(pattern.frequency * 100)}%</p>
                        </div>
                        <div>
                          <span className="text-gray-400">Confidence:</span>
                          <p className="text-white">{Math.round(pattern.confidence * 100)}%</p>
                        </div>
                        <div>
                          <span className="text-gray-400">Contexts:</span>
                          <p className="text-white">{pattern.contexts.join(', ')}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {activeTab === 'anomalies' && (
                <div className="space-y-4">
                  <h4 className="text-white font-medium">Anomaly Records</h4>
                  {generateAnomalies(selectedStudent.studentId).map((anomaly, index) => (
                    <div key={index} className="p-4 bg-gray-800/30 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <h5 className="text-white font-medium">{anomaly.type.replace('_', ' ')}</h5>
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          anomaly.severity === 'HIGH' ? 'bg-red-500/20 text-red-400' :
                          anomaly.severity === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-400' :
                          'bg-blue-500/20 text-blue-400'
                        }`}>
                          {anomaly.severity}
                        </span>
                      </div>
                      <p className="text-gray-300 text-sm mb-2">{anomaly.description}</p>
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <span className="text-gray-400">Frequency:</span>
                          <p className="text-white">{anomaly.frequency} occurrences</p>
                        </div>
                        <div>
                          <span className="text-gray-400">Last Occurrence:</span>
                          <p className="text-white">
                            {new Date(anomaly.lastOccurrence).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {activeTab === 'insights' && (
                <div className="space-y-4">
                  <h4 className="text-white font-medium">AI Insights</h4>
                  <div className="p-4 bg-blue-500/10 rounded-lg border border-blue-500/20">
                    <h5 className="text-blue-400 font-medium mb-2">Key Findings</h5>
                    <ul className="text-gray-300 text-sm space-y-1">
                      <li>• {selectedStudent.behaviorPattern === 'CONSISTENT' ? 'Student shows reliable attendance patterns' : 'Student shows irregular behavior patterns'}</li>
                      <li>• Movement stability is {selectedStudent.movementPatterns.typicalPathStability > 0.8 ? 'high' : 'moderate'}</li>
                      <li>• Risk score requires {selectedStudent.movementPatterns.riskScore > 0.5 ? 'immediate attention' : 'monitoring'}</li>
                      <li>• Prediction accuracy is {selectedStudent.predictionAccuracy.attendancePrediction > 0.9 ? 'excellent' : 'acceptable'}</li>
                    </ul>
                  </div>
                  
                  <div className="p-4 bg-green-500/10 rounded-lg border border-green-500/20">
                    <h5 className="text-green-400 font-medium mb-2">Recommendations</h5>
                    <ul className="text-gray-300 text-sm space-y-1">
                      <li>• Continue monitoring for pattern changes</li>
                      <li>• Consider intervention if risk score increases</li>
                      <li>• Maintain consistent schedule for better reliability</li>
                      <li>• Use AI insights for personalized support</li>
                    </ul>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default StudentAIProfiles;
