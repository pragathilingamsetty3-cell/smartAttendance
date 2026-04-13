import apiClient from '../lib/apiClient';

class MonitoringService {
  async getSystemMetrics(): Promise<unknown> {
    const response = await apiClient.get('/api/v1/monitoring/system-metrics');
    return response.data;
  }

  async getPerformanceMetrics(): Promise<unknown> {
    const response = await apiClient.get('/api/v1/performance/metrics');
    return response.data;
  }
}

export const monitoringService = new MonitoringService();
export default monitoringService;
