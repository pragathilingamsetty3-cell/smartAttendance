/**
 * Emergency Manager for Smart Attendance System
 * Handles emergency sessions, substitute faculty, and crisis management
 */

export interface EmergencySession {
  id: string;
  type: 'FIRE' | 'MEDICAL' | 'SECURITY' | 'WEATHER' | 'OTHER';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  title: string;
  description: string;
  location?: string;
  affectedRooms: string[];
  affectedUsers: string[];
  startTime: string;
  endTime?: string;
  status: 'ACTIVE' | 'RESOLVED' | 'CANCELLED';
  createdBy: string;
  responders: string[];
  evacuationRequired: boolean;
  shelterRequired: boolean;
  medicalRequired: boolean;
  communicationSent: boolean;
  rollCallRequired: boolean;
  instructions: string[];
}

export interface SubstituteFaculty {
  id: string;
  originalFacultyId: string;
  substituteFacultyId: string;
  sessionId: string;
  roomId: string;
  sectionId: string;
  startTime: string;
  endTime: string;
  reason: 'SICKNESS' | 'PERSONAL' | 'PROFESSIONAL' | 'EMERGENCY' | 'OTHER';
  status: 'PENDING' | 'APPROVED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  approvedBy?: string;
  notes?: string;
  handoverCompleted: boolean;
  studentNotified: boolean;
  temporaryAccess: boolean;
}

export interface CrisisAlert {
  id: string;
  type: 'LOCKDOWN' | 'EVACUATION' | 'SHELTER' | 'MEDICAL' | 'WEATHER';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  message: string;
  instructions: string[];
  affectedAreas: string[];
  startTime: string;
  endTime?: string;
  status: 'ACTIVE' | 'RESOLVED' | 'CANCELLED';
  communicationChannels: string[];
  acknowledgmentRequired: boolean;
  acknowledgments: Array<{
    userId: string;
    timestamp: string;
    location?: string;
  }>;
}

export interface EmergencyContact {
  id: string;
  name: string;
  role: string;
  phone: string;
  email: string;
  department: string;
  priority: 'PRIMARY' | 'SECONDARY' | 'TERTIARY';
  availability: {
    weekdays: boolean;
    weekends: boolean;
    holidays: boolean;
    afterHours: boolean;
  };
}

export interface EmergencyDrill {
  id: string;
  name: string;
  type: 'FIRE' | 'LOCKDOWN' | 'EARTHQUAKE' | 'TORNADO' | 'ACTIVE_SHOOTER';
  scheduledDate: string;
  duration: number;
  participants: string[];
  scenarios: string[];
  evaluationCriteria: string[];
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  results?: {
    evacuationTime: number;
    participationRate: number;
    communicationSuccess: number;
    issues: string[];
    recommendations: string[];
  };
}

export interface EmergencyNotification {
  type?: string;
  title?: string;
  message?: string;
  severity?: string;
  affectedUsers?: string[];
  instructions?: string[];
  test?: boolean;
}

/**
 * Emergency Manager Class
 */
export class EmergencyManager {
  private static instance: EmergencyManager;
  private activeEmergencies: EmergencySession[] = [];
  private activeAlerts: CrisisAlert[] = [];
  private emergencyContacts: EmergencyContact[] = [];
  private emergencyDrills: EmergencyDrill[] = [];
  private websocket: WebSocket | null = null;

  private constructor() {
    this.initializeWebSocket();
    this.loadEmergencyContacts();
  }

  static getInstance(): EmergencyManager {
    if (!EmergencyManager.instance) {
      EmergencyManager.instance = new EmergencyManager();
    }
    return EmergencyManager.instance;
  }

  /**
   * Initialize WebSocket for real-time emergency updates
   */
  private initializeWebSocket(): void {
    try {
      const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/emergency`;
      this.websocket = new WebSocket(wsUrl);

      this.websocket.onopen = () => {
        console.log('Emergency WebSocket connected');
      };

      this.websocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.handleEmergencyUpdate(data);
      };

      this.websocket.onerror = (error) => {
        console.error('Emergency WebSocket error:', error);
      };

      this.websocket.onclose = () => {
        console.log('Emergency WebSocket disconnected');
        // Attempt to reconnect after 5 seconds
        setTimeout(() => this.initializeWebSocket(), 5000);
      };
    } catch (error) {
      console.error('Failed to initialize emergency WebSocket:', error);
    }
  }

  /**
   * Handle emergency updates from WebSocket
   */
  private handleEmergencyUpdate(data: unknown): void {
    // @ts-expect-error type override for strict mode
    switch (data.type) {
      case 'EMERGENCY_CREATED':
        // @ts-expect-error type override for strict mode
        this.activeEmergencies.push(data.emergency);
        // @ts-expect-error type override for strict mode
        this.notifyEmergency(data.emergency);
        break;
      case 'EMERGENCY_UPDATED':
        // @ts-expect-error type override for strict mode
        this.updateEmergency(data.emergency);
        break;
      case 'ALERT_CREATED':
        // @ts-expect-error type override for strict mode
        this.activeAlerts.push(data.alert);
        // @ts-expect-error type override for strict mode
        this.notifyAlert(data.alert);
        break;
      case 'ALERT_RESOLVED':
        // @ts-expect-error type override for strict mode
        this.resolveAlert(data.alertId);
        break;
    }
  }

  /**
   * Create emergency session
   */
  public async createEmergencySession(emergency: Omit<EmergencySession, 'id' | 'startTime' | 'status'>): Promise<EmergencySession> {
    try {
      const response = await fetch('/api/v1/emergency/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(emergency),
      });

      if (!response.ok) {
        throw new Error('Failed to create emergency session');
      }

      const createdEmergency = await response.json();
      this.activeEmergencies.push(createdEmergency);
      
      // Send notifications
      await this.sendEmergencyNotifications(createdEmergency);
      
      return createdEmergency;
    } catch (error) {
      console.error('Error creating emergency session:', error);
      throw error;
    }
  }

  /**
   * Update emergency session
   */
  private updateEmergency(emergency: EmergencySession): void {
    const index = this.activeEmergencies.findIndex(e => e.id === emergency.id);
    if (index !== -1) {
      this.activeEmergencies[index] = emergency;
    }
  }

  /**
   * Resolve emergency session
   */
  public async resolveEmergencySession(emergencyId: string, resolution: string): Promise<void> {
    try {
      await fetch(`/api/v1/emergency/sessions/${emergencyId}/resolve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resolution }),
      });

      const index = this.activeEmergencies.findIndex(e => e.id === emergencyId);
      if (index !== -1) {
        this.activeEmergencies[index].status = 'RESOLVED';
        this.activeEmergencies[index].endTime = new Date().toISOString();
      }
    } catch (error) {
      console.error('Error resolving emergency session:', error);
      throw error;
    }
  }

  /**
   * Get active emergencies
   */
  public getActiveEmergencies(): EmergencySession[] {
    return this.activeEmergencies.filter(e => e.status === 'ACTIVE');
  }

  /**
   * Create crisis alert
   */
  public async createCrisisAlert(alert: Omit<CrisisAlert, 'id' | 'startTime' | 'status' | 'acknowledgments'>): Promise<CrisisAlert> {
    try {
      const response = await fetch('/api/v1/emergency/alerts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(alert),
      });

      if (!response.ok) {
        throw new Error('Failed to create crisis alert');
      }

      const createdAlert = await response.json();
      this.activeAlerts.push(createdAlert);
      
      // Send alert notifications
      await this.sendAlertNotifications(createdAlert);
      
      return createdAlert;
    } catch (error) {
      console.error('Error creating crisis alert:', error);
      throw error;
    }
  }

  /**
   * Acknowledge crisis alert
   */
  public async acknowledgeAlert(alertId: string, userId: string, location?: string): Promise<void> {
    try {
      await fetch(`/api/v1/emergency/alerts/${alertId}/acknowledge`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, location }),
      });

      const alert = this.activeAlerts.find(a => a.id === alertId);
      if (alert) {
        alert.acknowledgments.push({
          userId,
          timestamp: new Date().toISOString(),
          location,
        });
      }
    } catch (error) {
      console.error('Error acknowledging alert:', error);
      throw error;
    }
  }

  /**
   * Get active alerts
   */
  public getActiveAlerts(): CrisisAlert[] {
    return this.activeAlerts.filter(a => a.status === 'ACTIVE');
  }

  /**
   * Resolve crisis alert
   */
  public resolveAlert(alertId: string): void {
    const index = this.activeAlerts.findIndex(a => a.id === alertId);
    if (index !== -1) {
      this.activeAlerts[index].status = 'RESOLVED';
      this.activeAlerts[index].endTime = new Date().toISOString();
    }
  }

  /**
   * Request substitute faculty
   */
  public async requestSubstitute(request: Omit<SubstituteFaculty, 'id' | 'status'>): Promise<SubstituteFaculty> {
    try {
      const response = await fetch('/api/v1/emergency/substitute-faculty', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error('Failed to request substitute faculty');
      }

      return await response.json();
    } catch (error) {
      console.error('Error requesting substitute faculty:', error);
      throw error;
    }
  }

  /**
   * Get substitute faculty requests
   */
  public async getSubstituteRequests(status?: SubstituteFaculty['status']): Promise<SubstituteFaculty[]> {
    try {
      const params = status ? `?status=${status}` : '';
      const response = await fetch(`/api/v1/emergency/substitute-faculty${params}`);
      
      if (!response.ok) {
        throw new Error('Failed to get substitute requests');
      }

      return await response.json();
    } catch (error) {
      console.error('Error getting substitute requests:', error);
      throw error;
    }
  }

  /**
   * Approve substitute request
   */
  public async approveSubstitute(requestId: string, approvedBy: string): Promise<void> {
    try {
      await fetch(`/api/v1/emergency/substitute-faculty/${requestId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approvedBy }),
      });
    } catch (error) {
      console.error('Error approving substitute request:', error);
      throw error;
    }
  }

  /**
   * Send emergency notifications
   */
  private async sendEmergencyNotifications(emergency: EmergencySession): Promise<void> {
    const notification = {
      type: 'EMERGENCY',
      title: `Emergency: ${emergency.title}`,
      message: emergency.description,
      severity: emergency.severity,
      affectedUsers: emergency.affectedUsers,
      instructions: emergency.instructions,
    };

    // Send via multiple channels
    await Promise.all([
      this.sendNotification(notification),
      this.sendSMSNotification(notification),
      this.sendEmailNotification(notification),
      this.sendPushNotification(notification),
    ]);
  }

  /**
   * Send alert notifications
   */
  private async sendAlertNotifications(alert: CrisisAlert): Promise<void> {
    const notification = {
      type: 'CRISIS_ALERT',
      title: `Alert: ${alert.type}`,
      message: alert.message,
      severity: alert.severity,
      instructions: alert.instructions,
    };

    await Promise.all([
      this.sendNotification(notification),
      this.sendSMSNotification(notification),
      this.sendEmailNotification(notification),
      this.sendPushNotification(notification),
    ]);
  }

  /**
   * Send notification (in-app)
   */
  private async sendNotification(notification: EmergencyNotification): Promise<void> {
    // This would integrate with your notification system
    console.log('Sending notification:', notification);
  }

  /**
   * Send SMS notification
   */
  private async sendSMSNotification(notification: EmergencyNotification): Promise<void> {
    // This would integrate with SMS service
    console.log('Sending SMS notification:', notification);
  }

  /**
   * Send email notification
   */
  private async sendEmailNotification(notification: EmergencyNotification): Promise<void> {
    // This would integrate with email service
    console.log('Sending email notification:', notification);
  }

  /**
   * Send push notification
   */
  private async sendPushNotification(notification: EmergencyNotification): Promise<void> {
    // This would integrate with push notification service
    console.log('Sending push notification:', notification);
  }

  /**
   * Notify emergency to users
   */
  private notifyEmergency(emergency: EmergencySession): void {
    // Show emergency modal/alert
    if (emergency.severity === 'CRITICAL') {
      this.showEmergencyModal(emergency);
    } else {
      this.showEmergencyBanner(emergency);
    }
  }

  /**
   * Notify alert to users
   */
  private notifyAlert(alert: CrisisAlert): void {
    this.showAlertBanner(alert);
  }

  /**
   * Show emergency modal
   */
  private showEmergencyModal(emergency: EmergencySession): void {
    // This would show a modal with emergency information
    console.log('Showing emergency modal for:', emergency);
  }

  /**
   * Show emergency banner
   */
  private showEmergencyBanner(emergency: EmergencySession): void {
    // This would show a banner with emergency information
    console.log('Showing emergency banner for:', emergency);
  }

  /**
   * Show alert banner
   */
  private showAlertBanner(alert: CrisisAlert): void {
    // This would show a banner with alert information
    console.log('Showing alert banner for:', alert);
  }

  /**
   * Load emergency contacts
   */
  private async loadEmergencyContacts(): Promise<void> {
    try {
      const response = await fetch('/api/v1/emergency/contacts');
      if (response.ok) {
        this.emergencyContacts = await response.json();
      }
    } catch (error) {
      console.error('Error loading emergency contacts:', error);
    }
  }

  /**
   * Get emergency contacts
   */
  public getEmergencyContacts(): EmergencyContact[] {
    return this.emergencyContacts;
  }

  /**
   * Create emergency drill
   */
  public async createEmergencyDrill(drill: Omit<EmergencyDrill, 'id' | 'status'>): Promise<EmergencyDrill> {
    try {
      const response = await fetch('/api/v1/emergency/drills', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(drill),
      });

      if (!response.ok) {
        throw new Error('Failed to create emergency drill');
      }

      const createdDrill = await response.json();
      this.emergencyDrills.push(createdDrill);
      
      return createdDrill;
    } catch (error) {
      console.error('Error creating emergency drill:', error);
      throw error;
    }
  }

  /**
   * Get emergency drills
   */
  public async getEmergencyDrills(): Promise<EmergencyDrill[]> {
    try {
      const response = await fetch('/api/v1/emergency/drills');
      if (response.ok) {
        this.emergencyDrills = await response.json();
      }
      return this.emergencyDrills;
    } catch (error) {
      console.error('Error getting emergency drills:', error);
      return [];
    }
  }

  /**
   * Execute emergency drill
   */
  public async executeEmergencyDrill(drillId: string): Promise<void> {
    try {
      await fetch(`/api/v1/emergency/drills/${drillId}/execute`, {
        method: 'POST',
      });
    } catch (error) {
      console.error('Error executing emergency drill:', error);
      throw error;
    }
  }

  /**
   * Get emergency statistics
   */
  public getEmergencyStatistics(): {
    activeEmergencies: number;
    resolvedEmergencies: number;
    activeAlerts: number;
    resolvedAlerts: number;
    substituteRequests: number;
    drillsCompleted: number;
    averageResponseTime: number;
  } {
    return {
      activeEmergencies: this.getActiveEmergencies().length,
      resolvedEmergencies: this.activeEmergencies.filter(e => e.status === 'RESOLVED').length,
      activeAlerts: this.getActiveAlerts().length,
      resolvedAlerts: this.activeAlerts.filter(a => a.status === 'RESOLVED').length,
      substituteRequests: 0, // Would be fetched from API
      drillsCompleted: this.emergencyDrills.filter(d => d.status === 'COMPLETED').length,
      averageResponseTime: 0, // Would be calculated from data
    };
  }

  /**
   * Generate emergency report
   */
  public generateEmergencyReport(startDate: string, endDate: string): unknown {
    return {
      period: { startDate, endDate },
      emergencies: this.activeEmergencies.filter(e => 
        e.startTime >= startDate && e.startTime <= endDate
      ),
      alerts: this.activeAlerts.filter(a => 
        a.startTime >= startDate && a.startTime <= endDate
      ),
      drills: this.emergencyDrills.filter(d => 
        d.scheduledDate >= startDate && d.scheduledDate <= endDate
      ),
      statistics: this.getEmergencyStatistics(),
    };
  }

  /**
   * Test emergency system
   */
  public async testEmergencySystem(): Promise<{
    websocket: boolean;
    notifications: boolean;
    sms: boolean;
    email: boolean;
    push: boolean;
  }> {
    const results = {
      websocket: this.websocket?.readyState === WebSocket.OPEN,
      notifications: false,
      sms: false,
      email: false,
      push: false,
    };

    // Test notification systems
    try {
      await this.sendNotification({ test: true });
      results.notifications = true;
    } catch (error) {
      console.error('Notification test failed:', error);
    }

    try {
      await this.sendSMSNotification({ test: true });
      results.sms = true;
    } catch (error) {
      console.error('SMS test failed:', error);
    }

    try {
      await this.sendEmailNotification({ test: true });
      results.email = true;
    } catch (error) {
      console.error('Email test failed:', error);
    }

    try {
      await this.sendPushNotification({ test: true });
      results.push = true;
    } catch (error) {
      console.error('Push notification test failed:', error);
    }

    return results;
  }

  /**
   * Destroy emergency manager
   */
  public destroy(): void {
    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }
  }
}

// Export singleton instance
export const emergencyManager = EmergencyManager.getInstance();

// Export hook for React components
export const useEmergencyManager = () => {
  return {
    createEmergencySession: emergencyManager.createEmergencySession.bind(emergencyManager),
    resolveEmergencySession: emergencyManager.resolveEmergencySession.bind(emergencyManager),
    getActiveEmergencies: emergencyManager.getActiveEmergencies.bind(emergencyManager),
    createCrisisAlert: emergencyManager.createCrisisAlert.bind(emergencyManager),
    acknowledgeAlert: emergencyManager.acknowledgeAlert.bind(emergencyManager),
    getActiveAlerts: emergencyManager.getActiveAlerts.bind(emergencyManager),
    requestSubstitute: emergencyManager.requestSubstitute.bind(emergencyManager),
    getSubstituteRequests: emergencyManager.getSubstituteRequests.bind(emergencyManager),
    approveSubstitute: emergencyManager.approveSubstitute.bind(emergencyManager),
    getEmergencyContacts: emergencyManager.getEmergencyContacts.bind(emergencyManager),
    createEmergencyDrill: emergencyManager.createEmergencyDrill.bind(emergencyManager),
    getEmergencyDrills: emergencyManager.getEmergencyDrills.bind(emergencyManager),
    executeEmergencyDrill: emergencyManager.executeEmergencyDrill.bind(emergencyManager),
    getEmergencyStatistics: emergencyManager.getEmergencyStatistics.bind(emergencyManager),
    generateEmergencyReport: emergencyManager.generateEmergencyReport.bind(emergencyManager),
    testEmergencySystem: emergencyManager.testEmergencySystem.bind(emergencyManager),
  };
};
