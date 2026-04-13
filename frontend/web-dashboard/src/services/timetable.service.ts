import apiClient from '../lib/apiClient';

class TimetableService {
  async getTimetablesForFaculty(facultyId: string): Promise<any[]> {
    const response = await apiClient.get(`/api/v1/admin/timetables/faculty/${facultyId}`);
    return response.data;
  }

  async getTimetablesForSection(sectionId: string): Promise<any[]> {
    const response = await apiClient.get(`/api/v1/admin/timetables/section/${sectionId}`);
    return response.data;
  }

  async createTimetable(data: any): Promise<any> {
    const response = await apiClient.post('/api/v1/admin/timetables', data);
    return response.data;
  }

  async updateTimetable(id: string, data: any): Promise<any> {
    const response = await apiClient.put(`/api/v1/admin/timetables/${id}`, data);
    return response.data;
  }

  async deleteTimetable(id: string): Promise<void> {
    await apiClient.delete(`/api/v1/admin/timetables/${id}`);
  }

  async updateLunchBreak(timetableId: string, requestData: unknown): Promise<unknown> {
    const response = await apiClient.put(`/api/v1/admin/timetables/${timetableId}/lunch-break`, requestData);
    return response.data;
  }
}

export const timetableService = new TimetableService();
export default timetableService;
