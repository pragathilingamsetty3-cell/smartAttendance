/**
 * Comprehensive validation utilities for the smart attendance system
 */

import { useState } from 'react';
import { VALIDATION_RULES, REGEX_PATTERNS } from './constants';

// Validation result interface
export interface ValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
}

// Base validator class
export class Validator {
  static validate(value: unknown, rules: ValidationRule[]): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    for (const rule of rules) {
      const result = rule.validate(value);
      if (!result.isValid) {
        if (result.severity === 'error') {
          errors.push(result.message);
        } else {
          warnings.push(result.message);
        }
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
    };
  }
}

// Validation rule interface
export interface ValidationRule {
  validate(value: unknown): { isValid: boolean; message: string; severity?: 'error' | 'warning' };
}

// Required field validator
export const required = (message = 'This field is required'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: value !== null && value !== undefined && value !== '',
    message,
  }),
});

// Length validators
export const minLength = (min: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const str = String(value || '');
    return {
      isValid: str.length >= min,
      message: message || `Minimum length is ${min} characters`,
    };
  },
});

export const maxLength = (max: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const str = String(value || '');
    return {
      isValid: str.length <= max,
      message: message || `Maximum length is ${max} characters`,
    };
  },
});

export const exactLength = (length: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const str = String(value || '');
    return {
      isValid: str.length === length,
      message: message || `Must be exactly ${length} characters`,
    };
  },
});

// Pattern validators
export const pattern = (regex: RegExp, message: string): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || regex.test(String(value)),
    message,
  }),
});

// Email validator
export const email = (message = 'Please enter a valid email address'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.EMAIL.test(String(value)),
    message,
  }),
});

// Phone validator
export const phone = (message = 'Please enter a valid phone number'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.PHONE.test(String(value)),
    message,
  }),
});

// URL validator
export const url = (message = 'Please enter a valid URL'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.URL.test(String(value)),
    message,
  }),
});

// Number validators
export const min = (min: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const num = Number(value);
    return {
      isValid: isNaN(num) || num >= min,
      message: message || `Minimum value is ${min}`,
    };
  },
});

export const max = (max: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const num = Number(value);
    return {
      isValid: isNaN(num) || num <= max,
      message: message || `Maximum value is ${max}`,
    };
  },
});

export const range = (min: number, max: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    const num = Number(value);
    return {
      isValid: isNaN(num) || (num >= min && num <= max),
      message: message || `Value must be between ${min} and ${max}`,
    };
  },
});

// Date validators
export const minDate = (minDate: Date, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    if (!value) return { isValid: true, message: '' };
    const date = new Date(String(value));
    return {
      isValid: date >= minDate,
      message: message || `Date must be on or after ${minDate.toLocaleDateString()}`,
    };
  },
});

export const maxDate = (maxDate: Date, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    if (!value) return { isValid: true, message: '' };
    const date = new Date(String(value));
    return {
      isValid: date <= maxDate,
      message: message || `Date must be on or before ${maxDate.toLocaleDateString()}`,
    };
  },
});

// Select validators
export const selectRequired = (message = 'Please select an option'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: value !== null && value !== undefined && value !== '' && value !== '0',
    message,
  }),
});

// File validators
export const fileSize = (maxSize: number, message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    if (!value || !(value instanceof File)) return { isValid: true, message: '' };
    return {
      isValid: value.size <= maxSize,
      message: message || `File size must be less than ${Math.round(maxSize / 1024 / 1024)}MB`,
    };
  },
});

export const fileType = (allowedTypes: string[], message?: string): ValidationRule => ({
  validate: (value: unknown) => {
    if (!value || !(value instanceof File)) return { isValid: true, message: '' };
    return {
      isValid: allowedTypes.includes(value.type),
      message: message || `File type must be one of: ${allowedTypes.join(', ')}`,
    };
  },
});

// Password strength validator
export const passwordStrength = (message = 'Password does not meet security requirements'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.PASSWORD.test(String(value)),
    message,
  }),
});

// Username validator
export const username = (message = 'Username must contain only letters, numbers, and underscores'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.USERNAME.test(String(value)),
    message,
  }),
});

// Room code validator
export const roomCode = (message = 'Room code must contain only letters and numbers'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.ROOM_CODE.test(String(value)),
    message,
  }),
});

// Student ID validator
export const studentId = (message = 'Student ID must contain only letters and numbers'): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: !value || REGEX_PATTERNS.STUDENT_ID.test(String(value)),
    message,
  }),
});

// Custom validators
export const custom = (validator: (value: unknown) => boolean, message: string): ValidationRule => ({
  validate: (value: unknown) => ({
    isValid: validator(value),
    message,
  }),
});

// Form validation helper
export const validateForm = <T extends Record<string, unknown>>(
  data: T,
  rules: Record<keyof T, ValidationRule[]>
): Record<keyof T, ValidationResult> => {
  const results = {} as Record<keyof T, ValidationResult>;

  for (const [field, fieldRules] of Object.entries(rules)) {
    results[field as keyof T] = Validator.validate(data[field as keyof T], fieldRules);
  }

  return results;
};

// Check if form is valid
export const isFormValid = <T extends Record<string, unknown>>(
  validationResults: Record<keyof T, ValidationResult>
): boolean => {
  return Object.values(validationResults).every(result => result.isValid);
};

// Get all form errors
export const getFormErrors = <T extends Record<string, unknown>>(
  validationResults: Record<keyof T, ValidationResult>
): string[] => {
  const errors: string[] = [];
  
  for (const result of Object.values(validationResults)) {
    errors.push(...result.errors);
  }
  
  return errors;
};

// Get all form warnings
export const getFormWarnings = <T extends Record<string, unknown>>(
  validationResults: Record<keyof T, ValidationResult>
): string[] => {
  const warnings: string[] = [];
  
  for (const result of Object.values(validationResults)) {
    warnings.push(...result.warnings);
  }
  
  return warnings;
};

// Predefined validation rule sets
export const USER_VALIDATION_RULES = {
  name: [
    required('Name is required'),
    minLength(2, 'Name must be at least 2 characters'),
    maxLength(50, 'Name cannot exceed 50 characters'),
  ],
  email: [
    required('Email is required'),
    email(),
    maxLength(254, 'Email cannot exceed 254 characters'),
  ],
  username: [
    required('Username is required'),
    username(),
    minLength(3, 'Username must be at least 3 characters'),
    maxLength(50, 'Username cannot exceed 50 characters'),
  ],
  password: [
    required('Password is required'),
    minLength(VALIDATION_RULES.PASSWORD_MIN_LENGTH, `Password must be at least ${VALIDATION_RULES.PASSWORD_MIN_LENGTH} characters`),
    maxLength(VALIDATION_RULES.PASSWORD_MAX_LENGTH, `Password cannot exceed ${VALIDATION_RULES.PASSWORD_MAX_LENGTH} characters`),
    passwordStrength(),
  ],
  phone: [
    phone(),
  ],
  role: [
    selectRequired('Role is required'),
  ],
  department: [
    selectRequired('Department is required'),
  ],
  section: [
    selectRequired('Section is required'),
  ],
} as const;

export const ROOM_VALIDATION_RULES = {
  name: [
    required('Room name is required'),
    minLength(2, 'Room name must be at least 2 characters'),
    maxLength(VALIDATION_RULES.ROOM_NAME_MAX_LENGTH, `Room name cannot exceed ${VALIDATION_RULES.ROOM_NAME_MAX_LENGTH} characters`),
  ],
  building: [
    required('Building name is required'),
    minLength(2, 'Building name must be at least 2 characters'),
    maxLength(VALIDATION_RULES.BUILDING_NAME_MAX_LENGTH, `Building name cannot exceed ${VALIDATION_RULES.BUILDING_NAME_MAX_LENGTH} characters`),
  ],
  floor: [
    required('Floor is required'),
    min(0, 'Floor must be 0 or greater'),
    max(100, 'Floor cannot exceed 100'),
  ],
  capacity: [
    required('Capacity is required'),
    min(1, 'Capacity must be at least 1'),
    max(1000, 'Capacity cannot exceed 1000'),
  ],
  roomType: [
    selectRequired('Room type is required'),
  ],
} as const;

export const SESSION_VALIDATION_RULES = {
  courseId: [
    required('Course is required'),
  ],
  facultyId: [
    required('Faculty is required'),
  ],
  roomId: [
    required('Room is required'),
  ],
  startTime: [
    required('Start time is required'),
    minDate(new Date(), 'Start time cannot be in the past'),
  ],
  endTime: [
    required('End time is required'),
    minDate(new Date(), 'End time cannot be in the past'),
  ],
} as const;

export const HALL_PASS_VALIDATION_RULES = {
  studentId: [
    required('Student is required'),
  ],
  requestedMinutes: [
    required('Duration is required'),
    min(1, 'Duration must be at least 1 minute'),
    max(60, 'Duration cannot exceed 60 minutes'),
  ],
  reason: [
    required('Reason is required'),
    minLength(5, 'Reason must be at least 5 characters'),
    maxLength(VALIDATION_RULES.REASON_MAX_LENGTH, `Reason cannot exceed ${VALIDATION_RULES.REASON_MAX_LENGTH} characters`),
  ],
} as const;

// Real-time validation hook (for React components)
export const useValidation = <T extends Record<string, unknown>>(
  initialValues: T,
  rules: Record<keyof T, ValidationRule[]>
) => {
  const [values, setValues] = useState<T>(initialValues);
  const [errors, setErrors] = useState<Record<keyof T, string[]>>({} as Record<keyof T, string[]>);
  const [warnings, setWarnings] = useState<Record<keyof T, string[]>>({} as Record<keyof T, string[]>);
  const [isValid, setIsValid] = useState(false);

  const validateField = (field: keyof T, value: unknown) => {
    const result = Validator.validate(value, rules[field]);
    
    setErrors(prev => ({
      ...prev,
      [field]: result.errors,
    }));
    
    setWarnings(prev => ({
      ...prev,
      [field]: result.warnings,
    }));

    return result;
  };

  const validateAll = () => {
    const results = validateForm(values, rules);
    const newErrors: Record<keyof T, string[]> = {} as Record<keyof T, string[]>;
    const newWarnings: Record<keyof T, string[]> = {} as Record<keyof T, string[]>;

    for (const [field, result] of Object.entries(results)) {
      newErrors[field as keyof T] = result.errors;
      newWarnings[field as keyof T] = result.warnings;
    }

    setErrors(newErrors);
    setWarnings(newWarnings);
    setIsValid(isFormValid(results));

    return results;
  };

  const setValue = (field: keyof T, value: unknown) => {
    setValues(prev => ({ ...prev, [field]: value }));
    validateField(field, value);
  };

  const updateValues = (newValues: Partial<T>) => {
    setValues(prev => ({ ...prev, ...newValues }));
    
    // Validate all fields that have rules
    for (const field of Object.keys(rules)) {
      if (field in newValues) {
        validateField(field as keyof T, newValues[field as keyof T]);
      }
    }
  };

  return {
    values,
    errors,
    warnings,
    isValid,
    validateField,
    validateAll,
    setValue,
    updateValues,
  };
};
