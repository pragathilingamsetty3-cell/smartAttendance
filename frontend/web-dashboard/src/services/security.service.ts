import { EnhancedHeartbeatPing } from '../types/attendance';

/**
 * 🔐 Security Service
 * Handles HMAC-SHA256 request signing and packet sequence tracking
 * to ensure heartbeat integrity and order.
 */
class SecurityService {
  private sequenceCounters: Map<string, number> = new Map();

  /**
   * Generates an HMAC-SHA256 signature for the heartbeat payload.
   * Payload format: studentId|sessionId|latitude|longitude|stepCount|batteryLevel
   */
  async signHeartbeat(ping: EnhancedHeartbeatPing, secretKey: string): Promise<string> {
    const payload = `${ping.studentId}|${ping.sessionId}|${ping.latitude.toFixed(6)}|${ping.longitude.toFixed(6)}|${ping.stepCount}|${ping.batteryLevel || 0}`;
    
    // Use Web Crypto API for HMAC-SHA256
    const encoder = new TextEncoder();
    const keyData = encoder.encode(secretKey);
    const messageData = encoder.encode(payload);

    const cryptoKey = await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    );

    const signatureBuffer = await crypto.subtle.sign(
      'HMAC',
      cryptoKey,
      messageData
    );

    // Convert to hex string
    return Array.from(new Uint8Array(signatureBuffer))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }

  /**
   * Increments and returns the next sequence ID for a specific session.
   */
  getNextSequenceId(sessionId: string): number {
    const current = this.sequenceCounters.get(sessionId) || 0;
    const next = current + 1;
    this.sequenceCounters.set(sessionId, next);
    return next;
  }

  /**
   * Resets the sequence counter (e.g., when a session ends).
   */
  resetSequence(sessionId: string): void {
    this.sequenceCounters.delete(sessionId);
  }
}

export const securityService = new SecurityService();
export default securityService;
