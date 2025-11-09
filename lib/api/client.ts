/**
 * Main API Client with Axios Interceptors
 * Handles token injection and automatic token refresh
 */

import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from './types';
import { setupErrorInterceptor } from './error-interceptor';

// API base URL
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Create the main API client with interceptors
 */
export const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: 15000, // 15 second timeout
  });

  // Setup error interceptor first (for retry logic and error conversion)
  setupErrorInterceptor(client);

  // Request interceptor - inject auth token
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // Get token from localStorage
      const token = getStoredToken();

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // Response interceptor - handle 401 and auto-refresh
  client.interceptors.response.use(
    (response) => {
      // Success response, return as-is
      return response;
    },
    async (error: AxiosError<ApiError>) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & {
        _retry?: boolean;
      };

      // If error is 401 and we haven't retried yet, try to refresh token
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
          // Get refresh token from localStorage
          const refreshToken = getStoredRefreshToken();

          if (!refreshToken) {
            // No refresh token available, redirect to login
            handleAuthFailure();
            return Promise.reject(error);
          }

          // Attempt to refresh the token
          const newTokens = await refreshAccessToken(refreshToken);

          if (newTokens) {
            // Update stored tokens
            updateStoredTokens(newTokens.token, newTokens.refreshToken, newTokens.expiresIn);

            // Retry the original request with new token
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newTokens.token}`;
            }
            return client(originalRequest);
          } else {
            // Refresh failed, redirect to login
            handleAuthFailure();
            return Promise.reject(error);
          }
        } catch (refreshError) {
          // Refresh token is invalid or expired, redirect to login
          handleAuthFailure();
          return Promise.reject(refreshError);
        }
      }

      // For other errors, just reject
      return Promise.reject(error);
    }
  );

  return client;
};

/**
 * Helper: Get token from localStorage
 */
const getStoredToken = (): string | null => {
  if (typeof window === 'undefined') return null;

  try {
    const authState = localStorage.getItem('auth-storage');
    if (authState) {
      const parsed = JSON.parse(authState);
      return parsed.state?.token || null;
    }
  } catch (error) {
    console.error('Error reading token from localStorage:', error);
  }

  return null;
};

/**
 * Helper: Get refresh token from localStorage
 */
const getStoredRefreshToken = (): string | null => {
  if (typeof window === 'undefined') return null;

  try {
    const authState = localStorage.getItem('auth-storage');
    if (authState) {
      const parsed = JSON.parse(authState);
      return parsed.state?.refreshToken || null;
    }
  } catch (error) {
    console.error('Error reading refresh token from localStorage:', error);
  }

  return null;
};

/**
 * Helper: Refresh access token
 */
const refreshAccessToken = async (
  refreshToken: string
): Promise<{ token: string; refreshToken: string; expiresIn: number } | null> => {
  try {
    // Use a fresh axios instance to avoid interceptor recursion
    const response = await axios.post(
      `${API_BASE_URL}/auth/refresh`,
      { refreshToken },
      {
        headers: { 'Content-Type': 'application/json' },
      }
    );
    return response.data;
  } catch (error) {
    console.error('Token refresh failed:', error);
    return null;
  }
};

/**
 * Helper: Update stored tokens
 */
const updateStoredTokens = (
  token: string,
  refreshToken: string,
  expiresIn: number
): void => {
  if (typeof window === 'undefined') return;

  try {
    const authState = localStorage.getItem('auth-storage');
    if (authState) {
      const parsed = JSON.parse(authState);
      const tokenExpiresAt = Date.now() + expiresIn * 1000;

      parsed.state = {
        ...parsed.state,
        token,
        refreshToken,
        tokenExpiresAt,
      };

      localStorage.setItem('auth-storage', JSON.stringify(parsed));
    }
  } catch (error) {
    console.error('Error updating tokens in localStorage:', error);
  }
};

/**
 * Helper: Handle authentication failure
 */
const handleAuthFailure = (): void => {
  if (typeof window === 'undefined') return;

  // Clear auth state
  localStorage.removeItem('auth-storage');

  // Redirect to login page if not already there
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
};

/**
 * Create and export the main API client instance
 */
export const apiClient = createApiClient();
