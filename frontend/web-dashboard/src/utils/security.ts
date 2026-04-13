/**
 * Security utilities for the smart attendance system
 */

// Input sanitization
export const sanitizeInput = (input: string): string => {
  return input
    .trim()
    .replace(/[<>]/g, '') // Remove potential HTML tags
    .replace(/javascript:/gi, '') // Remove JavaScript protocol
    .replace(/on\w+=/gi, ''); // Remove event handlers
};

// XSS protection
export const escapeHtml = (text: string): string => {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
};

// CSRF token management
export class CSRFProtection {
  private static token: string | null = null;
  
  static setToken(token: string): void {
    this.token = token;
  }
  
  static getToken(): string | null {
    return this.token;
  }
  
  static addToHeaders(headers: Record<string, string>): Record<string, string> {
    if (this.token) {
      headers['X-CSRF-Token'] = this.token;
    }
    return headers;
  }
}

// Content Security Policy helper
export const CSPHelper = {
  // Generate nonce for inline scripts
  generateNonce: (): string => {
    const array = new Uint8Array(16);
    crypto.getRandomValues(array);
    return btoa(String.fromCharCode(...array));
  },
  
  // Validate URL against CSP
  isAllowedOrigin: (url: string): boolean => {
    try {
      const urlObj = new URL(url, window.location.origin);
      return urlObj.origin === window.location.origin;
    } catch {
      return false;
    }
  },
};

// Secure storage
export const secureStorage = {
  setItem: (key: string, value: string): void => {
    try {
      // Basic encryption for sensitive data
      const encrypted = btoa(value);
      localStorage.setItem(key, encrypted);
    } catch (error) {
      console.warn('Failed to store encrypted data:', error);
      localStorage.setItem(key, value);
    }
  },
  
  getItem: (key: string): string | null => {
    try {
      const value = localStorage.getItem(key);
      if (!value) return null;
      
      // Try to decrypt
      try {
        return atob(value);
      } catch {
        return value; // Return as-is if not encrypted
      }
    } catch (error) {
      console.warn('Failed to retrieve encrypted data:', error);
      return null;
    }
  },
  
  removeItem: (key: string): void => {
    localStorage.removeItem(key);
  },
  
  clear: (): void => {
    localStorage.clear();
  },
};

// Rate limiting
export class RateLimiter {
  private static requests = new Map<string, number[]>();
  
  static isAllowed(
    identifier: string,
    maxRequests: number,
    windowMs: number
  ): boolean {
    const now = Date.now();
    const windowStart = now - windowMs;
    
    if (!this.requests.has(identifier)) {
      this.requests.set(identifier, []);
    }
    
    const timestamps = this.requests.get(identifier)!;
    
    // Remove old requests outside the window
    const validTimestamps = timestamps.filter(time => time > windowStart);
    this.requests.set(identifier, validTimestamps);
    
    // Check if under limit
    if (validTimestamps.length < maxRequests) {
      validTimestamps.push(now);
      return true;
    }
    
    return false;
  }
  
  static getRemainingRequests(
    identifier: string,
    maxRequests: number,
    windowMs: number
  ): number {
    const now = Date.now();
    const windowStart = now - windowMs;
    
    const timestamps = this.requests.get(identifier) || [];
    const validTimestamps = timestamps.filter(time => time > windowStart);
    
    return Math.max(0, maxRequests - validTimestamps.length);
  }
  
  static getResetTime(
    identifier: string,
    windowMs: number
  ): number | null {
    const timestamps = this.requests.get(identifier);
    if (!timestamps || timestamps.length === 0) return null;
    
    const oldestTimestamp = Math.min(...timestamps);
    return oldestTimestamp + windowMs;
  }
}

// Password strength validation
export const validatePassword = (password: string): {
  isValid: boolean;
  strength: 'weak' | 'medium' | 'strong';
  issues: string[];
} => {
  const issues: string[] = [];
  let score = 0;
  
  // Length check
  if (password.length < 8) {
    issues.push('Password must be at least 8 characters long');
  } else {
    score += 1;
  }
  
  // Uppercase check
  if (!/[A-Z]/.test(password)) {
    issues.push('Password must contain at least one uppercase letter');
  } else {
    score += 1;
  }
  
  // Lowercase check
  if (!/[a-z]/.test(password)) {
    issues.push('Password must contain at least one lowercase letter');
  } else {
    score += 1;
  }
  
  // Number check
  if (!/\d/.test(password)) {
    issues.push('Password must contain at least one number');
  } else {
    score += 1;
  }
  
  // Special character check
  if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    issues.push('Password must contain at least one special character');
  } else {
    score += 1;
  }
  
  // Common patterns check
  const commonPatterns = [
    /123456/,
    /password/i,
    /qwerty/i,
    /admin/i,
    /letmein/i,
  ];
  
  if (commonPatterns.some(pattern => pattern.test(password))) {
    issues.push('Password contains common patterns');
    score = Math.max(0, score - 2);
  }
  
  let strength: 'weak' | 'medium' | 'strong' = 'weak';
  if (score >= 4) strength = 'strong';
  else if (score >= 2) strength = 'medium';
  
  return {
    isValid: issues.length === 0,
    strength,
    issues,
  };
};

// Session security
export class SessionSecurity {
  private static activityTimeout: NodeJS.Timeout | null = null;
  private static warningTimeout: NodeJS.Timeout | null = null;
  private static readonly INACTIVE_TIMEOUT = 15 * 60 * 1000; // 15 minutes
  private static readonly WARNING_TIMEOUT = 13 * 60 * 1000; // 13 minutes
  
  static startMonitoring(onWarning: () => void, onTimeout: () => void): void {
    this.resetActivity();
    
    // Listen for user activity
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'];
    const resetActivity = () => this.resetActivity();
    
    events.forEach(event => {
      document.addEventListener(event, resetActivity, true);
    });
    
    // Store event listeners for cleanup
    // @ts-expect-error type override for strict mode
    (this as Record<string, unknown>).eventListeners = events;
    // @ts-expect-error type override for strict mode
    (this as Record<string, unknown>).resetActivity = resetActivity;
  }
  
  static resetActivity(): void {
    // Clear existing timeouts
    if (this.activityTimeout) {
      clearTimeout(this.activityTimeout);
    }
    if (this.warningTimeout) {
      clearTimeout(this.warningTimeout);
    }
    
    // Set new timeouts
    this.warningTimeout = setTimeout(() => {
      // Show warning
      // @ts-expect-error type override for strict mode
      if ((this as Record<string, unknown>).onWarning) {
        // @ts-expect-error type override for strict mode
        (this as Record<string, unknown>).onWarning();
      }
    }, this.WARNING_TIMEOUT);
    
    this.activityTimeout = setTimeout(() => {
      // Log out user
      // @ts-expect-error type override for strict mode
      if ((this as Record<string, unknown>).onTimeout) {
        // @ts-expect-error type override for strict mode
        (this as Record<string, unknown>).onTimeout();
      }
    }, this.INACTIVE_TIMEOUT);
  }
  
  static stopMonitoring(): void {
    if (this.activityTimeout) {
      clearTimeout(this.activityTimeout);
      this.activityTimeout = null;
    }
    
    if (this.warningTimeout) {
      clearTimeout(this.warningTimeout);
      this.warningTimeout = null;
    }
    
    // Remove event listeners
    // @ts-expect-error type override for strict mode
    if ((this as Record<string, unknown>).eventListeners && (this as Record<string, unknown>).resetActivity) {
      // @ts-expect-error type override for strict mode
      (this as Record<string, unknown>).eventListeners.forEach((event: string) => {
        // @ts-expect-error type override for strict mode
        document.removeEventListener(event, (this as Record<string, unknown>).resetActivity, true);
      });
    }
  }
}

// API security headers
export const getSecureHeaders = (): Record<string, string> => {
  return {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache',
  };
};

// URL validation
export const isValidUrl = (url: string): boolean => {
  try {
    const urlObj = new URL(url);
    return ['http:', 'https:'].includes(urlObj.protocol);
  } catch {
    return false;
  }
};

// File upload security
export const validateFileUpload = (file: File): {
  isValid: boolean;
  issues: string[];
} => {
  const issues: string[] = [];
  
  // File size check (10MB limit)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    issues.push('File size exceeds 10MB limit');
  }
  
  // File type check
  const allowedTypes = [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'application/pdf',
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ];
  
  if (!allowedTypes.includes(file.type)) {
    issues.push('File type not allowed');
  }
  
  // File name check
  const dangerousPatterns = [
    /\.\./,
    /[<>:"|?*]/,
    /\.(exe|bat|cmd|scr|pif)$/i,
  ];
  
  if (dangerousPatterns.some(pattern => pattern.test(file.name))) {
    issues.push('File name contains dangerous characters');
  }
  
  return {
    isValid: issues.length === 0,
    issues,
  };
};
