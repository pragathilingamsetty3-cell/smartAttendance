import apiClient from '@/lib/apiClient';
import { 
  SpatialAnalysisResponse, 
  GPSDriftAnalysisResponse, 
  ContinuousTrackingResponse, 
  WalkOutPredictionResponse, 
  SessionAIStatusResponse 
} from '@/types/ai-analytics';
import { 
  AIAnalyticsDashboard, 
  AIModelMetrics, 
  AIAlert,
  MovementPattern
} from '@/types/ai-analytics';

class AIAnalyticsService {
  /**
   * 1. AI analyzes spatial behavior for anomalies
   * POST /api/v1/ai-analytics/spatial-analysis/{studentId}/{sessionId}
   */
  async spatialAnalysis(studentId: string, sessionId: string): Promise<SpatialAnalysisResponse> {
    const response = await apiClient.post(`/api/v1/ai-analytics/spatial-analysis/${studentId}/${sessionId}`);
    return response.data;
  }

  /**
   * 2. AI detects GPS drift vs real movement
   * POST /api/v1/ai-analytics/drift-analysis/{studentId}/{sessionId}
   */
  async driftAnalysis(studentId: string, sessionId: string): Promise<GPSDriftAnalysisResponse> {
    const response = await apiClient.post(`/api/v1/ai-analytics/drift-analysis/${studentId}/${sessionId}`);
    return response.data;
  }

  /**
   * 3. AI continuous tracking - learns student patterns
   * POST /api/v1/ai-analytics/continuous-tracking/{studentId}/{sessionId}
   */
  async continuousTracking(studentId: string, sessionId: string): Promise<ContinuousTrackingResponse> {
    const response = await apiClient.post(`/api/v1/ai-analytics/continuous-tracking/${studentId}/${sessionId}`);
    return response.data;
  }

  /**
   * 4. AI predicts if student will walk out
   * POST /api/v1/ai-analytics/walk-out-prediction/{studentId}/{sessionId}
   */
  async predictWalkOut(studentId: string, sessionId: string): Promise<WalkOutPredictionResponse> {
    const response = await apiClient.post(`/api/v1/ai-analytics/walk-out-prediction/${studentId}/${sessionId}`);
    return response.data;
  }

  /**
   * 5. Get AI status for all students in a session
   * GET /api/v1/ai-analytics/session-ai-status/{sessionId}
   */
  async getSessionAIStatus(sessionId: string): Promise<SessionAIStatusResponse> {
    const response = await apiClient.get(`/api/v1/ai-analytics/session-ai-status/${sessionId}`);
    return response.data;
  }

  // --- Actual API Integrations (Replaced Stubs) ---
  
  async getAnalyticsDashboard(departmentId?: string, sectionId?: string): Promise<AIAnalyticsDashboard> {
    const params = new URLSearchParams();
    if (departmentId) params.append('departmentId', departmentId);
    if (sectionId) params.append('sectionId', sectionId);
    const response = await apiClient.get(`/api/v1/ai-analytics/dashboard?${params.toString()}`);
    return response.data;
  }
  
  async getSpatialBehavior(sessionId: string): Promise<MovementPattern[]> {
    const response = await apiClient.get<MovementPattern[]>(`/api/v1/ai-analytics/spatial-behavior/${sessionId}`);
    return response.data;
  }

  async getFilteredSpatialBehavior(departmentId?: string, sectionId?: string): Promise<MovementPattern[]> {
    const params = new URLSearchParams();
    if (departmentId) params.append('departmentId', departmentId);
    if (sectionId) params.append('sectionId', sectionId);
    const response = await apiClient.get<MovementPattern[]>(`/api/v1/ai-analytics/spatial-behavior/filtered?${params.toString()}`);
    return response.data;
  }
  
  async getModelMetrics(): Promise<AIModelMetrics> {
    const response = await apiClient.get('/api/v1/ai-analytics/performance/metrics');
    return response.data;
  }

  async getActiveAlerts(departmentId?: string, sectionId?: string): Promise<AIAlert[]> {
    const params = new URLSearchParams();
    if (departmentId) params.append('departmentId', departmentId);
    if (sectionId) params.append('sectionId', sectionId);
    const response = await apiClient.get(`/api/v1/ai-analytics/alerts/active?${params.toString()}`);
    return response.data;
  }

  /**
   * 6. AI Assistant Query
   * POST /api/v1/ai-analytics/ask-ai
   */
  async askAI(question: string): Promise<{ question: string, answer: string, status: string }> {
    const response = await apiClient.post('/api/v1/ai-analytics/ask-ai', { question });
    return response.data;
  }

  /**
   * 7. AI Weekly Insights
   * GET /api/v1/ai-analytics/weekly-insights
   */
  async getWeeklyInsights(): Promise<{ insights: string, generatedAt: string, status: string }> {
    const response = await apiClient.get('/api/v1/ai-analytics/weekly-insights');
    return response.data;
  }
}

export const aiAnalyticsService = new AIAnalyticsService();
