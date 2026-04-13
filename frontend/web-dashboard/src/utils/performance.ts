/**
 * Performance optimization utilities for the smart attendance system
 */

// Debounce utility for preventing excessive API calls
export const debounce = <T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout;
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

// Throttle utility for limiting function calls
export const throttle = <T extends (...args: unknown[]) => unknown>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean;
  
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
};

// Memoization utility for caching expensive computations
export const memoize = <T extends (...args: unknown[]) => unknown>(
  func: T,
  keyGenerator?: (...args: Parameters<T>) => string
): ((...args: Parameters<T>) => ReturnType<T>) => {
  const cache = new Map<string, ReturnType<T>>();
  
  return (...args: Parameters<T>): ReturnType<T> => {
    const key = keyGenerator ? keyGenerator(...args) : JSON.stringify(args);
    
    if (cache.has(key)) {
      return cache.get(key)!;
    }
    
    const result = func(...args) as ReturnType<T>;
    cache.set(key, result);
    return result;
  };
};

// Performance monitoring
export class PerformanceMonitor {
  private static metrics = new Map<string, number[]>();
  
  static startTimer(name: string): () => void {
    const startTime = performance.now();
    
    return () => {
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      if (!this.metrics.has(name)) {
        this.metrics.set(name, []);
      }
      
      this.metrics.get(name)!.push(duration);
      
      // Keep only last 100 measurements
      const measurements = this.metrics.get(name)!;
      if (measurements.length > 100) {
        measurements.shift();
      }
    };
  }
  
  static getMetrics(name: string): { avg: number; min: number; max: number; count: number } | null {
    const measurements = this.metrics.get(name);
    
    if (!measurements || measurements.length === 0) {
      return null;
    }
    
    const avg = measurements.reduce((sum, val) => sum + val, 0) / measurements.length;
    const min = Math.min(...measurements);
    const max = Math.max(...measurements);
    
    return { avg, min, max, count: measurements.length };
  }
  
  static getAllMetrics(): Record<string, { avg: number; min: number; max: number; count: number }> {
    const result: Record<string, { avg: number; min: number; max: number; count: number }> = {};
    
    for (const [name] of this.metrics) {
      const metrics = this.getMetrics(name);
      if (metrics) {
        result[name] = metrics;
      }
    }
    
    return result;
  }
  
  static clearMetrics(name?: string): void {
    if (name) {
      this.metrics.delete(name);
    } else {
      this.metrics.clear();
    }
  }
}

// Virtual scrolling utility for large lists
export const virtualScrollConfig = {
  itemHeight: 60,
  overscan: 5,
  threshold: 100, // Enable virtual scrolling for lists with more than 100 items
};

// Image optimization utility
export const optimizeImage = (
  src: string,
  width?: number,
  height?: number,
  quality?: number
): string => {
  const params = new URLSearchParams();
  
  if (width) params.append('w', width.toString());
  if (height) params.append('h', height.toString());
  if (quality) params.append('q', quality.toString());
  
  const paramString = params.toString();
  return paramString ? `${src}?${paramString}` : src;
};

// Bundle size optimization
export const preloadCriticalResources = (resources: string[]): void => {
  resources.forEach(resource => {
    const link = document.createElement('link');
    link.rel = 'preload';
    link.href = resource;
    
    if (resource.endsWith('.js')) {
      link.as = 'script';
    } else if (resource.endsWith('.css')) {
      link.as = 'style';
    } else if (resource.match(/\.(png|jpg|jpeg|gif|webp)$/)) {
      link.as = 'image';
    }
    
    document.head.appendChild(link);
  });
};

// Memory management
export const cleanup = (): void => {
  // Clear performance metrics
  PerformanceMonitor.clearMetrics();
  
  // Clear unknown cached data
  if ('caches' in window) {
    caches.keys().then(names => {
      names.forEach(name => {
        if (name.includes('temp-') || name.includes('cache-')) {
          caches.delete(name);
        }
      });
    });
  }
};

// Intersection Observer for lazy loading
export const createIntersectionObserver = (
  callback: (entries: IntersectionObserverEntry[]) => void,
  options?: IntersectionObserverInit
): IntersectionObserver => {
  const defaultOptions: IntersectionObserverInit = {
    root: null,
    rootMargin: '50px',
    threshold: 0.1,
    ...options,
  };
  
  return new IntersectionObserver(callback, defaultOptions);
};

// Request animation frame utility
export const rafThrottle = <T extends (...args: unknown[]) => unknown>(
  func: T
): ((...args: Parameters<T>) => void) => {
  let rafId: number | null = null;
  
  return (...args: Parameters<T>) => {
    if (rafId === null) {
      rafId = requestAnimationFrame(() => {
        func(...args);
        rafId = null;
      });
    }
  };
};
