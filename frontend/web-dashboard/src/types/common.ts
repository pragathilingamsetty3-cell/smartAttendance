// Common Types used across the application

export interface ApiResponse<T = unknown> {
  message: string;
  data?: T;
  timestamp?: string;
}

export interface ApiError {
  error: string;
  message?: string;
  timestamp?: string;
  path?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface TableColumn<T = Record<string, unknown>> {
  key: keyof T;
  title: string;
  sortable?: boolean;
  filterable?: boolean;
  render?: (value: unknown, record: T) => React.ReactNode;
  width?: string;
}

export interface FilterOption {
  field: string;
  operator: 'equals' | 'contains' | 'startsWith' | 'endsWith' | 'gt' | 'lt' | 'gte' | 'lte';
  value: unknown;
}

export interface SortOption {
  field: string;
  direction: 'asc' | 'desc';
}

export interface DateRange {
  startDate: string;
  endDate: string;
}

export interface TimeRange {
  startTime: string;
  endTime: string;
}

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface Address {
  street?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
}

export interface ContactInfo {
  email?: string;
  phone?: string;
  mobile?: string;
}

export interface SystemStats {
  totalUsers: number;
  activeUsers: number;
  totalSessions: number;
  activeSessions: number;
  totalRooms: number;
  averageAttendance: number;
  systemLoad: number;
  storageUsed: number;
  lastUpdated: string;
}

export interface NotificationPreferences {
  email: boolean;
  push: boolean;
  sms: boolean;
  attendance: boolean;
  roomChanges: boolean;
  alerts: boolean;
  reports: boolean;
}

export interface ThemeConfig {
  mode: 'light' | 'dark' | 'auto';
  primaryColor: string;
  accentColor: string;
  glassmorphism: boolean;
  animations: boolean;
}

export interface UserPreferences {
  theme: ThemeConfig;
  language: string;
  timezone: string;
  dateFormat: string;
  timeFormat: '12h' | '24h';
  notifications: NotificationPreferences;
}

export interface ExportOptions {
  format: 'excel' | 'csv' | 'pdf' | 'json';
  dateRange?: DateRange;
  includeHeaders: boolean;
  filters?: FilterOption[];
}

export interface ImportOptions {
  format: 'excel' | 'csv' | 'json';
  hasHeaders: boolean;
  skipDuplicates: boolean;
  updateExisting: boolean;
}

export interface FileUpload {
  file: File;
  name: string;
  size: number;
  type: string;
  progress?: number;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
}

export interface LoadingState {
  isLoading: boolean;
  message?: string;
  progress?: number;
}

export interface ValidationError {
  field: string;
  message: string;
  code?: string;
}

export interface FormState<T = Record<string, unknown>> {
  data: T;
  errors: ValidationError[];
  touched: Record<keyof T, boolean>;
  isSubmitting: boolean;
  isValid: boolean;
}

// HTTP Methods
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'OPTIONS';

// Status Types
export type Status = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

// Priority Types
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'CRITICAL';
