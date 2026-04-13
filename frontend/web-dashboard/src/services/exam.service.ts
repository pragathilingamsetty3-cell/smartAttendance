import apiClient from '../lib/apiClient';

export interface ExamBarcodeScanRequest {
  barcode: string;
  sessionId: string;
  deviceId: string;
}

class ExamService {
  async scanBarcode(request: ExamBarcodeScanRequest): Promise<unknown> {
    const response = await apiClient.post('/api/v1/exam/scan-barcode', request);
    return response.data;
  }

  async getTodaySessions(): Promise<unknown> {
    const response = await apiClient.get('/api/v1/exam/today-sessions');
    return response.data;
  }

  async getExamSessionDetails(sessionId: string): Promise<unknown> {
    const response = await apiClient.get(`/api/v1/exam/session/${sessionId}`);
    return response.data;
  }
}

export const examService = new ExamService();
export default examService;
