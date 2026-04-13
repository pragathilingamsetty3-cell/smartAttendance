import apiClient from '../lib/apiClient';
import { OnboardingResponseDTO } from '../types/user-management';

export interface StudentOnboardingRequest { /* define later */ }
export interface FacultyOnboardingRequest { /* define later */ }
export interface AdminOnboardingRequest { /* define later */ }
export interface NLPInsightRequest { query: string; }

class AdminService {
  async onboardStudent(request: StudentOnboardingRequest): Promise<OnboardingResponseDTO> {
    const response = await apiClient.post('/api/v1/admin/onboard/student', request);
    return response.data;
  }

  async onboardFaculty(request: FacultyOnboardingRequest): Promise<OnboardingResponseDTO> {
    const response = await apiClient.post('/api/v1/admin/onboard/faculty', request);
    return response.data;
  }
  
  async onboardAdmin(request: AdminOnboardingRequest): Promise<OnboardingResponseDTO> {
    const response = await apiClient.post('/api/v1/admin/onboard/admin', request);
    return response.data;
  }

  async askAI(request: NLPInsightRequest): Promise<unknown> {
    const response = await apiClient.post('/api/v1/admin/ask-ai', request);
    return response.data;
  }
}

export const adminService = new AdminService();
export default adminService;
