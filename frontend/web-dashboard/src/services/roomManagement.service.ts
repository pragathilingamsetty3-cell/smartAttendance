import apiClient from '../lib/apiClient';
import {
  RoomCreationRequest,
  Room,
  RoomListItem,
  RoomListResponse,
  BoundaryValidationResponse,
  BoundaryResponse,
  RoomChangeRequest,
  QRRoomChangeRequest,
  WeeklyRoomSwapConfig,
  RoomChangeTransition,
  GracePeriodCheck,
  RectanglePayload,
  CirclePayload,
  LShapePayload
} from '../types/room-management';
import { createErrorHandler } from '../utils/errorHandler';

// Error handler for room management service
const handleRoomError = createErrorHandler('RoomManagementService');

class RoomManagementService {
  // AdminV1 Controller - Rooms
  async createRoom(request: RoomCreationRequest): Promise<Room> {
    try {
      const response = await apiClient.post<Room>('/api/v1/admin/rooms', request);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async getAllRooms(): Promise<RoomListResponse> {
    try {
      const response = await apiClient.get<RoomListResponse>('/api/v1/admin/rooms');
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async getRoomById(id: string): Promise<Room> {
    try {
      const response = await apiClient.get<{room: Room}>(`/api/v1/admin/rooms/${id}`);
      return response.data.room;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async updateRoom(id: string, request: RoomCreationRequest): Promise<Room> {
    try {
      const response = await apiClient.put<Room>(`/api/v1/admin/rooms/${id}`, request);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async deleteRoom(id: string): Promise<void> {
    try {
      await apiClient.delete(`/api/v1/admin/rooms/${id}`);
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async updateBoundary(roomId: string, coordinates: number[][]): Promise<BoundaryResponse> {
    try {
      const response = await apiClient.put<BoundaryResponse>(`/api/v1/admin/rooms/${roomId}/boundary`, { coordinates });
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async validateRoomBoundary(roomId: string, coordinates: number[][]): Promise<BoundaryValidationResponse> {
    try {
      const response = await apiClient.post<BoundaryValidationResponse>(`/api/v1/admin/rooms/${roomId}/validate-boundary`, { coordinates });
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async getBoundary(roomId: string): Promise<BoundaryResponse> {
    try {
      const response = await apiClient.get<BoundaryResponse>(`/api/v1/admin/rooms/${roomId}/boundary`);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  // Room Change Management
  async requestPrePlannedRoomChange(request: RoomChangeRequest): Promise<RoomChangeTransition> {
    try {
      const response = await apiClient.post<RoomChangeTransition>('/api/v1/room-change/pre-planned', request);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async requestQRRoomChange(request: QRRoomChangeRequest): Promise<RoomChangeTransition> {
    try {
      const response = await apiClient.post<RoomChangeTransition>('/api/v1/room-change/qr', request);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async createRectangleBoundary(payload: RectanglePayload): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/admin/boundaries/rectangle', payload);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async createCircleBoundary(payload: CirclePayload): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/admin/boundaries/circle', payload);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async createLShapeBoundary(payload: LShapePayload): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/admin/boundaries/l-shape', payload);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async getActiveTransitions(): Promise<unknown> {
    try {
      const response = await apiClient.get<unknown>('/api/v1/room-change/active-transitions');
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async executeWeeklySwap(swapConfigId: string): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>(`/api/v1/room-change/weekly-swap/${swapConfigId}`);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async createWeeklySwapConfig(config: WeeklyRoomSwapConfig): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/room-change/weekly-swap-config', config);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async checkGracePeriod(userId: string, sessionId: string): Promise<GracePeriodCheck> {
    try {
      const response = await apiClient.get<GracePeriodCheck>(`/api/v1/room-change/grace-period/${userId}/${sessionId}`);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  // Boundary Management Controller general routes
  async simplifyBoundary(request: unknown): Promise<unknown> {
    try {
      const response = await apiClient.post<unknown>('/api/v1/admin/boundaries/simplify', request);
      return response.data;
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  // --- Actual API Integrations (Replaced Stubs) ---
  async getAvailableRooms(): Promise<RoomListItem[]> {
    try {
      const response = await apiClient.get<RoomListResponse>('/api/v1/admin/rooms');
      
      // ✅ Handle the structured response from AdminV1Controller
      if (response.data && response.data.rooms) {
        return response.data.rooms;
      }
      
      // Fallback for different API versions
      return Array.isArray(response.data) ? response.data : 
             (response.data as any).content ? (response.data as any).content : [];
    } catch (error) {
      throw handleRoomError(error, '');
    }
  }

  async getRoomChangeHistory(): Promise<unknown[]> {
    const response = await apiClient.get('/api/v1/room-change/history');
    return response.data;
  }

  async getWeeklySwaps(): Promise<unknown[]> {
    const response = await apiClient.get('/api/v1/room-change/weekly-swaps');
    return response.data;
  }

  async approveRoomChange(id: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/admin/rooms/change/${id}/approve`);
    return response.data;
  }

  async denyRoomChange(id: string): Promise<unknown> {
    const response = await apiClient.post(`/api/v1/admin/rooms/change/${id}/deny`);
    return response.data;
  }

  async quickRoomChange(roomId: string, sectionId: string, reason?: string): Promise<unknown> {
    const response = await apiClient.post('/api/v1/room-change/quick', null, {
      params: { 
        roomId, 
        sectionId, 
        reason: reason || 'Sudden room change' 
      }
    });
    return response.data;
  }
}

export const roomManagementService = new RoomManagementService();
export default roomManagementService;
