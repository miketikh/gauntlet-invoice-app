import { toast } from 'sonner';
import { ApiError, FieldError } from '../api/types';
import { getErrorMessage, getErrorTitle, getSupportMessage } from '../utils/error-messages';

/**
 * Hook for displaying error notifications using Sonner toast
 */
export function useErrorToast() {
  /**
   * Shows a general error toast
   */
  const showError = (error: ApiError, duration?: number) => {
    const title = getErrorTitle(error);
    const message = getErrorMessage(error);
    const supportMessage = getSupportMessage(error);

    const description = supportMessage ? `${message}\n\n${supportMessage}` : message;

    toast.error(title, {
      description,
      duration: duration || (error.isServerError() ? 10000 : 5000),
    });
  };

  /**
   * Shows a validation error toast with field-level details
   */
  const showValidationError = (fieldErrors: FieldError[], duration?: number) => {
    const messages = fieldErrors.map(e => `${e.field}: ${e.message}`).join('\n');

    toast.error('Validation Error', {
      description: messages,
      duration: duration || 7000,
    });
  };

  /**
   * Shows a success toast
   */
  const showSuccess = (message: string, duration?: number) => {
    toast.success(message, {
      duration: duration || 3000,
    });
  };

  /**
   * Shows an info toast
   */
  const showInfo = (message: string, duration?: number) => {
    toast.info(message, {
      duration: duration || 3000,
    });
  };

  /**
   * Shows a warning toast
   */
  const showWarning = (message: string, duration?: number) => {
    toast.warning(message, {
      duration: duration || 5000,
    });
  };

  /**
   * Shows a generic error message (for non-ApiError errors)
   */
  const showGenericError = (error: unknown, duration?: number) => {
    const message = error instanceof Error ? error.message : 'An unexpected error occurred';

    toast.error('Error', {
      description: message,
      duration: duration || 5000,
    });
  };

  return {
    showError,
    showValidationError,
    showSuccess,
    showInfo,
    showWarning,
    showGenericError,
  };
}
