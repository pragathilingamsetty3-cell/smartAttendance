// AI Analytics Types based on API_BLUEPRINT.md

export interface SpatialAnalysisRequest {
  studentId: string;
  sessionId: string;
}

export interface SpatialAnalysisResponse {
  studentId: string;
  sessionId: string;
  anomalyDetected: boolean;
  anomalyType: string;
  status: string;
  processedBy: 'AI';
}

export interface GPSDriftAnalysisResponse {
  studentId: string;
  sessionId: string;
  isGPSDrift: boolean;
  isSpoofing: boolean;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  processedBy: 'AI';
}

export interface ContinuousTrackingResponse {
  studentId: string;
  sessionId: string;
  trackingStatus: string;
  behaviorPattern: string;
  processedBy: 'AI';
}

export interface WalkOutPredictionResponse {
  studentId: string;
  sessionId: string;
  willWalkOut: boolean;
  probability: number;
  reason: string;
  processedBy: 'AI';
}

export interface SessionAIStatusResponse {
  sessionId: string;
  message: string;
  features: {
    spatialAnomalyDetection: 'ACTIVE' | 'INACTIVE';
    gpsDriftAnalysis: 'ACTIVE' | 'INACTIVE';
    continuousBehaviorLearning: 'ACTIVE' | 'INACTIVE';
    walkOutPrediction: 'ACTIVE' | 'INACTIVE';
    hallPassControl: 'FACULTY_CONTROLLED' | 'AI_CONTROLLED';
  };
  controlledBy: string;
}

export interface AIAnalyticsDashboard {
  sessionId: string;
  totalStudents: number;
  activeStudents: number;
  anomaliesDetected: number;
  totalPredictions: number; // Added for volume tracking
  spoofingAttempts: number;
  walkOutPredictions: number;
  totalAbsences: number;
  liveVerifications: number;
  averageConfidence: number;
  lastUpdated: string;
  activeSessions?: Array<{ id: string; room: string; active: boolean }>;
  velocityTrend?: Array<{ time: string; value: number }>;
  anomalyBreakdown?: Array<{ type: string; count: number }>;
}

export interface StudentAIProfile {
  studentId: string;
  behaviorPattern: 'CONSISTENT' | 'ERRATIC' | 'SUSPICIOUS';
  attendanceReliability: number;
  movementPatterns: {
    typicalPathStability: number;
    deviationFrequency: number;
    riskScore: number;
  };
  predictionAccuracy: {
    walkOutPrediction: number;
    attendancePrediction: number;
    anomalyDetection: number;
  };
  lastAnalyzed: string;
}

export interface AIAlert {
  id: string;
  type: 'SPATIAL_ANOMALY' | 'GPS_DRIFT' | 'SPOOFING_DETECTED' | 'WALK_OUT_PREDICTION' | 'BEHAVIOR_CHANGE' | 'WALKOUT_ABSENCE';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  studentId: string;
  studentName?: string;
  registrationNumber?: string;
  sessionId: string;
  message: string;
  confidence: number;
  timestamp: string;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
}

export interface AIModelMetrics {
  modelName: string;
  version: string;
  accuracy: number;
  precision: number;
  recall: number;
  f1Score: number;
  totalPredictions: number;
  correctPredictions: number;
  lastTrained: string;
  trainingDataSize: number;
}

export interface MovementPattern {
  studentId: string;
  studentName: string;
  pattern: 'STATIONARY' | 'WALKING' | 'RUNNING' | 'ERRATIC' | 'MOVING';
  status?: string;
  confidence: number;
  speed: number;
  distance: number;
  anomalies: number;
  latitude?: number;
  longitude?: number;
  accuracy?: number;
  timestamp?: string;
}

export interface HeatmapPoint {
  x: number;
  y: number;
  intensity: number;
  studentId?: string;
  timestamp?: string;
}
