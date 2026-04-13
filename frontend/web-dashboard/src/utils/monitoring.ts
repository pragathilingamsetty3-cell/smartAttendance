/**
 * Production Monitoring for Smart Attendance System
 * Performance monitoring, error tracking, and analytics
 */

export interface PerformanceMetrics {
  // Core Web Vitals
  fcp: number; // First Contentful Paint
  lcp: number; // Largest Contentful Paint
  fid: number; // First Input Delay
  cls: number; // Cumulative Layout Shift
  ttfb: number; // Time to First Byte
  
  // Custom Metrics
  pageLoadTime: number;
  apiResponseTime: number;
  renderTime: number;
  memoryUsage: number;
  bundleSize: number;
  
  // User Metrics
  sessionDuration: number;
  pageViews: number;
  bounceRate: number;
  errorRate: number;
}

export interface ErrorInfo {
  message: string;
  stack?: string;
  source: string;
  line?: number;
  column?: number;
  timestamp: string;
  userAgent: string;
  url: string;
  userId?: string;
  sessionId: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  context?: Record<string, unknown>;
}

export interface UserAnalytics {
  userId?: string;
  sessionId: string;
  timestamp: string;
  event: string;
  properties: Record<string, unknown>;
  page: string;
  referrer: string;
  userAgent: string;
  screenSize: string;
  viewport: string;
  connection: string;
  battery?: number;
  location?: {
    latitude: number;
    longitude: number;
  };
}

export interface SystemHealth {
  status: 'healthy' | 'warning' | 'critical';
  uptime: number;
  memoryUsage: number;
  cpuUsage: number;
  diskUsage: number;
  networkLatency: number;
  errorRate: number;
  activeUsers: number;
  apiResponseTime: number;
  databaseConnections: number;
  cacheHitRate: number;
}

/**
 * Monitoring Manager
 */
export class MonitoringManager {
  private static instance: MonitoringManager;
  private sessionId: string;
  private startTime: number;
  private metrics: PerformanceMetrics;
  private errors: ErrorInfo[] = [];
  private analytics: UserAnalytics[] = [];
  private observers: PerformanceObserver[] = [];
  private config: MonitoringConfig;

  private constructor(config: MonitoringConfig = {}) {
    this.config = {
      enablePerformanceMonitoring: true,
      enableErrorTracking: true,
      enableUserAnalytics: true,
      enableSystemHealth: true,
      samplingRate: 1.0,
      maxErrors: 100,
      maxAnalytics: 1000,
      apiEndpoint: '/api/v1/monitoring',
      ...config,
    };
    
    this.sessionId = this.generateSessionId();
    this.startTime = Date.now();
    this.metrics = this.initializeMetrics();
    
    this.initializeMonitoring();
  }

  static getInstance(config?: MonitoringConfig): MonitoringManager {
    if (!MonitoringManager.instance) {
      MonitoringManager.instance = new MonitoringManager(config);
    }
    return MonitoringManager.instance;
  }

  /**
   * Generate unique session ID
   */
  private generateSessionId(): string {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2);
  }

  /**
   * Initialize metrics
   */
  private initializeMetrics(): PerformanceMetrics {
    return {
      fcp: 0,
      lcp: 0,
      fid: 0,
      cls: 0,
      ttfb: 0,
      pageLoadTime: 0,
      apiResponseTime: 0,
      renderTime: 0,
      memoryUsage: 0,
      bundleSize: 0,
      sessionDuration: 0,
      pageViews: 0,
      bounceRate: 0,
      errorRate: 0,
    };
  }

  /**
   * Initialize monitoring systems
   */
  private initializeMonitoring(): void {
    if (this.config.enablePerformanceMonitoring) {
      this.initializePerformanceMonitoring();
    }
    
    if (this.config.enableErrorTracking) {
      this.initializeErrorTracking();
    }
    
    if (this.config.enableUserAnalytics) {
      this.initializeUserAnalytics();
    }
    
    // Track page visibility changes
    this.trackPageVisibility();
    
    // Track page unload
    this.trackPageUnload();
  }

  /**
   * Initialize performance monitoring
   */
  private initializePerformanceMonitoring(): void {
    try {
      // Core Web Vitals
      if ('PerformanceObserver' in window) {
        // First Contentful Paint
        const fcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const fcp = entries.find(entry => entry.name === 'first-contentful-paint');
          if (fcp) {
            this.metrics.fcp = fcp.startTime;
          }
        });
        fcpObserver.observe({ entryTypes: ['paint'] });
        this.observers.push(fcpObserver);

        // Largest Contentful Paint
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lcp = entries[entries.length - 1];
          this.metrics.lcp = lcp.startTime;
        });
        lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.push(lcpObserver);

        // Cumulative Layout Shift
        const clsObserver = new PerformanceObserver((list) => {
          let clsValue = 0;
          for (const entry of list.getEntries()) {
            // @ts-expect-error type override for strict mode
            if (!(entry as Record<string, unknown>).hadRecentInput) {
              // @ts-expect-error type override for strict mode
              clsValue += (entry as Record<string, unknown>).value;
            }
          }
          this.metrics.cls += clsValue;
        });
        clsObserver.observe({ entryTypes: ['layout-shift'] });
        this.observers.push(clsObserver);

        // First Input Delay
        const fidObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            // @ts-expect-error type override for strict mode
            this.metrics.fid = (entry as Record<string, unknown>).processingStart - entry.startTime;
          }
        });
        fidObserver.observe({ entryTypes: ['first-input'] });
        this.observers.push(fidObserver);
      }

      // Page Load Time
      window.addEventListener('load', () => {
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        if (navigation) {
          this.metrics.pageLoadTime = navigation.loadEventEnd - navigation.fetchStart;
          this.metrics.ttfb = navigation.responseStart - navigation.requestStart;
        }
      });

      // Memory Usage
      if ('memory' in performance) {
        const memory = (performance as Record<string, unknown>).memory;
        // @ts-expect-error type override for strict mode
        this.metrics.memoryUsage = memory.usedJSHeapSize;
      }

    } catch (error) {
      console.error('Error initializing performance monitoring:', error);
    }
  }

  /**
   * Initialize error tracking
   */
  private initializeErrorTracking(): void {
    // Global error handler
    window.addEventListener('error', (event) => {
      this.trackError({
        message: event.message,
        source: event.filename,
        line: event.lineno,
        column: event.colno,
        stack: event.error?.stack,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        sessionId: this.sessionId,
        severity: 'high',
      });
    });

    // Unhandled promise rejections
    window.addEventListener('unhandledrejection', (event) => {
      this.trackError({
        message: event.reason?.message || 'Unhandled Promise Rejection',
        stack: event.reason?.stack,
        source: 'Promise',
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        sessionId: this.sessionId,
        severity: 'medium',
        context: { reason: event.reason },
      });
    });
  }

  /**
   * Initialize user analytics
   */
  private initializeUserAnalytics(): void {
    // Track page view
    this.trackEvent('page_view', {
      page: window.location.pathname,
      referrer: document.referrer,
      title: document.title,
    });

    // Track user interactions
    this.trackUserInteractions();
  }

  /**
   * Track page visibility changes
   */
  private trackPageVisibility(): void {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.trackEvent('page_hidden', {
          duration: Date.now() - this.startTime,
        });
      } else {
        this.trackEvent('page_visible');
        this.startTime = Date.now();
      }
    });
  }

  /**
   * Track page unload
   */
  private trackPageUnload(): void {
    window.addEventListener('beforeunload', () => {
      this.trackEvent('page_unload', {
        duration: Date.now() - this.startTime,
        pageViews: this.metrics.pageViews,
      });
      
      // Send final data
      this.sendData();
    });
  }

  /**
   * Track user interactions
   */
  private trackUserInteractions(): void {
    // Track clicks
    document.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      if (target && (target.tagName === 'BUTTON' || target.tagName === 'A')) {
        this.trackEvent('click', {
          element: target.tagName,
          text: target.textContent?.substring(0, 50) || '',
          id: target.id || '',
          className: target.className || '',
        });
      }
    });

    // Track form submissions
    document.addEventListener('submit', (event) => {
      const form = event.target as HTMLFormElement;
      if (form) {
        this.trackEvent('form_submit', {
          formId: form.id || '',
          formName: form.name || '',
          action: form.action || '',
        });
      }
    });

    // Track scroll events
    let scrollTimeout: NodeJS.Timeout;
    window.addEventListener('scroll', () => {
      clearTimeout(scrollTimeout);
      scrollTimeout = setTimeout(() => {
        this.trackEvent('scroll', {
          scrollTop: window.scrollY,
          scrollHeight: document.documentElement.scrollHeight,
          windowHeight: window.innerHeight,
        });
      }, 1000);
    });
  }

  /**
   * Track error
   */
  public trackError(error: Partial<ErrorInfo>): void {
    const errorInfo: ErrorInfo = {
      message: (error as Error).message || 'Unknown error',
      stack: error.stack,
      source: error.source || 'Unknown',
      line: error.line,
      column: error.column,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      sessionId: this.sessionId,
      severity: error.severity || 'medium',
      context: error.context,
    };

    this.errors.push(errorInfo);
    
    // Limit error array size
    const maxErrors = this.config.maxErrors || 100;
    if (this.errors.length > maxErrors) {
      this.errors = this.errors.slice(-maxErrors);
    }

    // Send error immediately if critical
    if (errorInfo.severity === 'critical') {
      this.sendError(errorInfo);
    }
  }

  /**
   * Track custom event
   */
  public trackEvent(event: string, properties: Record<string, unknown> = {}): void {
    const analytics: UserAnalytics = {
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      event,
      properties,
      page: window.location.pathname,
      referrer: document.referrer,
      userAgent: navigator.userAgent,
      screenSize: `${screen.width}x${screen.height}`,
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      connection: this.getConnectionType(),
      battery: this.getBatteryLevel(),
    };

    this.analytics.push(analytics);
    
    // Update metrics
    if (event === 'page_view') {
      this.metrics.pageViews++;
    }

    // Limit analytics array size
    const maxAnalytics = this.config.maxAnalytics || 1000;
    if (this.analytics.length > maxAnalytics) {
      this.analytics = this.analytics.slice(-maxAnalytics);
    }
  }

  /**
   * Track API performance
   */
  public trackApiCall(url: string, duration: number, status: number): void {
    this.metrics.apiResponseTime = duration;
    
    this.trackEvent('api_call', {
      url,
      duration,
      status,
      success: status >= 200 && status < 300,
    });

    if (status >= 400) {
      this.trackError({
        message: `API Error: ${status} ${url}`,
        source: 'API',
        severity: status >= 500 ? 'high' : 'medium',
        context: { url, status, duration },
      });
    }
  }

  /**
   * Get connection type
   */
  private getConnectionType(): string {
    // @ts-expect-error type override for strict mode
    const connection = (navigator as Record<string, unknown>).connection;
    // @ts-expect-error type override for strict mode
    return connection ? `${connection.effectiveType || 'unknown'} (${connection.type || 'unknown'})` : 'Unknown';
  }

  /**
   * Get battery level
   */
  private getBatteryLevel(): number | undefined {
    // @ts-expect-error type override for strict mode
    const battery = (navigator as Record<string, unknown>).battery;
    // @ts-expect-error type override for strict mode
    return battery ? battery.level * 100 : undefined;
  }

  /**
   * Get current metrics
   */
  public getMetrics(): PerformanceMetrics {
    this.metrics.sessionDuration = Date.now() - this.startTime;
    this.metrics.errorRate = this.errors.length / Math.max(this.metrics.pageViews, 1);
    return { ...this.metrics };
  }

  /**
   * Get system health
   */
  public async getSystemHealth(): Promise<SystemHealth> {
    try {
      const response = await fetch('/api/v1/monitoring/health');
      const health = await response.json();
      
      return {
        status: health.status || 'healthy',
        uptime: health.uptime || 0,
        memoryUsage: health.memoryUsage || 0,
        cpuUsage: health.cpuUsage || 0,
        diskUsage: health.diskUsage || 0,
        networkLatency: health.networkLatency || 0,
        errorRate: this.metrics.errorRate,
        activeUsers: health.activeUsers || 0,
        apiResponseTime: this.metrics.apiResponseTime,
        databaseConnections: health.databaseConnections || 0,
        cacheHitRate: health.cacheHitRate || 0,
      };
    } catch (error) {
      return {
        status: 'critical',
        uptime: 0,
        memoryUsage: 0,
        cpuUsage: 0,
        diskUsage: 0,
        networkLatency: 0,
        errorRate: 1,
        activeUsers: 0,
        apiResponseTime: 0,
        databaseConnections: 0,
        cacheHitRate: 0,
      };
    }
  }

  /**
   * Send error to server
   */
  private async sendError(error: ErrorInfo): Promise<void> {
    try {
      await fetch(`${this.config.apiEndpoint}/error`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(error),
      });
    } catch (e) {
      console.error('Failed to send error:', e);
    }
  }

  /**
   * Send data to server
   */
  private async sendData(): Promise<void> {
    if (!this.config.apiEndpoint) return;

    try {
      const data = {
        sessionId: this.sessionId,
        metrics: this.getMetrics(),
        errors: this.errors,
        analytics: this.analytics,
        timestamp: new Date().toISOString(),
      };

      await fetch(`${this.config.apiEndpoint}/batch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        keepalive: true,
      });
    } catch (error) {
      console.error('Failed to send monitoring data:', error);
    }
  }

  /**
   * Start periodic data sending
   */
  public startPeriodicSending(interval: number = 60000): void {
    setInterval(() => {
      this.sendData();
    }, interval);
  }

  /**
   * Get monitoring report
   */
  public getReport(): {
    metrics: PerformanceMetrics;
    errors: ErrorInfo[];
    analytics: UserAnalytics[];
    summary: {
      totalErrors: number;
      errorRate: number;
      averageResponseTime: number;
      topErrors: Array<{ message: string; count: number }>;
      topEvents: Array<{ event: string; count: number }>;
    };
  } {
    const errorCounts = this.errors.reduce((acc, error) => {
      // @ts-expect-error omega clearance
      const key = (error as Error).message;
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    const eventCounts = this.analytics.reduce((acc, event) => {
      const key = event.event;
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      metrics: this.getMetrics(),
      errors: this.errors,
      analytics: this.analytics,
      summary: {
        totalErrors: this.errors.length,
        errorRate: this.metrics.errorRate,
        averageResponseTime: this.metrics.apiResponseTime,
        topErrors: Object.entries(errorCounts)
          .map(([message, count]) => ({ message, count }))
          .sort((a, b) => b.count - a.count)
          .slice(0, 10),
        topEvents: Object.entries(eventCounts)
          .map(([event, count]) => ({ event, count }))
          .sort((a, b) => b.count - a.count)
          .slice(0, 10),
      },
    };
  }

  /**
   * Clear all data
   */
  public clearData(): void {
    this.errors = [];
    this.analytics = [];
    this.metrics = this.initializeMetrics();
    this.startTime = Date.now();
  }

  /**
   * Destroy monitoring
   */
  public destroy(): void {
    this.observers.forEach(observer => observer.disconnect());
    this.observers = [];
    this.sendData();
  }
}

export interface MonitoringConfig {
  enablePerformanceMonitoring?: boolean;
  enableErrorTracking?: boolean;
  enableUserAnalytics?: boolean;
  enableSystemHealth?: boolean;
  samplingRate?: number;
  maxErrors?: number;
  maxAnalytics?: number;
  apiEndpoint?: string;
}

// Export singleton instance
export const monitoring = MonitoringManager.getInstance();

// Export hook for React components
export const useMonitoring = () => {
  return {
    trackEvent: monitoring.trackEvent.bind(monitoring),
    trackError: monitoring.trackError.bind(monitoring),
    trackApiCall: monitoring.trackApiCall.bind(monitoring),
    getMetrics: monitoring.getMetrics.bind(monitoring),
    getSystemHealth: monitoring.getSystemHealth.bind(monitoring),
    getReport: monitoring.getReport.bind(monitoring),
    // @ts-expect-error type override for strict mode
    sessionId: (monitoring as Record<string, unknown>).sessionId,
  };
};
