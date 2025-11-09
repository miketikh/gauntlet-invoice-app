import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { ApiError, ApiErrorResponse } from './types';

const MAX_RETRIES = 3;
const RETRY_DELAY = 1000; // Base delay in milliseconds

// Extend Axios config to include our custom retry count
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    _retryCount?: number;
  }
}

/**
 * Determines if an error should be retried
 */
function shouldRetry(error: AxiosError): boolean {
  // Don't retry if there's no response (network error)
  if (!error.response) {
    return true; // Network errors are retryable
  }

  // Retry on server errors (5xx)
  if (error.response.status >= 500) {
    return true;
  }

  // Retry on timeout
  if (error.code === 'ECONNABORTED') {
    return true;
  }

  // Don't retry client errors (4xx)
  return false;
}

/**
 * Delays execution for exponential backoff
 */
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Converts axios error to ApiError
 */
function convertToApiError(error: AxiosError): ApiError {
  if (error.response?.data && typeof error.response.data === 'object') {
    const data = error.response.data as any;

    // Check if it's our standard error format
    if (data.correlationId && data.status && data.message) {
      const apiErrorResponse: ApiErrorResponse = {
        timestamp: data.timestamp || new Date().toISOString(),
        status: data.status,
        error: data.error || 'Error',
        message: data.message,
        path: data.path || '',
        correlationId: data.correlationId,
        fieldErrors: data.fieldErrors || []
      };
      return new ApiError(apiErrorResponse);
    }
  }

  // Fallback for non-standard errors
  const apiErrorResponse: ApiErrorResponse = {
    timestamp: new Date().toISOString(),
    status: error.response?.status || 500,
    error: error.code || 'Unknown Error',
    message: error.message || 'An unexpected error occurred',
    path: error.config?.url || '',
    correlationId: 'unknown',
    fieldErrors: []
  };

  return new ApiError(apiErrorResponse);
}

/**
 * Sets up error interceptor for an Axios instance
 */
export function setupErrorInterceptor(axiosInstance: AxiosInstance) {
  // Request interceptor to add correlation ID if available
  axiosInstance.interceptors.request.use(
    (config) => {
      // Could add correlation ID from context here if needed
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // Response interceptor for error handling and retry logic
  axiosInstance.interceptors.response.use(
    (response) => {
      // Successful response - just return it
      return response;
    },
    async (error: AxiosError) => {
      const config = error.config as InternalAxiosRequestConfig;

      // Handle retry logic for retryable errors
      if (shouldRetry(error) && config) {
        const retryCount = config._retryCount || 0;

        if (retryCount < MAX_RETRIES) {
          config._retryCount = retryCount + 1;

          // Exponential backoff
          const delayMs = RETRY_DELAY * Math.pow(2, retryCount);
          await delay(delayMs);

          console.log(`Retrying request (attempt ${config._retryCount}/${MAX_RETRIES})`);
          return axiosInstance(config);
        }
      }

      // Convert to ApiError
      const apiError = convertToApiError(error);

      // Handle specific error types
      if (apiError.isUnauthorizedError()) {
        // Redirect to login or refresh token
        // This will be handled by the component that catches this error
        console.log('Unauthorized error - authentication required');
      } else if (apiError.isForbiddenError()) {
        console.log('Forbidden error - access denied');
      } else if (apiError.isNotFoundError()) {
        console.log('Not found error');
      } else if (apiError.isServerError()) {
        console.error('Server error:', apiError.correlationId, apiError.message);
      }

      return Promise.reject(apiError);
    }
  );
}

/**
 * Creates an Axios instance with error interceptor configured
 */
export function createApiClient(baseURL: string): AxiosInstance {
  const client = axios.create({
    baseURL,
    timeout: 30000, // 30 seconds
    headers: {
      'Content-Type': 'application/json',
    },
  });

  setupErrorInterceptor(client);

  return client;
}
