/**
 * Comprehensive notification system for the smart attendance system
 */

import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

// Notification types
export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message?: string;
  duration?: number;
  persistent?: boolean;
  actions?: NotificationAction[];
  timestamp: Date;
}

export interface NotificationAction {
  label: string;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
}

// Notification context
interface NotificationContextType {
  notifications: Notification[];
  addNotification: (notification: Omit<Notification, 'id' | 'timestamp'>) => string;
  removeNotification: (id: string) => void;
  clearNotifications: () => void;
  updateNotification: (id: string, updates: Partial<Notification>) => void;
}

const NotificationContext = createContext<NotificationContextType | null>(null);

// Notification provider
export const NotificationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);

  const addNotification = useCallback((notification: Omit<Notification, 'id' | 'timestamp'>) => {
    const id = Math.random().toString(36).substring(7);
    const newNotification: Notification = {
      ...notification,
      id,
      timestamp: new Date(),
      duration: notification.duration ?? 5000,
    };

    setNotifications(prev => [...prev, newNotification]);

    // Auto-remove notification after duration (if not persistent)
    if (!notification.persistent && notification.duration !== 0) {
      setTimeout(() => {
        removeNotification(id);
      }, notification.duration ?? 5000);
    }

    return id;
  }, []);

  const removeNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(notification => notification.id !== id));
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const updateNotification = useCallback((id: string, updates: Partial<Notification>) => {
    setNotifications(prev =>
      prev.map(notification =>
        notification.id === id ? { ...notification, ...updates } : notification
      )
    );
  }, []);

  return React.createElement(
    NotificationContext.Provider,
    {
      value: {
        notifications,
        addNotification,
        removeNotification,
        clearNotifications,
        updateNotification,
      },
    },
    children
  );
};

// Hook to use notifications
export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};

// Convenience functions for different notification types
export const useNotificationActions = () => {
  const { addNotification } = useNotifications();

  return {
    success: (title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) =>
      addNotification({ type: 'success', title, message, ...options }),
    
    error: (title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) =>
      addNotification({ type: 'error', title, message, persistent: true, ...options }),
    
    warning: (title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) =>
      addNotification({ type: 'warning', title, message, ...options }),
    
    info: (title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) =>
      addNotification({ type: 'info', title, message, ...options }),
  };
};

// Notification service for non-React usage
export class NotificationService {
  private static listeners: ((notifications: Notification[]) => void)[] = [];
  private static notifications: Notification[] = [];

  static subscribe(listener: (notifications: Notification[]) => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private static notifyListeners() {
    this.listeners.forEach(listener => listener([...this.notifications]));
  }

  static add(notification: Omit<Notification, 'id' | 'timestamp'>): string {
    const id = Math.random().toString(36).substring(7);
    const newNotification: Notification = {
      ...notification,
      id,
      timestamp: new Date(),
    };

    this.notifications.push(newNotification);
    this.notifyListeners();

    // Auto-remove notification after duration (if not persistent)
    if (!notification.persistent && notification.duration !== 0) {
      setTimeout(() => {
        this.remove(id);
      }, notification.duration ?? 5000);
    }

    return id;
  }

  static remove(id: string) {
    this.notifications = this.notifications.filter(n => n.id !== id);
    this.notifyListeners();
  }

  static clear() {
    this.notifications = [];
    this.notifyListeners();
  }

  static update(id: string, updates: Partial<Notification>) {
    this.notifications = this.notifications.map(notification =>
      notification.id === id ? { ...notification, ...updates } : notification
    );
    this.notifyListeners();
  }

  static getAll(): Notification[] {
    return [...this.notifications];
  }

  // Convenience methods
  static success(title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) {
    return this.add({ type: 'success', title, message, ...options });
  }

  static error(title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) {
    return this.add({ type: 'error', title, message, persistent: true, ...options });
  }

  static warning(title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) {
    return this.add({ type: 'warning', title, message, ...options });
  }

  static info(title: string, message?: string, options?: Partial<Omit<Notification, 'id' | 'timestamp' | 'type'>>) {
    return this.add({ type: 'info', title, message, ...options });
  }
}

// Browser notification API wrapper
export class BrowserNotifications {
  private static isSupported = 'Notification' in window;
  private static permission: NotificationPermission = 'default';

  static async requestPermission(): Promise<boolean> {
    if (!this.isSupported) return false;

    try {
      this.permission = await Notification.requestPermission();
      return this.permission === 'granted';
    } catch (error) {
      console.error('Failed to request notification permission:', error);
      return false;
    }
  }

  static async show(
    title: string,
    options?: NotificationOptions & {
      onClick?: () => void;
    }
  ): Promise<unknown> {
    if (!this.isSupported || this.permission !== 'granted') {
      return null;
    }

    try {
      const browserNotification = new Notification(title, {
        icon: '/favicon.ico',
        badge: '/favicon.ico',
        ...options,
      });

      if (options?.onClick) {
        browserNotification.onclick = options.onClick;
      }

      return browserNotification;
    } catch (error) {
      console.error('Failed to show browser notification:', error);
      return null;
    }
  }

  static isPermissionGranted(): boolean {
    return this.isSupported && this.permission === 'granted';
  }

  static async checkPermission(): Promise<NotificationPermission> {
    if (!this.isSupported) return 'denied';
    
    this.permission = await Notification.requestPermission();
    return this.permission;
  }
}

// Notification templates for common scenarios
export const NotificationTemplates = {
  // User management
  userCreated: () => ({
    type: 'success' as const,
    title: 'User Created',
    message: 'User has been successfully created.',
  }),

  userUpdated: () => ({
    type: 'success' as const,
    title: 'User Updated',
    message: 'User information has been updated.',
  }),

  userDeleted: () => ({
    type: 'warning' as const,
    title: 'User Deleted',
    message: 'User has been removed from the system.',
  }),

  // Room management
  roomCreated: () => ({
    type: 'success' as const,
    title: 'Room Created',
    message: 'Room has been successfully created.',
  }),

  roomUpdated: () => ({
    type: 'success' as const,
    title: 'Room Updated',
    message: 'Room information has been updated.',
  }),

  roomDeleted: () => ({
    type: 'warning' as const,
    title: 'Room Deleted',
    message: 'Room has been removed from the system.',
  }),

  // Attendance
  sessionStarted: () => ({
    type: 'success' as const,
    title: 'Session Started',
    message: 'Attendance session has been started.',
  }),

  sessionEnded: () => ({
    type: 'info' as const,
    title: 'Session Ended',
    message: 'Attendance session has been ended.',
  }),

  attendanceRecorded: (studentName: string) => ({
    type: 'success' as const,
    title: 'Attendance Recorded',
    message: `${studentName}'s attendance has been recorded.`,
  }),

  // Hall pass
  hallPassApproved: (studentName: string) => ({
    type: 'success' as const,
    title: 'Hall Pass Approved',
    message: `${studentName}'s hall pass has been approved.`,
  }),

  hallPassDenied: (studentName: string) => ({
    type: 'warning' as const,
    title: 'Hall Pass Denied',
    message: `${studentName}'s hall pass request has been denied.`,
  }),

  // AI Analytics
  anomalyDetected: (type: string) => ({
    type: 'warning' as const,
    title: 'Anomaly Detected',
    message: `${type} anomaly has been detected.`,
    persistent: true,
  }),

  walkOutRisk: (studentName: string, risk: string) => ({
    type: 'warning' as const,
    title: 'Walk-Out Risk',
    message: `${studentName} shows ${risk} walk-out risk.`,
    persistent: true,
  }),

  // System
  networkError: () => ({
    type: 'error' as const,
    title: 'Network Error',
    message: 'Please check your internet connection.',
    persistent: true,
  }),

  unauthorized: () => ({
    type: 'error' as const,
    title: 'Unauthorized',
    message: 'You are not authorized to perform this action.',
    persistent: true,
  }),

  serverError: () => ({
    type: 'error' as const,
    title: 'Server Error',
    message: 'An unexpected error occurred. Please try again.',
    persistent: true,
  }),

  dataSaved: () => ({
    type: 'success' as const,
    title: 'Data Saved',
    message: 'Your changes have been saved successfully.',
  }),

  dataExported: (format: string) => ({
    type: 'success' as const,
    title: 'Data Exported',
    message: `Data has been exported as ${format}.`,
  }),
} as const;
