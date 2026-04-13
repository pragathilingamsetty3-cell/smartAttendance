/**
 * Testing utilities for the smart attendance system
 */

// Mock data generators
export const generateMockUser = (overrides: Partial<unknown> = {}) => ({
  id: Math.random().toString(36).substring(7),
  name: 'Test User',
  email: 'test@example.com',
  role: 'STUDENT',
  isActive: true,
  createdAt: new Date().toISOString(),
  ...overrides,
});

export const generateMockRoom = (overrides: Partial<unknown> = {}) => ({
  roomId: Math.random().toString(36).substring(7),
  name: 'Test Room',
  building: 'Main Building',
  floor: 1,
  capacity: 30,
  hasBoundary: true,
  boundaryType: 'RECTANGLE',
  ...overrides,
});

export const generateMockAttendanceSession = (overrides: Partial<unknown> = {}) => ({
  id: Math.random().toString(36).substring(7),
  courseId: 'CS101',
  facultyId: Math.random().toString(36).substring(7),
  roomId: Math.random().toString(36).substring(7),
  startTime: new Date().toISOString(),
  endTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
  isActive: true,
  attendanceRecords: [],
  ...overrides,
});

// Test utilities
export class TestUtils {
  static async waitFor<T>(
    condition: () => T,
    timeout: number = 5000,
    interval: number = 100
  ): Promise<T> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < timeout) {
      try {
        const result = condition();
        if (result) return result;
      } catch (error) {
        // Continue waiting
      }
      
      await new Promise(resolve => setTimeout(resolve, interval));
    }
    
    throw new Error(`Condition not met within ${timeout}ms`);
  }
  
  static async waitForElement(
    selector: string,
    timeout: number = 5000
  ): Promise<Element> {
    return this.waitFor(
      () => document.querySelector(selector),
      timeout
    ) as Promise<Element>;
  }
  
  static async waitForElementToDisappear(
    selector: string,
    timeout: number = 5000
  ): Promise<void> {
    await this.waitFor(
      () => !document.querySelector(selector),
      timeout
    );
  }
  
  static mockApiResponse<T>(
    data: T,
    delay: number = 100
  ): Promise<T> {
    return new Promise(resolve => {
      setTimeout(() => resolve(data), delay);
    });
  }
  
  static mockApiError(
    message: string,
    delay: number = 100
  ): Promise<never> {
    return new Promise((_, reject) => {
      setTimeout(() => reject(new Error(message)), delay);
    });
  }
  
  static createMockEvent(
    type: string,
    properties: Partial<Event> = {}
  ): Event {
    const event = new Event(type, {
      bubbles: true,
      cancelable: true,
      ...properties,
    });
    
    Object.assign(event, properties);
    return event;
  }
  
  static simulateUserInput(
    element: HTMLElement,
    value: string
  ): void {
    element.focus();
    
    if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
      element.value = value;
      element.dispatchEvent(this.createMockEvent('input'));
      element.dispatchEvent(this.createMockEvent('change'));
    }
  }
  
  static simulateClick(element: HTMLElement): void {
    element.click();
    element.dispatchEvent(this.createMockEvent('click'));
  }
  
  static simulateFormSubmit(form: HTMLFormElement): void {
    const submitEvent = this.createMockEvent('submit', {
      cancelable: true,
    });
    form.dispatchEvent(submitEvent);
  }
}

// Assertion utilities
export class Assertions {
  static assertElementExists(selector: string): Element {
    const element = document.querySelector(selector);
    if (!element) {
      throw new Error(`Element with selector "${selector}" not found`);
    }
    return element;
  }
  
  static assertElementNotExists(selector: string): void {
    const element = document.querySelector(selector);
    if (element) {
      throw new Error(`Element with selector "${selector}" should not exist`);
    }
  }
  
  static assertElementText(selector: string, expectedText: string): void {
    const element = this.assertElementExists(selector);
    const text = element.textContent?.trim();
    if (text !== expectedText) {
      throw new Error(
        `Element "${selector}" text "${text}" does not match expected "${expectedText}"`
      );
    }
  }
  
  static assertElementContainsText(selector: string, expectedText: string): void {
    const element = this.assertElementExists(selector);
    const text = element.textContent?.trim();
    if (!text?.includes(expectedText)) {
      throw new Error(
        `Element "${selector}" text "${text}" does not contain "${expectedText}"`
      );
    }
  }
  
  static assertElementVisible(selector: string): void {
    const element = this.assertElementExists(selector);
    const styles = window.getComputedStyle(element);
    if (styles.display === 'none' || styles.visibility === 'hidden') {
      throw new Error(`Element "${selector}" is not visible`);
    }
  }
  
  static assertElementHidden(selector: string): void {
    const element = this.assertElementExists(selector);
    const styles = window.getComputedStyle(element);
    if (styles.display !== 'none' && styles.visibility !== 'hidden') {
      throw new Error(`Element "${selector}" is not hidden`);
    }
  }
  
  static assertElementDisabled(selector: string): void {
    const element = this.assertElementExists(selector);
    if (!(element instanceof HTMLInputElement || element instanceof HTMLButtonElement)) {
      throw new Error(`Element "${selector}" is not an input or button`);
    }
    if (!element.disabled) {
      throw new Error(`Element "${selector}" is not disabled`);
    }
  }
  
  static assertElementEnabled(selector: string): void {
    const element = this.assertElementExists(selector);
    if (!(element instanceof HTMLInputElement || element instanceof HTMLButtonElement)) {
      throw new Error(`Element "${selector}" is not an input or button`);
    }
    if (element.disabled) {
      throw new Error(`Element "${selector}" is disabled`);
    }
  }
  
  static assertElementHasClass(selector: string, className: string): void {
    const element = this.assertElementExists(selector);
    if (!element.classList.contains(className)) {
      throw new Error(`Element "${selector}" does not have class "${className}"`);
    }
  }
  
  static assertElementLacksClass(selector: string, className: string): void {
    const element = this.assertElementExists(selector);
    if (element.classList.contains(className)) {
      throw new Error(`Element "${selector}" has class "${className}"`);
    }
  }
}

// Test runner
export class TestRunner {
  private static tests: Array<{
    name: string;
    fn: () => Promise<void> | void;
  }> = [];
  
  static test(name: string, fn: () => Promise<void> | void): void {
    this.tests.push({ name, fn });
  }
  
  static async runAll(): Promise<void> {
    console.log('🧪 Running tests...');
    
    const results = {
      passed: 0,
      failed: 0,
      errors: [] as string[],
    };
    
    for (const test of this.tests) {
      try {
        await test.fn();
        console.log(`✅ ${test.name}`);
        results.passed++;
      } catch (error) {
        console.error(`❌ ${test.name}:`, error);
        results.failed++;
        results.errors.push(`${test.name}: ${error}`);
      }
    }
    
    console.log(`\n📊 Test Results:`);
    console.log(`✅ Passed: ${results.passed}`);
    console.log(`❌ Failed: ${results.failed}`);
    console.log(`📈 Success Rate: ${((results.passed / (results.passed + results.failed)) * 100).toFixed(1)}%`);
    
    if (results.errors.length > 0) {
      console.log('\n❌ Errors:');
      results.errors.forEach(error => console.log(`  - ${error}`));
    }
    
    this.tests = []; // Clear tests after run
  }
  
  static async runSingle(name: string): Promise<void> {
    const test = this.tests.find(t => t.name === name);
    if (!test) {
      throw new Error(`Test "${name}" not found`);
    }
    
    try {
      await test.fn();
      console.log(`✅ ${test.name}`);
    } catch (error) {
      console.error(`❌ ${test.name}:`, error);
      throw error;
    }
  }
}

// Mock service factory
export class MockServiceFactory {
  static createMockAuthService() {
    return {
      // @ts-expect-error type override for strict mode
      login: mockFn().mockResolvedValue({
        token: 'mock-token',
        user: generateMockUser(),
      }),
      // @ts-expect-error type override for strict mode
      logout: mockFn().mockResolvedValue(undefined),
      // @ts-expect-error type override for strict mode
      refreshToken: mockFn().mockResolvedValue({
        token: 'new-mock-token',
      }),
    };
  }
  
  static createMockUserManagementService() {
    return {
      // @ts-expect-error type override for strict mode
      onboardStudent: mockFn().mockResolvedValue({
        success: true,
        message: 'Student onboarded successfully',
      }),
      // @ts-expect-error type override for strict mode
      getAllUsers: mockFn().mockResolvedValue({
        data: [generateMockUser()],
        total: 1,
        page: 1,
        limit: 10,
        totalPages: 1,
      }),
      // @ts-expect-error type override for strict mode
      updateUserStatus: mockFn().mockResolvedValue(undefined),
    };
  }
  
  static createMockRoomManagementService() {
    return {
      // @ts-expect-error type override for strict mode
      createRoom: mockFn().mockResolvedValue(generateMockRoom()),
      // @ts-expect-error type override for strict mode
      getAllRooms: mockFn().mockResolvedValue({
        data: [generateMockRoom()],
        total: 1,
        page: 1,
        limit: 10,
        totalPages: 1,
      }),
      // @ts-expect-error type override for strict mode
      validateBoundary: mockFn().mockResolvedValue({
        isValid: true,
        area: 100,
        warnings: [],
      }),
    };
  }
  
  static createMockAttendanceService() {
    return {
      // @ts-expect-error type override for strict mode
      createSession: mockFn().mockResolvedValue(generateMockAttendanceSession()),
      // @ts-expect-error type override for strict mode
      getActiveSessions: mockFn().mockResolvedValue([generateMockAttendanceSession()]),
      // @ts-expect-error type override for strict mode
      startSession: mockFn().mockResolvedValue(undefined),
      // @ts-expect-error type override for strict mode
      endSession: mockFn().mockResolvedValue(undefined),
    };
  }
}

// Mock function for testing (replaces jest.fn)
export const mockFn = <T extends (...args: unknown[]) => unknown>(
  implementation?: (...args: Parameters<T>) => ReturnType<T>
) => {
  const calls: Parameters<T>[] = [];
  let currentImpl: ((...args: Parameters<T>) => ReturnType<T>) | undefined;
  
  const mock = (...args: Parameters<T>): ReturnType<T> => {
    calls.push(args);
    if (currentImpl) {
      return currentImpl(...args);
    }
    // @ts-expect-error type override for strict mode
    return undefined as unknown;
  };
  
  mock.calls = calls;
  mock.mockReturnValue = (value: ReturnType<T>) => {
    currentImpl = () => value;
    return mock;
  };
  
  mock.mockResolvedValue = (value: ReturnType<T>) => {
    currentImpl = () => Promise.resolve(value) as ReturnType<T>;
    return mock;
  };
  
  mock.mockRejectedValue = (value: unknown) => {
    currentImpl = () => Promise.reject(value) as ReturnType<T>;
    return mock;
  };
  
  if (implementation) {
    currentImpl = implementation;
  }
  
  return mock as unknown;
};

// Performance testing utilities
export class PerformanceTestUtils {
  static async measureRenderTime(
    renderFn: () => void,
    iterations: number = 10
  ): Promise<{ avg: number; min: number; max: number; total: number }> {
    const times: number[] = [];
    
    for (let i = 0; i < iterations; i++) {
      const start = performance.now();
      renderFn();
      const end = performance.now();
      times.push(end - start);
    }
    
    const total = times.reduce((sum, time) => sum + time, 0);
    const avg = total / times.length;
    const min = Math.min(...times);
    const max = Math.max(...times);
    
    return { avg, min, max, total };
  }
  
  static async measureMemoryUsage(
    operation: () => Promise<void> | void
  ): Promise<{ before: number; after: number; delta: number }> {
    // @ts-expect-error type override for strict mode
    const before = (performance as unknown).memory?.usedJSHeapSize || 0;
    await operation();
    
    // Force garbage collection if available
    // @ts-expect-error type override for strict mode
    if ((window as unknown).gc) {
      // @ts-expect-error type override for strict mode
      (window as unknown).gc();
    }
    
    // @ts-expect-error type override for strict mode
    const after = (performance as unknown).memory?.usedJSHeapSize || 0;
    const delta = after - before;
    
    return { before, after, delta };
  }
}
