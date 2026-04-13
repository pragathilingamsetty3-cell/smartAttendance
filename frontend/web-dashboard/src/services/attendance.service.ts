import apiClient from '../lib/apiClient';
import {
  EnhancedHeartbeatPing,
  HeartbeatResponse,
  HallPassRequestDTO,
  HallPassStatusDTO,
  HallPassApprovalRequest,
  HallPassDenialRequest,
  SensorStatusResponse
} from '../types/attendance';
import { createErrorHandler } from '../utils/errorHandler';

const handleAttendanceError = createErrorHandler('AttendanceService');

class AttendanceService {
  // Enhanced Heartbeat with Sensor Fusion
  async sendHeartbeat(ping: EnhancedHeartbeatPing): Promise<HeartbeatResponse> {
    try {
      const response = await apiClient.post<HeartbeatResponse>('/api/v1/attendance/heartbeat', ping);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  async sendHeartbeatEnhanced(ping: EnhancedHeartbeatPing): Promise<HeartbeatResponse> {
    try {
      const response = await apiClient.post<HeartbeatResponse>('/api/v1/attendance/heartbeat-enhanced', ping);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  // Hall Pass Management (Student)
  async requestHallPass(request: HallPassRequestDTO): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/attendance/hall-pass', request);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  // Hall Pass Management (Faculty)
  async facultyRequestHallPass(request: HallPassRequestDTO): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/faculty/hall-pass/request', request);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  async approveHallPass(request: HallPassApprovalRequest): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/faculty/hall-pass/approve', request);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  async denyHallPass(request: HallPassDenialRequest): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/faculty/hall-pass/deny', request);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  async getPendingHallPasses(): Promise<unknown> {
    try {
      const response = await apiClient.get<unknown>('/api/v1/faculty/hall-pass/pending');
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  async getHallPassHistory(sessionId: string): Promise<unknown> {
    try {
      const response = await apiClient.get<unknown>(`/api/v1/faculty/hall-pass/history/${sessionId}`);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  // Sensor Status
  async getSensorStatus(sessionId: string, studentId: string): Promise<unknown> {
    try {
      const response = await apiClient.get<unknown>(`/api/v1/attendance/sensor-status/${sessionId}/${studentId}`);
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, '');
    }
  }

  // --- Real Endpoints Mapping (Replaced Stubs) ---
  async getSession(sessionId: string): Promise<unknown> {
    const response = await apiClient.get(`/api/v1/faculty/sessions/${sessionId}`);
    return response.data;
  }
  
  async getAttendanceRecords(sessionId: string): Promise<unknown[]> {
    const response = await apiClient.get(`/api/v1/faculty/sessions/${sessionId}/records`);
    return response.data;
  }
  
  async getFacultySections(): Promise<{ id: string; name: string }[]> {
    try {
      const response = await apiClient.get<any[]>('/api/v1/faculty/sections');
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, 'Failed to fetch faculty sections');
    }
  }

  // WebSocket subscription handled ideally in websocketClient.ts, 
  // but exposing these interfaces to prevent UI breakage.
  subscribeToSessionUpdates(sessionId: string, cb: (data: unknown) => void) {
    console.warn('subscribeToSessionUpdates should be routed through WebSocketClient');
  }
  unsubscribeFromSessionUpdates(sessionId: string) {}
  
  async getActiveSessions(): Promise<unknown[]> {
    const response = await apiClient.get('/api/v1/faculty/sessions/active');
    return response.data;
  }

  async createSession(data: unknown): Promise<unknown> {
    const response = await apiClient.post('/api/v1/faculty/sessions', data);
    return response.data;
  }

  async resumeSession(id: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/faculty/sessions/${id}/resume`);
    return response.data;
  }

  async pauseSession(id: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/faculty/sessions/${id}/pause`);
    return response.data;
  }

  async endSession(id: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/faculty/sessions/${id}/end`);
    return response.data;
  }

  async getHallPassStatus(id: string): Promise<unknown> {
    const response = await apiClient.get(`/api/v1/faculty/hall-pass/${id}/status`);
    return response.data;
  }

  async getFacultyDashboardStats(): Promise<any> {
    try {
      const response = await apiClient.get('/api/v1/faculty/dashboard/stats');
      return response.data;
    } catch (error) {
      throw handleAttendanceError(error, 'Failed to fetch faculty dashboard stats');
    }
  }

  // --- REPORTING & EXPORT ---
  async downloadSectionReport(sectionId: string, threshold?: number): Promise<void> {
    try {
      const response = await apiClient.get('/api/v1/reports/attendance/section', {
        params: { sectionId, threshold },
        responseType: 'blob'
      });
      this.triggerDownload(response.data, `attendance_report_${sectionId}.xlsx`);
    } catch (error) {
      throw handleAttendanceError(error, 'Failed to download attendance report');
    }
  }

  async downloadStudentList(sectionId: string): Promise<void> {
    try {
      const response = await apiClient.get(`/api/v1/reports/attendance/section/${sectionId}/students`, {
        responseType: 'blob'
      });
      this.triggerDownload(response.data, `student_list_${sectionId}.xlsx`);
    } catch (error) {
      throw handleAttendanceError(error, 'Failed to download student list');
    }
  }

  async downloadBulkReport(deptId?: string, sectionId?: string, startDate?: string, endDate?: string): Promise<void> {
    try {
      const response = await apiClient.get('/api/v1/reports/attendance/bulk', {
        params: { deptId, sectionId, startDate, endDate },
        responseType: 'blob'
      });
      
      const timestamp = new Date().toISOString().split('T')[0].replace(/-/g, '');
      const fileName = `Full_Attendance_Bulk_Report_${timestamp}.zip`;
      
      this.triggerDownload(response.data, fileName);
    } catch (error) {
      throw handleAttendanceError(error, 'Failed to download bulk ZIP report');
    }
  }

  private triggerDownload(data: Blob, fileName: string) {
    const url = window.URL.createObjectURL(new Blob([data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
  }
}

export const attendanceService = new AttendanceService();
export default attendanceService;
