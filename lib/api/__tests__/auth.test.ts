/**
 * Tests for Auth API Client
 * Tests login, refresh, and error handling
 */

import { authApi, AuthError } from '../auth';
import axios from 'axios';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Create a mock axios instance
const mockAxiosInstance = {
  post: jest.fn(),
  get: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  request: jest.fn(),
  defaults: {},
  interceptors: {
    request: { use: jest.fn(), eject: jest.fn() },
    response: { use: jest.fn(), eject: jest.fn() },
  },
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
mockedAxios.create = jest.fn(() => mockAxiosInstance as any);

describe('Auth API Client', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', async () => {
      const mockResponse = {
        data: {
          token: 'test-access-token',
          refreshToken: 'test-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
        },
      };

      mockAxiosInstance.post.mockResolvedValueOnce(mockResponse);

      const result = await authApi.login('admin', 'admin123');

      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/auth/login', {
        username: 'admin',
        password: 'admin123',
      });

      expect(result).toEqual(mockResponse.data);
      expect(result.token).toBe('test-access-token');
      expect(result.refreshToken).toBe('test-refresh-token');
    });

    it('should throw AuthError on invalid credentials', async () => {
      const errorResponse = {
        response: {
          status: 401,
          data: {
            message: 'Invalid username or password',
          },
        },
      };

      mockAxiosInstance.post.mockRejectedValueOnce(errorResponse);

      await expect(authApi.login('wrong', 'password')).rejects.toThrow(AuthError);

      try {
        await authApi.login('wrong', 'password');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).status).toBe(401);
        expect((error as AuthError).message).toBe('Invalid username or password');
      }
    });

    it('should throw AuthError on network failure', async () => {
      const networkError = {
        request: {},
        message: 'Network Error',
      };

      mockAxiosInstance.post.mockRejectedValueOnce(networkError);

      await expect(authApi.login('admin', 'admin123')).rejects.toThrow(AuthError);

      try {
        await authApi.login('admin', 'admin123');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).message).toContain('Network error');
      }
    });

    it('should throw AuthError on server error (500)', async () => {
      const serverError = {
        response: {
          status: 500,
          data: {
            message: 'Internal server error',
          },
        },
      };

      mockAxiosInstance.post.mockRejectedValueOnce(serverError);

      await expect(authApi.login('admin', 'admin123')).rejects.toThrow(AuthError);

      try {
        await authApi.login('admin', 'admin123');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).status).toBe(500);
      }
    });

    it('should handle unknown errors', async () => {
      const unknownError = new Error('Something went wrong');

      mockAxiosInstance.post.mockRejectedValueOnce(unknownError);

      await expect(authApi.login('admin', 'admin123')).rejects.toThrow(AuthError);

      try {
        await authApi.login('admin', 'admin123');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).message).toBe('Something went wrong');
      }
    });
  });

  describe('refresh', () => {
    it('should successfully refresh token with valid refresh token', async () => {
      const mockResponse = {
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
        },
      };

      mockAxiosInstance.post.mockResolvedValueOnce(mockResponse);

      const result = await authApi.refresh('valid-refresh-token');

      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/auth/refresh', {
        refreshToken: 'valid-refresh-token',
      });

      expect(result).toEqual(mockResponse.data);
      expect(result.token).toBe('new-access-token');
      expect(result.refreshToken).toBe('new-refresh-token');
    });

    it('should throw AuthError on invalid refresh token', async () => {
      const errorResponse = {
        response: {
          status: 401,
          data: {
            message: 'Invalid refresh token',
          },
        },
      };

      mockAxiosInstance.post.mockRejectedValueOnce(errorResponse);

      await expect(authApi.refresh('invalid-token')).rejects.toThrow(AuthError);

      try {
        await authApi.refresh('invalid-token');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).status).toBe(401);
        expect((error as AuthError).message).toBe('Invalid refresh token');
      }
    });

    it('should throw AuthError on expired refresh token', async () => {
      const errorResponse = {
        response: {
          status: 401,
          data: {
            message: 'Refresh token expired',
          },
        },
      };

      mockAxiosInstance.post.mockRejectedValueOnce(errorResponse);

      await expect(authApi.refresh('expired-token')).rejects.toThrow(AuthError);
    });

    it('should handle network errors during refresh', async () => {
      const networkError = {
        request: {},
        message: 'Network Error',
      };

      mockAxiosInstance.post.mockRejectedValueOnce(networkError);

      await expect(authApi.refresh('token')).rejects.toThrow(AuthError);

      try {
        await authApi.refresh('token');
      } catch (error) {
        expect(error).toBeInstanceOf(AuthError);
        expect((error as AuthError).message).toContain('Network error');
      }
    });
  });

  describe('logout', () => {
    it('should resolve successfully (client-side only)', async () => {
      await expect(authApi.logout()).resolves.toBeUndefined();
    });

    it('should not make any API calls', async () => {
      await authApi.logout();

      expect(mockAxiosInstance.post).not.toHaveBeenCalled();
      expect(mockAxiosInstance.get).not.toHaveBeenCalled();
    });
  });

  describe('AuthError class', () => {
    it('should create AuthError with all parameters', () => {
      const apiError = { message: 'Test error' };
      const error = new AuthError('Test message', 401, apiError);

      expect(error.message).toBe('Test message');
      expect(error.status).toBe(401);
      expect(error.apiError).toEqual(apiError);
      expect(error.name).toBe('AuthError');
    });

    it('should create AuthError without status', () => {
      const error = new AuthError('Test message');

      expect(error.message).toBe('Test message');
      expect(error.status).toBeUndefined();
      expect(error.apiError).toBeUndefined();
    });

    it('should be instance of Error', () => {
      const error = new AuthError('Test');

      expect(error).toBeInstanceOf(Error);
      expect(error).toBeInstanceOf(AuthError);
    });
  });
});
