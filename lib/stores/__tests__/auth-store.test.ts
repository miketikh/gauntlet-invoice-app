/**
 * Tests for Auth Store (Zustand)
 * Tests login, logout, token refresh, and persistence
 */

import { useAuthStore } from '../auth-store';
import { authApi, AuthError } from '../../api/auth';

// Mock the auth API
jest.mock('../../api/auth');
const mockedAuthApi = authApi as jest.Mocked<typeof authApi>;

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

describe('Auth Store', () => {
  beforeEach(() => {
    // Clear the store before each test
    useAuthStore.getState().clearAuth();
    localStorage.clear();
    jest.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const state = useAuthStore.getState();

      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.isAuthenticated).toBe(false);
      expect(state.tokenExpiresAt).toBeNull();
    });
  });

  describe('login', () => {
    it('should successfully login and update state', async () => {
      const mockResponse = {
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600, // 1 hour
      };

      mockedAuthApi.login.mockResolvedValueOnce(mockResponse);

      const { login } = useAuthStore.getState();
      await login('admin', 'admin123');

      const state = useAuthStore.getState();

      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual({ username: 'admin' });
      expect(state.token).toBe('test-token');
      expect(state.refreshToken).toBe('test-refresh-token');
      expect(state.tokenExpiresAt).toBeGreaterThan(Date.now());
    });

    it('should clear auth state on login failure', async () => {
      const error = new AuthError('Invalid credentials', 401);
      mockedAuthApi.login.mockRejectedValueOnce(error);

      const { login } = useAuthStore.getState();

      await expect(login('wrong', 'password')).rejects.toThrow(AuthError);

      const state = useAuthStore.getState();

      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
    });

    it('should call API with correct credentials', async () => {
      const mockResponse = {
        token: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
      };

      mockedAuthApi.login.mockResolvedValueOnce(mockResponse);

      const { login } = useAuthStore.getState();
      await login('testuser', 'testpass');

      expect(mockedAuthApi.login).toHaveBeenCalledWith('testuser', 'testpass');
      expect(mockedAuthApi.login).toHaveBeenCalledTimes(1);
    });
  });

  describe('logout', () => {
    it('should clear all auth state on logout', async () => {
      // First login
      const mockResponse = {
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      };

      mockedAuthApi.login.mockResolvedValueOnce(mockResponse);

      const { login, logout } = useAuthStore.getState();
      await login('admin', 'admin123');

      // Verify logged in
      expect(useAuthStore.getState().isAuthenticated).toBe(true);

      // Now logout
      logout();

      const state = useAuthStore.getState();

      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.tokenExpiresAt).toBeNull();
    });

    it('should call API logout method', () => {
      const { logout } = useAuthStore.getState();
      logout();

      expect(mockedAuthApi.logout).toHaveBeenCalled();
    });
  });

  describe('setTokens', () => {
    it('should set tokens and user correctly', () => {
      const { setTokens } = useAuthStore.getState();

      setTokens(
        'new-token',
        'new-refresh-token',
        3600,
        { username: 'testuser' }
      );

      const state = useAuthStore.getState();

      expect(state.token).toBe('new-token');
      expect(state.refreshToken).toBe('new-refresh-token');
      expect(state.user).toEqual({ username: 'testuser' });
      expect(state.isAuthenticated).toBe(true);
      expect(state.tokenExpiresAt).toBeGreaterThan(Date.now());
    });
  });

  describe('refreshAuth', () => {
    it('should successfully refresh tokens', async () => {
      // Setup initial state
      const { setTokens } = useAuthStore.getState();
      setTokens('old-token', 'old-refresh-token', 3600, { username: 'admin' });

      // Mock refresh response
      const mockRefreshResponse = {
        token: 'new-token',
        refreshToken: 'new-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      };

      mockedAuthApi.refresh.mockResolvedValueOnce(mockRefreshResponse);

      const { refreshAuth } = useAuthStore.getState();
      const result = await refreshAuth();

      expect(result).toBe(true);

      const state = useAuthStore.getState();

      expect(state.token).toBe('new-token');
      expect(state.refreshToken).toBe('new-refresh-token');
      expect(state.isAuthenticated).toBe(true);
    });

    it('should clear auth on refresh failure', async () => {
      const { setTokens } = useAuthStore.getState();
      setTokens('token', 'refresh-token', 3600, { username: 'admin' });

      const error = new AuthError('Refresh token expired', 401);
      mockedAuthApi.refresh.mockRejectedValueOnce(error);

      const { refreshAuth } = useAuthStore.getState();
      const result = await refreshAuth();

      expect(result).toBe(false);

      const state = useAuthStore.getState();

      expect(state.isAuthenticated).toBe(false);
      expect(state.token).toBeNull();
    });

    it('should return false when no refresh token available', async () => {
      // No tokens set
      const { refreshAuth } = useAuthStore.getState();
      const result = await refreshAuth();

      expect(result).toBe(false);
      expect(mockedAuthApi.refresh).not.toHaveBeenCalled();
    });
  });

  describe('checkTokenExpiry', () => {
    it('should return true for valid token', () => {
      // Set up token with future expiry
      const { setTokens } = useAuthStore.getState();
      setTokens('token', 'refresh', 3600, { username: 'admin' });

      const { checkTokenExpiry } = useAuthStore.getState();
      const isValid = checkTokenExpiry();

      expect(isValid).toBe(true);
    });

    it('should return false for expired token', () => {
      // Manually set expired token
      useAuthStore.setState({
        token: 'token',
        tokenExpiresAt: Date.now() - 1000, // 1 second ago
        isAuthenticated: true,
        user: { username: 'admin' },
        refreshToken: 'refresh',
      });

      const { checkTokenExpiry } = useAuthStore.getState();
      const isValid = checkTokenExpiry();

      expect(isValid).toBe(false);
    });

    it('should return false when no token', () => {
      const { checkTokenExpiry } = useAuthStore.getState();
      const isValid = checkTokenExpiry();

      expect(isValid).toBe(false);
    });

    it('should return false when no expiry time', () => {
      useAuthStore.setState({
        token: 'token',
        tokenExpiresAt: null,
        isAuthenticated: true,
        user: { username: 'admin' },
        refreshToken: 'refresh',
      });

      const { checkTokenExpiry } = useAuthStore.getState();
      const isValid = checkTokenExpiry();

      expect(isValid).toBe(false);
    });
  });

  describe('clearAuth', () => {
    it('should reset to initial state', () => {
      // Set some state first
      const { setTokens, clearAuth } = useAuthStore.getState();
      setTokens('token', 'refresh', 3600, { username: 'admin' });

      // Clear it
      clearAuth();

      const state = useAuthStore.getState();

      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.isAuthenticated).toBe(false);
      expect(state.tokenExpiresAt).toBeNull();
    });
  });

  describe('Persistence', () => {
    it('should persist auth state to localStorage', async () => {
      const mockResponse = {
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      };

      mockedAuthApi.login.mockResolvedValueOnce(mockResponse);

      const { login } = useAuthStore.getState();
      await login('admin', 'admin123');

      // Check localStorage
      const stored = localStorage.getItem('auth-storage');
      expect(stored).toBeTruthy();

      if (stored) {
        const parsedState = JSON.parse(stored);
        expect(parsedState.state.token).toBe('test-token');
        expect(parsedState.state.user.username).toBe('admin');
      }
    });
  });
});
