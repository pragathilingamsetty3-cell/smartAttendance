/**
 * Offline Manager for Smart Attendance System
 * Handles offline data storage, sync, and conflict resolution
 */

import { EnhancedHeartbeatPing, AttendanceRecord } from '../types/attendance';

// Offline storage interface
export interface OfflineStorage {
  attendanceRecords: OfflineAttendanceRecord[];
  heartbeatData: OfflineHeartbeatRecord[];
  syncQueue: SyncOperation[];
  lastSyncTime: string;
  deviceId: string;
}

export interface OfflineAttendanceRecord {
  id: string;
  sessionId: string;
  studentId: string;
  timestamp: string;
  latitude: number;
  longitude: number;
  status: 'PRESENT' | 'ABSENT' | 'LATE';
  deviceId: string;
  synced: boolean;
  conflictResolution?: 'KEEP_LOCAL' | 'KEEP_SERVER' | 'MERGE';
}

export interface OfflineHeartbeatRecord {
  id: string;
  sessionId: string;
  studentId: string;
  timestamp: string;
  latitude: number;
  longitude: number;
  stepCount: number;
  accelerationX: number;
  accelerationY: number;
  accelerationZ: number;
  isDeviceMoving: boolean;
  deviceFingerprint: string;
  batteryLevel: number;
  isCharging: boolean;
  isScreenOn: boolean;
  deviceState: 'STATIONARY' | 'MOVING' | 'WALKING';
  nextHeartbeatInterval: number;
  synced: boolean;
}

export interface SyncOperation {
  id: string;
  type: 'ATTENDANCE' | 'HEARTBEAT' | 'HALL_PASS';
  data: unknown;
  timestamp: string;
  retryCount: number;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  error?: string;
}

export interface SyncResult {
  success: boolean;
  syncedRecords: number;
  failedRecords: number;
  conflicts: SyncConflict[];
  errors: string[];
}

export interface SyncConflict {
  id: string;
  type: 'ATTENDANCE' | 'HEARTBEAT';
  localData: unknown;
  serverData: unknown;
  timestamp: string;
  resolution?: 'KEEP_LOCAL' | 'KEEP_SERVER' | 'MERGE';
}

/**
 * Offline Manager Class
 */
export class OfflineManager {
  private static instance: OfflineManager;
  private storageKey = 'smart-attendance-offline';
  private isOnline: boolean = navigator.onLine;
  private syncInProgress: boolean = false;
  private syncInterval: NodeJS.Timeout | null = null;

  private constructor() {
    this.initializeEventListeners();
    this.startPeriodicSync();
  }

  static getInstance(): OfflineManager {
    if (!OfflineManager.instance) {
      OfflineManager.instance = new OfflineManager();
    }
    return OfflineManager.instance;
  }

  /**
   * Initialize event listeners for online/offline detection
   */
  private initializeEventListeners(): void {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.triggerSync();
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
      this.stopPeriodicSync();
    });
  }

  /**
   * Get offline storage data
   */
  private getStorage(): OfflineStorage {
    try {
      const data = localStorage.getItem(this.storageKey);
      if (data) {
        return JSON.parse(data);
      }
    } catch (error) {
      console.error('Error reading offline storage:', error);
    }
    
    return {
      attendanceRecords: [],
      heartbeatData: [],
      syncQueue: [],
      lastSyncTime: new Date().toISOString(),
      deviceId: this.generateDeviceId(),
    };
  }

  /**
   * Save data to offline storage
   */
  private saveStorage(storage: OfflineStorage): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(storage));
    } catch (error) {
      console.error('Error saving to offline storage:', error);
    }
  }

  /**
   * Generate unique device ID
   */
  private generateDeviceId(): string {
    const existing = this.getStorage();
    if (existing.deviceId) {
      return existing.deviceId;
    }
    
    return 'device_' + Math.random().toString(36).substring(2) + Date.now();
  }

  /**
   * Store attendance record offline
   */
  public storeAttendanceRecord(record: Omit<OfflineAttendanceRecord, 'id' | 'synced'>): void {
    const storage = this.getStorage();
    const offlineRecord: OfflineAttendanceRecord = {
      ...record,
      id: 'attendance_' + Date.now() + '_' + Math.random().toString(36).substring(2),
      synced: false,
    };

    storage.attendanceRecords.push(offlineRecord);
    this.saveStorage(storage);

    // Add to sync queue
    this.addToSyncQueue({
      id: offlineRecord.id,
      type: 'ATTENDANCE',
      data: offlineRecord,
      timestamp: new Date().toISOString(),
      retryCount: 0,
      status: 'PENDING',
    });
  }

  /**
   * Store heartbeat data offline
   */
  public storeHeartbeatData(heartbeat: Omit<OfflineHeartbeatRecord, 'id' | 'synced'>): void {
    const storage = this.getStorage();
    const offlineHeartbeat: OfflineHeartbeatRecord = {
      ...heartbeat,
      id: 'heartbeat_' + Date.now() + '_' + Math.random().toString(36).substring(2),
      synced: false,
    };

    storage.heartbeatData.push(offlineHeartbeat);
    this.saveStorage(storage);

    // Add to sync queue
    this.addToSyncQueue({
      id: offlineHeartbeat.id,
      type: 'HEARTBEAT',
      data: offlineHeartbeat,
      timestamp: new Date().toISOString(),
      retryCount: 0,
      status: 'PENDING',
    });
  }

  /**
   * Add operation to sync queue
   */
  private addToSyncQueue(operation: SyncOperation): void {
    const storage = this.getStorage();
    storage.syncQueue.push(operation);
    this.saveStorage(storage);

    if (this.isOnline) {
      this.triggerSync();
    }
  }

  /**
   * Trigger sync process
   */
  public async triggerSync(): Promise<SyncResult> {
    if (!this.isOnline || this.syncInProgress) {
      return {
        success: false,
        syncedRecords: 0,
        failedRecords: 0,
        conflicts: [],
        errors: ['Not online or sync already in progress'],
      };
    }

    this.syncInProgress = true;
    const storage = this.getStorage();
    const result: SyncResult = {
      success: true,
      syncedRecords: 0,
      failedRecords: 0,
      conflicts: [],
      errors: [],
    };

    try {
      // Process sync queue
      for (const operation of storage.syncQueue) {
        if (operation.status === 'COMPLETED') continue;

        try {
          await this.syncOperation(operation);
          operation.status = 'COMPLETED';
          result.syncedRecords++;
        } catch (error) {
          operation.status = 'FAILED';
          operation.error = error instanceof Error ? (error as Error).message : 'Unknown error';
          operation.retryCount++;
          result.failedRecords++;
          result.errors.push(operation.error);
        }
      }

      // Clean up completed operations
      storage.syncQueue = storage.syncQueue.filter(op => op.status !== 'COMPLETED');
      
      // Update last sync time
      storage.lastSyncTime = new Date().toISOString();
      
      this.saveStorage(storage);

    } catch (error) {
      result.success = false;
      result.errors.push(error instanceof Error ? (error as Error).message : 'Sync failed');
    } finally {
      this.syncInProgress = false;
    }

    return result;
  }

  /**
   * Sync individual operation
   */
  private async syncOperation(operation: SyncOperation): Promise<void> {
    // This would integrate with your API client
    // For now, we'll simulate the sync
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // In a real implementation, you would:
    // 1. Send data to server
    // 2. Handle conflicts
    // 3. Update local storage with server response
  }

  /**
   * Start periodic sync
   */
  private startPeriodicSync(): void {
    if (this.syncInterval) return;
    
    this.syncInterval = setInterval(() => {
      if (this.isOnline && !this.syncInProgress) {
        this.triggerSync();
      }
    }, 60000); // Sync every minute when online
  }

  /**
   * Stop periodic sync
   */
  private stopPeriodicSync(): void {
    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = null;
    }
  }

  /**
   * Get offline status
   */
  public getOfflineStatus(): {
    isOnline: boolean;
    pendingRecords: number;
    lastSyncTime: string;
    syncInProgress: boolean;
  } {
    const storage = this.getStorage();
    return {
      isOnline: this.isOnline,
      pendingRecords: storage.syncQueue.filter(op => op.status === 'PENDING').length,
      lastSyncTime: storage.lastSyncTime,
      syncInProgress: this.syncInProgress,
    };
  }

  /**
   * Get offline statistics
   */
  public getOfflineStatistics(): {
    totalAttendanceRecords: number;
    syncedAttendanceRecords: number;
    totalHeartbeatRecords: number;
    syncedHeartbeatRecords: number;
    pendingSyncOperations: number;
    failedSyncOperations: number;
  } {
    const storage = this.getStorage();
    
    return {
      totalAttendanceRecords: storage.attendanceRecords.length,
      syncedAttendanceRecords: storage.attendanceRecords.filter(r => r.synced).length,
      totalHeartbeatRecords: storage.heartbeatData.length,
      syncedHeartbeatRecords: storage.heartbeatData.filter(r => r.synced).length,
      pendingSyncOperations: storage.syncQueue.filter(op => op.status === 'PENDING').length,
      failedSyncOperations: storage.syncQueue.filter(op => op.status === 'FAILED').length,
    };
  }

  /**
   * Clear all offline data
   */
  public clearOfflineData(): void {
    try {
      localStorage.removeItem(this.storageKey);
    } catch (error) {
      console.error('Error clearing offline data:', error);
    }
  }

  /**
   * Export offline data for backup
   */
  public exportOfflineData(): string {
    const storage = this.getStorage();
    return JSON.stringify(storage, null, 2);
  }

  /**
   * Import offline data from backup
   */
  public importOfflineData(data: string): boolean {
    try {
      const storage = JSON.parse(data) as OfflineStorage;
      this.saveStorage(storage);
      return true;
    } catch (error) {
      console.error('Error importing offline data:', error);
      return false;
    }
  }

  /**
   * Resolve sync conflicts
   */
  public resolveConflict(conflictId: string, resolution: 'KEEP_LOCAL' | 'KEEP_SERVER' | 'MERGE'): void {
    const storage = this.getStorage();
    const conflict = storage.syncQueue.find(op => op.id === conflictId);
    
    if (conflict) {
      conflict.status = 'COMPLETED';
      this.saveStorage(storage);
    }
  }
}

// Export singleton instance
export const offlineManager = OfflineManager.getInstance();
