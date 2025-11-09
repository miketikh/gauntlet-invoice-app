/**
 * Authentication API Client
 * Handles all authentication-related API calls to the backend
 */

import axios, { AxiosInstance, AxiosError } from 'axios';
import type {
  LoginRequest,
  LoginResponse,
  RefreshRequest,
  RefreshResponse,
  ApiError,
} from './types';

// API base URL - backend running on localhost:8080
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Create axios instance for authentication API
 */
const createAuthClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: 10000, // 10 second timeout
  });

  return client;
};

// Create the auth API client instance
const authClient = createAuthClient();

/**
 * Custom error class for authentication errors
 */
export class AuthError extends Error {
  public status?: number;
  public apiError?: ApiError;

  constructor(message: string, status?: number, apiError?: ApiError) {
    super(message);
    this.name = 'AuthError';
    this.status = status;
    this.apiError = apiError;
  }
}

/**
 * Handle API errors and convert to AuthError
 */
const handleAuthError = (error: unknown): never => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiError>;

    if (axiosError.response) {
      // Server responded with error status
      const apiError = axiosError.response.data;
      throw new AuthError(
        apiError?.message || 'Authentication failed',
        axiosError.response.status,
        apiError
      );
    } else if (axiosError.request) {
      // Request made but no response received
      throw new AuthError(
        'Network error: Unable to reach authentication server',
        undefined,
        { message: 'Network error' }
      );
    }
  }

  // Unknown error
  throw new AuthError(
    error instanceof Error ? error.message : 'An unexpected error occurred',
    undefined,
    { message: 'Unknown error' }
  );
};

/**
 * Authentication API methods
 */
export const authApi = {
  /**
   * Login with username and password
   * @param username - User's username
   * @param password - User's password
   * @returns LoginResponse with tokens and expiry time
   * @throws AuthError on failure
   */
  async login(username: string, password: string): Promise<LoginResponse> {
    try {
      const request: LoginRequest = { username, password };
      const response = await authClient.post<LoginResponse>('/auth/login', request);
      return response.data;
    } catch (error) {
      return handleAuthError(error);
    }
  },

  /**
   * Refresh access token using refresh token
   * @param refreshToken - The refresh token
   * @returns RefreshResponse with new tokens
   * @throws AuthError on failure
   */
  async refresh(refreshToken: string): Promise<RefreshResponse> {
    try {
      const request: RefreshRequest = { refreshToken };
      const response = await authClient.post<RefreshResponse>('/auth/refresh', request);
      return response.data;
    } catch (error) {
      return handleAuthError(error);
    }
  },

  /**
   * Logout - client-side only (clears local state)
   * Backend doesn't have a logout endpoint as JWT is stateless
   */
  async logout(): Promise<void> {
    // This is client-side only - just a placeholder for consistency
    // The actual logout logic happens in the auth store
    return Promise.resolve();
  },
};

/**
 * Export the axios instance for use in interceptors
 */
export { authClient };
