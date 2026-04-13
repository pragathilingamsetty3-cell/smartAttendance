import apiClient from '../lib/apiClient';

class ReportsService {
  /**
   * Export the attendance data as an Excel file.
   * GET /api/v1/reports/attendance/excel
   */
  async downloadAttendanceExcel(): Promise<Blob> {
    const response = await apiClient.get('/api/v1/reports/attendance/excel', {
      responseType: 'blob', // Critical for handling binary files
    });
    return response.data;
  }
}

export const reportsService = new ReportsService();
export default reportsService;
