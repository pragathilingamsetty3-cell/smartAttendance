/**
 * Safe number parsing utility to prevent NaN values in the application.
 */

/**
 * Parses a input value into an integer safely.
 * @param value The value to parse (string, number, or null/undefined)
 * @param fallback The value to return if parsing fails (defaults to 0)
 * @returns A valid number (never NaN)
 */
export const safeParseInt = (value: any, fallback: number = 0): number => {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  
  if (typeof value === 'number') {
    return isNaN(value) ? fallback : Math.floor(value);
  }
  
  const parsed = parseInt(String(value), 10);
  return isNaN(parsed) ? fallback : parsed;
};

/**
 * Parses a input value into a float safely.
 * @param value The value to parse
 * @param fallback The value to return if parsing fails
 * @returns A valid number (never NaN)
 */
export const safeParseFloat = (value: any, fallback: number = 0): number => {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  
  if (typeof value === 'number') {
    return isNaN(value) ? fallback : value;
  }
  
  const parsed = parseFloat(String(value));
  return isNaN(parsed) ? fallback : parsed;
};
