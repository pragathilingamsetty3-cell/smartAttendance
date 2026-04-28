/**
 * Centralized error handling utility for consistent error management.
 * Optimized for Smart Attendance System v1.
 */

export class AppError extends Error {
  public readonly code?: string;
  public readonly statusCode?: number;
  public readonly response?: any; // 🔍 Preserve axios response for detailed error info

  constructor(message: string, code?: string, statusCode?: number, response?: any) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.statusCode = statusCode;
    this.response = response;
  }
}

export const handleError = (error: unknown, defaultMessage: string): AppError => {
  if (error instanceof AppError) {
    return error;
  }

  // 🔍 Preserve axios response data for backend error diagnostics
  const axiosResponse = (error as any)?.response;

  if (error instanceof Error) {
    return new AppError(error.message, 'UNKNOWN_ERROR', axiosResponse?.status, axiosResponse);
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
    // 🔍 Log full response data if available
    if (appError.response?.data) {
      console.error(`[${serviceName}] Server Response:`, appError.response.data);
    }
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
