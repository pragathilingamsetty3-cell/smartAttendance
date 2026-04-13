import apiClient from '../lib/apiClient';

export interface CRLRAssignmentRequest {
  studentId: string;
  sectionId: string;
  roleType: string; // 'CR' or 'LR'
  academicYear?: string;
  semester?: string;
}

class RoleAssignmentService {
  async assignCRLR(request: CRLRAssignmentRequest): Promise<unknown> {
    const response = await apiClient.post('/api/v1/cr-lr-assignments/assign', request);
    return response.data;
  }

  async getSectionAssignments(sectionId: string): Promise<unknown> {
    const response = await apiClient.get(`/api/v1/cr-lr-assignments/section/${sectionId}`);
    return response.data;
  }

  async revokeCRLR(assignmentId: string, reason: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/cr-lr-assignments/${assignmentId}/revoke`, null, {
      params: { reason }
    });
    return response.data;
  }

  async getCoordinatorAssignments(): Promise<unknown> {
    const response = await apiClient.get('/api/v1/cr-lr-assignments/my-assignments');
    return response.data;
  }
}

export const roleAssignmentService = new RoleAssignmentService();
export default roleAssignmentService;
