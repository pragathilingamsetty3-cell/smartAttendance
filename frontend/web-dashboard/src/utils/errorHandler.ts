/**
 * Centralized error handling utility for consistent error management.
 * Optimized for Smart Attendance System v1.
 */

export class AppError extends Error {
  public readonly code?: string;
  public readonly statusCode?: number;

  constructor(message: string, code?: string, statusCode?: number) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.statusCode = statusCode;
  }
}

export const handleError = (error: unknown, defaultMessage: string): AppError => {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof Error) {
    return new AppError(error.message, 'UNKNOWN_ERROR');
  }

  if (typeof error === 'string') {
    return new AppError(error, 'STRING_ERROR');
  }

  return new AppError(defaultMessage, 'UNKNOWN_ERROR');
};

export const createErrorHandler = (serviceName: string) => {
  return (error: unknown, defaultMessage: string): AppError => {
    const appError = handleError(error, defaultMessage);
    // Explicit standard syntax for logging
    console.error(`[${serviceName}] Error:`, appError.message);
    return appError;
  };
};

// Common error messages
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network connection failed. Please check your internet connection.',
  UNAUTHORIZED: 'You are not authorized to perform this action.',
  FORBIDDEN: 'You do not have permission to access this resource.',
  NOT_FOUND: 'The requested resource was not found.',
  VALIDATION_ERROR: 'Please check your input and try again.',
  SERVER_ERROR: 'An unexpected error occurred. Please try again later.',
  TIMEOUT_ERROR: 'The request timed out. Please try again.',
  UNKNOWN_ERROR: 'An unexpected error occurred.',
} as const;

export const getErrorMessage = (error: unknown): string => {
  const appError = handleError(error, ERROR_MESSAGES.UNKNOWN_ERROR);
  return appError.message;
};
