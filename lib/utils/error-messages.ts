import { ApiError, FieldError } from '../api/types';

/**
 * Maps technical error codes to user-friendly messages
 */
const ERROR_MESSAGES: Record<number, string> = {
  400: 'There was a problem with your request. Please check your input and try again.',
  401: 'You need to log in to access this resource.',
  403: 'You do not have permission to perform this action.',
  404: 'The requested resource was not found.',
  409: 'This action conflicts with existing data. Please refresh and try again.',
  500: 'An unexpected error occurred. Please try again later.',
  503: 'The service is temporarily unavailable. Please try again later.',
};

/**
 * Gets a user-friendly error message from an ApiError
 */
export function getErrorMessage(error: ApiError): string {
  // If it's a validation error, format the field errors
  if (error.isValidationError()) {
    return formatValidationErrors(error.response.fieldErrors || []);
  }

  // Use status-specific message if available, otherwise use the error message from backend
  const statusMessage = ERROR_MESSAGES[error.response.status];
  if (statusMessage) {
    return statusMessage;
  }

  // Fallback to the error message from backend
  return error.message || 'An unexpected error occurred.';
}

/**
 * Formats validation errors as a readable string
 */
export function formatValidationErrors(fieldErrors: FieldError[]): string {
  if (!fieldErrors || fieldErrors.length === 0) {
    return 'Validation failed. Please check your input.';
  }

  if (fieldErrors.length === 1) {
    return fieldErrors[0].message;
  }

  // Multiple errors - create a list
  const messages = fieldErrors.map(e => `${capitalize(e.field)}: ${e.message}`);
  return messages.join('; ');
}

/**
 * Gets a short error title based on the error type
 */
export function getErrorTitle(error: ApiError): string {
  if (error.isValidationError()) {
    return 'Validation Error';
  }
  if (error.isUnauthorizedError()) {
    return 'Authentication Required';
  }
  if (error.isForbiddenError()) {
    return 'Access Denied';
  }
  if (error.isNotFoundError()) {
    return 'Not Found';
  }
  if (error.isConflictError()) {
    return 'Conflict';
  }
  if (error.isServerError()) {
    return 'Server Error';
  }
  return 'Error';
}

/**
 * Determines if user should be shown a "contact support" message
 */
export function shouldShowSupportMessage(error: ApiError): boolean {
  return error.isServerError();
}

/**
 * Gets support message with correlation ID
 */
export function getSupportMessage(error: ApiError): string {
  if (!shouldShowSupportMessage(error)) {
    return '';
  }
  return `If this problem persists, please contact support with reference ID: ${error.correlationId}`;
}

/**
 * Capitalizes first letter of a string
 */
function capitalize(str: string): string {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Checks if an error is an ApiError instance
 */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/**
 * Converts unknown error to user message
 */
export function getGenericErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return getErrorMessage(error);
  }

  if (error instanceof Error) {
    return error.message || 'An unexpected error occurred.';
  }

  return 'An unexpected error occurred.';
}
