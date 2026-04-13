import apiClient from '../lib/apiClient';
import {
  StudentOnboardingRequest,
  FacultyOnboardingRequest,
  AdminOnboardingRequest,
  OnboardingResponseDTO,
  DropdownDTO,
  DepartmentCreateRequest,
  SectionCreateRequest,
  BulkPromotionRequest,
  UpdateUserStatusRequest,
  UserListResponse,
  DeviceRegistrationRequest,
  BiometricValidationRequest,
  OfflineSyncRequest,
} from '../types/user-management';
import { createErrorHandler } from '../utils/errorHandler';

const handleUserManagementError = createErrorHandler('UserManagementService');

const validateRole = (requiredRole: 'ADMIN' | 'SUPER_ADMIN', currentUserRole?: string): boolean => {
  if (!currentUserRole) return false;
  if (requiredRole === 'SUPER_ADMIN') return currentUserRole === 'SUPER_ADMIN';
  if (requiredRole === 'ADMIN') return currentUserRole === 'ADMIN' || currentUserRole === 'SUPER_ADMIN';
  return false;
};

class UserManagementService {
  async onboardStudent(request: StudentOnboardingRequest): Promise<OnboardingResponseDTO> {
    try {
      const response = await apiClient.post<OnboardingResponseDTO>('/api/v1/admin/onboard/student', request);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, '');
    }
  }

  async onboardFaculty(request: FacultyOnboardingRequest): Promise<OnboardingResponseDTO> {
    try {
      const response = await apiClient.post<OnboardingResponseDTO>('/api/v1/admin/onboard/faculty', request);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async onboardAdmin(request: AdminOnboardingRequest): Promise<OnboardingResponseDTO> {
    try {
      const response = await apiClient.post<OnboardingResponseDTO>('/api/v1/admin/onboard/admin', request);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async createDepartment(request: DepartmentCreateRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/admin/departments', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async getDepartments(): Promise<DropdownDTO[]> {
    try {
      const response = await apiClient.get<DropdownDTO[]>('/api/v1/admin/departments');
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async updateDepartment(id: string, request: DepartmentCreateRequest): Promise<void> {
    try {
      await apiClient.put(`/api/v1/admin/departments/${id}`, request);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to update department');
    }
  }

  async deleteDepartment(id: string): Promise<void> {
    try {
      await apiClient.delete(`/api/v1/admin/departments/${id}`);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to delete department');
    }
  }

  async createSection(request: SectionCreateRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/admin/sections', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async getSections(departmentId?: string): Promise<DropdownDTO[]> {
    try {
      if (departmentId) {
        const response = await apiClient.get<DropdownDTO[]>(`/api/v1/admin/departments/${departmentId}/sections`);
        return response.data;
      }
      return [];
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async updateSection(id: string, request: SectionCreateRequest): Promise<void> {
    try {
      await apiClient.put(`/api/v1/admin/sections/${id}`, request);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to update section');
    }
  }

  async deleteSection(id: string): Promise<void> {
    try {
      await apiClient.delete(`/api/v1/admin/sections/${id}`);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to delete section');
    }
  }

  async getDepartmentFaculty(departmentId: string): Promise<any[]> {
    try {
      const response = await apiClient.get(`/api/v1/admin/departments/${departmentId}/faculty`);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, `Failed to fetch faculty for department: ${departmentId}`);
    }
  }

  async getSectionStudents(sectionId: string): Promise<any[]> {
    try {
      const response = await apiClient.get(`/api/v1/admin/sections/${sectionId}/students`);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, `Failed to fetch students for section: ${sectionId}`);
    }
  }

  async getAllUsers(page: number = 1, limit: number = 10): Promise<UserListResponse | unknown> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        limit: limit.toString(),
      });
      const response = await apiClient.get<UserListResponse>(`/api/v1/admin/users?${params}`);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async getUserDetails(userId: string): Promise<any> {
    try {
      const response = await apiClient.get(`/api/v1/admin/users/${userId}`);
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to fetch user details');
    }
  }

  async updateUserStatus(studentId: string, request: UpdateUserStatusRequest): Promise<void> {
    try {
      await apiClient.put(`/api/v1/admin/students/${studentId}/status`, request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async resetUserDevice(registrationNumber: string): Promise<void> {
    try {
      await apiClient.post(`/api/v1/admin/students/${registrationNumber}/reset-device`);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async bulkPromoteStudents(request: BulkPromotionRequest): Promise<void> {
    try {
      await apiClient.put('/api/v1/admin/students/promote', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async registerDevice(request: DeviceRegistrationRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/device/register', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async validateBiometric(request: BiometricValidationRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/device/validate-biometric', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async syncOfflineAttendance(request: OfflineSyncRequest): Promise<void> {
    try {
      await apiClient.post('/api/v1/device/sync-offline', request);
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async getDeviceStatus(): Promise<{
    isRegistered: boolean;
    deviceId?: string;
    lastSync?: string;
  }> {
    try {
      const response = await apiClient.get('/api/v1/device/status');
      // @ts-expect-error omega clearance
      return response.data as unknown;
    } catch (error) {
      throw handleUserManagementError(error, 'Operation failed');
    }
  }

  async getSemesters(): Promise<string[]> {
    try {
      const response = await apiClient.get<string[]>('/api/v1/admin/metadata/semesters');
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to fetch semesters');
    }
  }

  async getAcademicYears(): Promise<string[]> {
    try {
      const response = await apiClient.get<string[]>('/api/v1/admin/metadata/academic-years');
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to fetch academic years');
    }
  }

  async getRoles(): Promise<string[]> {
    try {
      const response = await apiClient.get<string[]>('/api/v1/admin/metadata/roles');
      return response.data;
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to fetch roles');
    }
  }

  async updateUser(userId: string, request: any): Promise<void> {
    try {
      await apiClient.put(`/api/v1/admin/users/${userId}`, request);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to update user identity');
    }
  }

  async getUserActivity(userId: string): Promise<any[]> {
    try {
      const response = await apiClient.get(`/api/v1/admin/users/${userId}/activity`);
      return response.data.activity;
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to fetch user activity logs');
    }
  }

  async downloadSectionReport(
    sectionId: string, 
    threshold?: number, 
    startDate?: string, 
    endDate?: string
  ): Promise<void> {
    try {
      const params = new URLSearchParams();
      params.append('sectionId', sectionId);
      if (threshold !== undefined) params.append('threshold', threshold.toString());
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);

      const response = await apiClient.get(`/api/v1/reports/attendance/section?${params.toString()}`, {
        responseType: 'blob',
      });

      // Create a link and trigger download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const fileName = `attendance_report_${new Date().toISOString().split('T')[0]}.xlsx`;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to download attendance report');
    }
  }

  async downloadStudentList(sectionId: string): Promise<void> {
    try {
      const response = await apiClient.get(`/api/v1/reports/attendance/section/${sectionId}/students`, {
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const fileName = `student_list_${new Date().toISOString().split('T')[0]}.xlsx`;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      throw handleUserManagementError(error, 'Failed to download student list');
    }
  }
}

export const userManagementService = new UserManagementService();
export default userManagementService;
