// Room Management Types based on API_BLUEPRINT.md

export interface RoomCreationRequest {
  name: string;
  capacity: number;
  building: string;
  floor: number;
  boundaryPoints: Coordinate[];
  description?: string;
  sensorUrl?: string;
}

export interface Room {
  id: string;
  name: string;
  capacity: number;
  building: string;
  floor: number;
  boundaryPolygon?: Polygon;
  description?: string;
  sensorUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RoomListResponse {
  message: string;
  totalRooms: number;
  rooms: RoomListItem[];
}

export interface RoomListItem {
  roomId: string;
  name: string;
  capacity: number;
  building: string;
  floor: number;
  isAvailable: boolean;
  hasBoundary: boolean;
  boundaryType: 'NONE' | 'RECTANGLE' | 'PENTAGON' | 'POLYGON' | 'COMPLEX' | 'L_SHAPE' | 'CIRCLE';
  description?: string;
}

export interface BoundaryValidationRequest {
  coordinates: [number, number][];
}

export interface BoundaryValidationResponse {
  message: string;
  isValid: boolean;
  boundaryType: string;
  area: number;
  errors: string[];
  warnings: string[];
  recommendations: string[];
}

export interface BoundaryCreationRequest {
  centerLat: number;
  centerLng: number;
  widthMeters?: number;
  heightMeters?: number;
  radiusMeters?: number;
  longSideMeters?: number;
  shortSideMeters?: number;
}

export interface RectanglePayload {
  centerLat: number;
  centerLng: number;
  widthMeters: number;
  heightMeters: number;
}

export interface CirclePayload {
  centerLat: number;
  centerLng: number;
  radiusMeters: number;
}

export interface LShapePayload {
  centerLat: number;
  centerLng: number;
  longSideMeters: number;
  shortSideMeters: number;
}

export interface BoundaryResponse {
  message: string;
  boundaryType: string;
  coordinates: [number, number][];
  area: number;
  isValid: boolean;
  warnings: string[];
}

export interface RoomChangeRequest {
  roomId: string;
  sectionId: string;
  scheduledTime: string;
  reason: string;
  notifyStudents: boolean;
}

export interface QRRoomChangeRequest {
  roomId: string;
  facultyId: string;
  sectionId: string;
  reason: string;
  isEmergency: boolean;
  isPermanent: boolean;
  requiresGracePeriod: boolean;
  notifyStudents: boolean;
}

export interface WeeklyRoomSwapConfig {
  originalRoomId: string;
  newRoomId: string;
  swapDate: string;
  reason: string;
  notifyStudents: boolean;
  isActive: boolean;
}

export interface RoomChangeTransition {
  id: string;
  sessionId: string;
  originalRoomId: string;
  newRoomId: string;
  gracePeriodMinutes: number;
  transitionStartTime: string;
  transitionEndTime: string;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED';
}

export interface GracePeriodCheck {
  studentId: string;
  sessionId: string;
  inGracePeriod: boolean;
  gracePeriodMinutes: number;
  message: string;
}

// Geographic types
export interface Coordinate {
  longitude: number;
  latitude: number;
}

export interface Polygon {
  type: 'Polygon';
  coordinates: Coordinate[][];
}

export interface GeoJSON {
  type: 'Feature';
  geometry: Polygon;
  properties: {
    name?: string;
    capacity?: number;
    building?: string;
    floor?: number;
  };
}
