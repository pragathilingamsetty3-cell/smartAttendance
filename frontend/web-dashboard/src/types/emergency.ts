export interface EmergencySessionChangeRequest {
  roomId: string;
  sessionTime: string; // ISO 8601 string
  reason?: string;
  notifyAll?: boolean;
}

export interface SubstituteClaimRequest {
  substituteFacultyId: string;
  sessionId: string;
  reason?: string;
}
