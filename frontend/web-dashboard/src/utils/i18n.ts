/**
 * Internationalization (i18n) for Smart Attendance System
 * Multi-language support with RTL support
 */

export type Language = 'en' | 'es' | 'fr' | 'de' | 'zh' | 'ar' | 'hi';

export interface TranslationKeys {
  // Common
  common: {
    loading: string;
    error: string;
    success: string;
    warning: string;
    info: string;
    save: string;
    cancel: string;
    delete: string;
    edit: string;
    view: string;
    search: string;
    filter: string;
    export: string;
    import: string;
    refresh: string;
    close: string;
    back: string;
    next: string;
    previous: string;
    submit: string;
    confirm: string;
    yes: string;
    no: string;
    ok: string;
    retry: string;
    offline: string;
    online: string;
  };

  // Authentication
  auth: {
    login: string;
    logout: string;
    email: string;
    password: string;
    forgotPassword: string;
    resetPassword: string;
    changePassword: string;
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
    loginSuccess: string;
    loginFailed: string;
    logoutSuccess: string;
    sessionExpired: string;
    unauthorized: string;
    accessDenied: string;
    rememberMe: string;
    firstTimeSetup: string;
    completeSetup: string;
  };

  // Navigation
  navigation: {
    dashboard: string;
    users: string;
    rooms: string;
    attendance: string;
    analytics: string;
    profile: string;
    settings: string;
    reports: string;
    help: string;
    home: string;
    admin: string;
    faculty: string;
    student: string;
  };

  // Dashboard
  dashboard: {
    welcome: string;
    overview: string;
    statistics: string;
    quickActions: string;
    recentActivity: string;
    totalUsers: string;
    activeSessions: string;
    todayAttendance: string;
    systemHealth: string;
    notifications: string;
    alerts: string;
    trends: string;
    performance: string;
  };

  // User Management
  users: {
    userList: string;
    addUser: string;
    editUser: string;
    deleteUser: string;
    userStatus: string;
    active: string;
    inactive: string;
    suspended: string;
    name: string;
    email: string;
    role: string;
    department: string;
    section: string;
    registrationNumber: string;
    employeeId: string;
    phone: string;
    address: string;
    onboardStudent: string;
    onboardFaculty: string;
    onboardAdmin: string;
    bulkOperations: string;
    promote: string;
    demote: string;
    exportUsers: string;
    importUsers: string;
  };

  // Room Management
  rooms: {
    roomList: string;
    addRoom: string;
    editRoom: string;
    deleteRoom: string;
    roomName: string;
    capacity: string;
    building: string;
    floor: string;
    boundary: string;
    createBoundary: string;
    rectangleBoundary: string;
    circleBoundary: string;
    polygonBoundary: string;
    validateBoundary: string;
    roomChange: string;
    qrRoomChange: string;
    weeklySwap: string;
    gracePeriod: string;
    transition: string;
    occupancy: string;
    statistics: string;
  };

  // Attendance
  attendance: {
    sessions: string;
    createSession: string;
    startSession: string;
    endSession: string;
    pauseSession: string;
    resumeSession: string;
    activeSession: string;
    sessionHistory: string;
    attendanceRecord: string;
    present: string;
    absent: string;
    late: string;
    earlyLeave: string;
    heartbeat: string;
    realTimeTracking: string;
    gpsTracking: string;
    batteryOptimization: string;
    sensorFusion: string;
    hallPass: string;
    requestHallPass: string;
    approveHallPass: string;
    denyHallPass: string;
    hallPassHistory: string;
    reports: string;
    export: string;
    analytics: string;
  };

  // AI Analytics
  analytics: {
    aiDashboard: string;
    spatialAnalysis: string;
    gpsDrift: string;
    walkOutPrediction: string;
    behaviorPatterns: string;
    anomalyDetection: string;
    riskAssessment: string;
    predictiveAnalytics: string;
    studentProfiles: string;
    confidence: string;
    accuracy: string;
    insights: string;
    recommendations: string;
    alerts: string;
    monitoring: string;
    performance: string;
    trends: string;
    heatmap: string;
  };

  // Settings
  settings: {
    general: string;
    security: string;
    privacy: string;
    notifications: string;
    language: string;
    theme: string;
    appearance: string;
    account: string;
    preferences: string;
    system: string;
    maintenance: string;
    backup: string;
    restore: string;
    logs: string;
    audit: string;
  };

  // Errors
  errors: {
    networkError: string;
    serverError: string;
    validationError: string;
    notFound: string;
    forbidden: string;
    timeout: string;
    unknown: string;
    required: string;
    invalidFormat: string;
    duplicate: string;
    limitExceeded: string;
    maintenance: string;
    offline: string;
    syncFailed: string;
    conflict: string;
  };

  // Success Messages
  success: {
    saved: string;
    updated: string;
    deleted: string;
    created: string;
    uploaded: string;
    downloaded: string;
    exported: string;
    imported: string;
    synced: string;
    completed: string;
    approved: string;
    denied: string;
    sent: string;
    received: string;
  };

  // Time
  time: {
    now: string;
    today: string;
    yesterday: string;
    tomorrow: string;
    thisWeek: string;
    thisMonth: string;
    thisYear: string;
    lastWeek: string;
    lastMonth: string;
    lastYear: string;
    minutes: string;
    hours: string;
    days: string;
    weeks: string;
    months: string;
    years: string;
    ago: string;
    remaining: string;
  };

  // Units
  units: {
    meters: string;
    kilometers: string;
    feet: string;
    miles: string;
    seconds: string;
    minutes: string;
    hours: string;
    celsius: string;
    fahrenheit: string;
    percent: string;
    battery: string;
  };
}

// Translation definitions
export const translations: Record<Language, TranslationKeys> = {
  en: {
    common: {
      loading: 'Loading...',
      error: 'Error',
      success: 'Success',
      warning: 'Warning',
      info: 'Info',
      save: 'Save',
      cancel: 'Cancel',
      delete: 'Delete',
      edit: 'Edit',
      view: 'View',
      search: 'Search',
      filter: 'Filter',
      export: 'Export',
      import: 'Import',
      refresh: 'Refresh',
      close: 'Close',
      back: 'Back',
      next: 'Next',
      previous: 'Previous',
      submit: 'Submit',
      confirm: 'Confirm',
      yes: 'Yes',
      no: 'No',
      ok: 'OK',
      retry: 'Retry',
      offline: 'Offline',
      online: 'Online',
    },
    auth: {
      login: 'Login',
      logout: 'Logout',
      email: 'Email',
      password: 'Password',
      forgotPassword: 'Forgot Password?',
      resetPassword: 'Reset Password',
      changePassword: 'Change Password',
      currentPassword: 'Current Password',
      newPassword: 'New Password',
      confirmPassword: 'Confirm Password',
      loginSuccess: 'Login successful',
      loginFailed: 'Login failed',
      logoutSuccess: 'Logout successful',
      sessionExpired: 'Session expired',
      unauthorized: 'Unauthorized access',
      accessDenied: 'Access denied',
      rememberMe: 'Remember me',
      firstTimeSetup: 'First Time Setup',
      completeSetup: 'Complete Setup',
    },
    navigation: {
      dashboard: 'Dashboard',
      users: 'Users',
      rooms: 'Rooms',
      attendance: 'Attendance',
      analytics: 'Analytics',
      profile: 'Profile',
      settings: 'Settings',
      reports: 'Reports',
      help: 'Help',
      home: 'Home',
      admin: 'Admin',
      faculty: 'Faculty',
      student: 'Student',
    },
    dashboard: {
      welcome: 'Welcome',
      overview: 'Overview',
      statistics: 'Statistics',
      quickActions: 'Quick Actions',
      recentActivity: 'Recent Activity',
      totalUsers: 'Total Users',
      activeSessions: 'Active Sessions',
      todayAttendance: "Today's Attendance",
      systemHealth: 'System Health',
      notifications: 'Notifications',
      alerts: 'Alerts',
      trends: 'Trends',
      performance: 'Performance',
    },
    users: {
      userList: 'User List',
      addUser: 'Add User',
      editUser: 'Edit User',
      deleteUser: 'Delete User',
      userStatus: 'User Status',
      active: 'Active',
      inactive: 'Inactive',
      suspended: 'Suspended',
      name: 'Name',
      email: 'Email',
      role: 'Role',
      department: 'Department',
      section: 'Section',
      registrationNumber: 'Registration Number',
      employeeId: 'Employee ID',
      phone: 'Phone',
      address: 'Address',
      onboardStudent: 'Onboard Student',
      onboardFaculty: 'Onboard Faculty',
      onboardAdmin: 'Onboard Admin',
      bulkOperations: 'Bulk Operations',
      promote: 'Promote',
      demote: 'Demote',
      exportUsers: 'Export Users',
      importUsers: 'Import Users',
    },
    rooms: {
      roomList: 'Room List',
      addRoom: 'Add Room',
      editRoom: 'Edit Room',
      deleteRoom: 'Delete Room',
      roomName: 'Room Name',
      capacity: 'Capacity',
      building: 'Building',
      floor: 'Floor',
      boundary: 'Boundary',
      createBoundary: 'Create Boundary',
      rectangleBoundary: 'Rectangle Boundary',
      circleBoundary: 'Circle Boundary',
      polygonBoundary: 'Polygon Boundary',
      validateBoundary: 'Validate Boundary',
      roomChange: 'Room Change',
      qrRoomChange: 'QR Room Change',
      weeklySwap: 'Weekly Swap',
      gracePeriod: 'Grace Period',
      transition: 'Transition',
      occupancy: 'Occupancy',
      statistics: 'Statistics',
    },
    attendance: {
      sessions: 'Sessions',
      createSession: 'Create Session',
      startSession: 'Start Session',
      endSession: 'End Session',
      pauseSession: 'Pause Session',
      resumeSession: 'Resume Session',
      activeSession: 'Active Session',
      sessionHistory: 'Session History',
      attendanceRecord: 'Attendance Record',
      present: 'Present',
      absent: 'Absent',
      late: 'Late',
      earlyLeave: 'Early Leave',
      heartbeat: 'Heartbeat',
      realTimeTracking: 'Real-time Tracking',
      gpsTracking: 'GPS Tracking',
      batteryOptimization: 'Battery Optimization',
      sensorFusion: 'Sensor Fusion',
      hallPass: 'Hall Pass',
      requestHallPass: 'Request Hall Pass',
      approveHallPass: 'Approve Hall Pass',
      denyHallPass: 'Deny Hall Pass',
      hallPassHistory: 'Hall Pass History',
      reports: 'Reports',
      export: 'Export',
      analytics: 'Analytics',
    },
    analytics: {
      aiDashboard: 'AI Dashboard',
      spatialAnalysis: 'Spatial Analysis',
      gpsDrift: 'GPS Drift',
      walkOutPrediction: 'Walk-out Prediction',
      behaviorPatterns: 'Behavior Patterns',
      anomalyDetection: 'Anomaly Detection',
      riskAssessment: 'Risk Assessment',
      predictiveAnalytics: 'Predictive Analytics',
      studentProfiles: 'Student Profiles',
      confidence: 'Confidence',
      accuracy: 'Accuracy',
      insights: 'Insights',
      recommendations: 'Recommendations',
      alerts: 'Alerts',
      monitoring: 'Monitoring',
      performance: 'Performance',
      trends: 'Trends',
      heatmap: 'Heatmap',
    },
    settings: {
      general: 'General',
      security: 'Security',
      privacy: 'Privacy',
      notifications: 'Notifications',
      language: 'Language',
      theme: 'Theme',
      appearance: 'Appearance',
      account: 'Account',
      preferences: 'Preferences',
      system: 'System',
      maintenance: 'Maintenance',
      backup: 'Backup',
      restore: 'Restore',
      logs: 'Logs',
      audit: 'Audit',
    },
    errors: {
      networkError: 'Network error',
      serverError: 'Server error',
      validationError: 'Validation error',
      notFound: 'Not found',
      forbidden: 'Forbidden',
      timeout: 'Request timeout',
      unknown: 'Unknown error',
      required: 'This field is required',
      invalidFormat: 'Invalid format',
      duplicate: 'Duplicate entry',
      limitExceeded: 'Limit exceeded',
      maintenance: 'System under maintenance',
      offline: 'You are offline',
      syncFailed: 'Sync failed',
      conflict: 'Data conflict',
    },
    success: {
      saved: 'Saved successfully',
      updated: 'Updated successfully',
      deleted: 'Deleted successfully',
      created: 'Created successfully',
      uploaded: 'Uploaded successfully',
      downloaded: 'Downloaded successfully',
      exported: 'Exported successfully',
      imported: 'Imported successfully',
      synced: 'Synced successfully',
      completed: 'Completed successfully',
      approved: 'Approved successfully',
      denied: 'Denied successfully',
      sent: 'Sent successfully',
      received: 'Received successfully',
    },
    time: {
      now: 'Now',
      today: 'Today',
      yesterday: 'Yesterday',
      tomorrow: 'Tomorrow',
      thisWeek: 'This Week',
      thisMonth: 'This Month',
      thisYear: 'This Year',
      lastWeek: 'Last Week',
      lastMonth: 'Last Month',
      lastYear: 'Last Year',
      minutes: 'minutes',
      hours: 'hours',
      days: 'days',
      weeks: 'weeks',
      months: 'months',
      years: 'years',
      ago: 'ago',
      remaining: 'remaining',
    },
    units: {
      meters: 'meters',
      kilometers: 'kilometers',
      feet: 'feet',
      miles: 'miles',
      seconds: 'seconds',
      minutes: 'minutes',
      hours: 'hours',
      celsius: 'Celsius',
      fahrenheit: 'Fahrenheit',
      percent: 'percent',
      battery: 'battery',
    },
  },
  // Add more languages as needed
  es: {
    // Spanish translations would go here
  } as TranslationKeys,
  fr: {
    // French translations would go here
  } as TranslationKeys,
  de: {
    // German translations would go here
  } as TranslationKeys,
  zh: {
    // Chinese translations would go here
  } as TranslationKeys,
  ar: {
    // Arabic translations would go here
  } as TranslationKeys,
  hi: {
    // Hindi translations would go here
  } as TranslationKeys,
};

/**
 * Internationalization Manager
 */
export class I18nManager {
  private static instance: I18nManager;
  private currentLanguage: Language = 'en';
  private storageKey = 'smart-attendance-language';

  private constructor() {
    this.loadSavedLanguage();
  }

  static getInstance(): I18nManager {
    if (!I18nManager.instance) {
      I18nManager.instance = new I18nManager();
    }
    return I18nManager.instance;
  }

  /**
   * Load saved language from localStorage
   */
  private loadSavedLanguage(): void {
    try {
      const saved = localStorage.getItem(this.storageKey);
      if (saved && this.isValidLanguage(saved)) {
        this.currentLanguage = saved as Language;
      } else {
        // Detect browser language
        const browserLang = navigator.language.split('-')[0];
        if (this.isValidLanguage(browserLang)) {
          this.currentLanguage = browserLang as Language;
        }
      }
    } catch (error) {
      console.error('Error loading saved language:', error);
    }
  }

  /**
   * Check if language is valid
   */
  private isValidLanguage(lang: string): lang is Language {
    return ['en', 'es', 'fr', 'de', 'zh', 'ar', 'hi'].includes(lang);
  }

  /**
   * Get current language
   */
  getCurrentLanguage(): Language {
    return this.currentLanguage;
  }

  /**
   * Set current language
   */
  setLanguage(language: Language): void {
    if (this.isValidLanguage(language)) {
      this.currentLanguage = language;
      this.saveLanguage();
      this.updateDocumentLanguage();
    }
  }

  /**
   * Save language to localStorage
   */
  private saveLanguage(): void {
    try {
      localStorage.setItem(this.storageKey, this.currentLanguage);
    } catch (error) {
      console.error('Error saving language:', error);
    }
  }

  /**
   * Update document language and direction
   */
  private updateDocumentLanguage(): void {
    document.documentElement.lang = this.currentLanguage;
    
    // Set direction for RTL languages
    if (this.isRTL(this.currentLanguage)) {
      document.documentElement.dir = 'rtl';
    } else {
      document.documentElement.dir = 'ltr';
    }
  }

  /**
   * Check if language is RTL
   */
  private isRTL(language: Language): boolean {
    return ['ar'].includes(language);
  }

  /**
   * Get translation for key
   */
  t(key: string): string {
    const keys = key.split('.');
    let value: unknown = translations[this.currentLanguage];

    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        // @ts-expect-error type override for strict mode
        value = value[k];
      } else {
        // Fallback to English
        value = translations.en;
        for (const fallbackKey of keys) {
          if (value && typeof value === 'object' && fallbackKey in value) {
            // @ts-expect-error type override for strict mode
            value = value[fallbackKey];
          } else {
            return key; // Return key if not found
          }
        }
        break;
      }
    }

    return typeof value === 'string' ? value : key;
  }

  /**
   * Get available languages
   */
  getAvailableLanguages(): Array<{ code: Language; name: string; nativeName: string; rtl: boolean }> {
    return [
      { code: 'en', name: 'English', nativeName: 'English', rtl: false },
      { code: 'es', name: 'Spanish', nativeName: 'Español', rtl: false },
      { code: 'fr', name: 'French', nativeName: 'Français', rtl: false },
      { code: 'de', name: 'German', nativeName: 'Deutsch', rtl: false },
      { code: 'zh', name: 'Chinese', nativeName: '中文', rtl: false },
      { code: 'ar', name: 'Arabic', nativeName: 'العربية', rtl: true },
      { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', rtl: false },
    ];
  }

  /**
   * Format date with locale
   */
  formatDate(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return dateObj.toLocaleDateString(this.currentLanguage, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      ...options,
    });
  }

  /**
   * Format time with locale
   */
  formatTime(date: Date | string, options?: Intl.DateTimeFormatOptions): string {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return dateObj.toLocaleTimeString(this.currentLanguage, {
      hour: '2-digit',
      minute: '2-digit',
      ...options,
    });
  }

  /**
   * Format number with locale
   */
  formatNumber(number: number, options?: Intl.NumberFormatOptions): string {
    return number.toLocaleString(this.currentLanguage, options);
  }

  /**
   * Format currency with locale
   */
  formatCurrency(amount: number, currency: string = 'USD'): string {
    return amount.toLocaleString(this.currentLanguage, {
      style: 'currency',
      currency,
    });
  }

  /**
   * Get plural form for count
   */
  pluralize(count: number, singular: string, plural?: string): string {
    if (count === 1) return `${count} ${singular}`;
    return `${count} ${plural || singular + 's'}`;
  }
}

// Export singleton instance
export const i18n = I18nManager.getInstance();

// Export hook for React components
export const useTranslation = () => {
  return {
    t: i18n.t.bind(i18n),
    currentLanguage: i18n.getCurrentLanguage(),
    setLanguage: i18n.setLanguage.bind(i18n),
    availableLanguages: i18n.getAvailableLanguages(),
    formatDate: i18n.formatDate.bind(i18n),
    formatTime: i18n.formatTime.bind(i18n),
    formatNumber: i18n.formatNumber.bind(i18n),
    formatCurrency: i18n.formatCurrency.bind(i18n),
    pluralize: i18n.pluralize.bind(i18n),
    isRTL: ['ar'].includes(i18n.getCurrentLanguage()),
  };
};
