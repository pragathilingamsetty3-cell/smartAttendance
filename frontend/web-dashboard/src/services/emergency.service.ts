import apiClient from '../lib/apiClient';
import { EmergencySessionChangeRequest, SubstituteClaimRequest } from '../types/emergency';

class EmergencyService {
  /**
   * Emergency session shifting (e.g. fire drill, sudden maintenance).
   * POST /api/v1/emergency/session-change
   */
  async triggerSessionChange(request: EmergencySessionChangeRequest): Promise<unknown> {
    const response = await apiClient.post('/api/v1/emergency/session-change', request);
    return response.data;
  }

  /**
   * Sudden faculty unavailability requiring a substitute to claim the session.
   * POST /api/v1/emergency/substitute-claim
   */
  async substituteClaim(request: SubstituteClaimRequest): Promise<unknown> {
    const response = await apiClient.post('/api/v1/emergency/substitute-claim', request);
    return response.data;
  }

  /**
   * Move the current active session rapidly.
   * POST /api/v1/emergency/quick-room-change
   */
  async quickRoomChange(): Promise<unknown> {
    const response = await apiClient.post('/api/v1/emergency/quick-room-change');
    return response.data;
  }
}

export const emergencyService = new EmergencyService();
export default emergencyService;
