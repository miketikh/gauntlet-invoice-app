/**
 * Authentication Store (Zustand)
 * Manages authentication state with persistence and auto-refresh
 */

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { authApi } from '../api/auth';
import type { User, AuthState } from '../api/types';

// Extended auth state with actions
interface AuthStore extends AuthState {
  // Hydration state
  hasHydrated: boolean;
  setHasHydrated: (state: boolean) => void;

  // Actions
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  setTokens: (token: string, refreshToken: string, expiresIn: number, user: User) => void;
  refreshAuth: () => Promise<boolean>;
  checkTokenExpiry: () => boolean;
  clearAuth: () => void;
}

// Initial auth state
const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  tokenExpiresAt: null,
};

/**
 * Calculate token expiration timestamp
 */
const calculateTokenExpiry = (expiresIn: number): number => {
  // expiresIn is in seconds, add to current timestamp
  // Subtract 60 seconds as buffer to refresh before actual expiry
  return Date.now() + (expiresIn - 60) * 1000;
};

/**
 * Create the auth store with persistence
 */
export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // Initial state
      ...initialState,
      hasHydrated: false,

      /**
       * Set hydration state
       */
      setHasHydrated: (state: boolean) => {
        set({ hasHydrated: state });
      },

      /**
       * Login action
       * @param username - User's username
       * @param password - User's password
       * @throws AuthError on login failure
       */
      login: async (username: string, password: string) => {
        try {
          const response = await authApi.login(username, password);

          const tokenExpiresAt = calculateTokenExpiry(response.expiresIn);

          set({
            user: { username },
            token: response.token,
            refreshToken: response.refreshToken,
            tokenExpiresAt,
            isAuthenticated: true,
          });
        } catch (error) {
          // Clear auth state on error
          get().clearAuth();

          // Re-throw the error for the caller to handle
          throw error;
        }
      },

      /**
       * Logout action - clear all auth state
       */
      logout: () => {
        // Call API logout (currently a no-op as backend is stateless)
        authApi.logout();

        // Clear state
        get().clearAuth();
      },

      /**
       * Set tokens manually (used after refresh)
       */
      setTokens: (token: string, refreshToken: string, expiresIn: number, user: User) => {
        const tokenExpiresAt = calculateTokenExpiry(expiresIn);

        set({
          user,
          token,
          refreshToken,
          tokenExpiresAt,
          isAuthenticated: true,
        });
      },

      /**
       * Refresh authentication tokens
       * @returns true if refresh successful, false otherwise
       */
      refreshAuth: async (): Promise<boolean> => {
        const state = get();

        // Check if we have a refresh token
        if (!state.refreshToken) {
          get().clearAuth();
          return false;
        }

        try {
          const response = await authApi.refresh(state.refreshToken);

          const tokenExpiresAt = calculateTokenExpiry(response.expiresIn);

          set({
            token: response.token,
            refreshToken: response.refreshToken,
            tokenExpiresAt,
            isAuthenticated: true,
          });

          return true;
        } catch (error) {
          // Refresh failed - clear auth state
          console.error('Token refresh failed:', error);
          get().clearAuth();
          return false;
        }
      },

      /**
       * Check if token is expired or about to expire
       * @returns true if token is valid, false if expired/about to expire
       */
      checkTokenExpiry: (): boolean => {
        const state = get();

        if (!state.tokenExpiresAt || !state.token) {
          return false;
        }

        // Check if token is expired or will expire in the next 60 seconds
        const now = Date.now();
        return state.tokenExpiresAt > now;
      },

      /**
       * Clear all authentication state
       */
      clearAuth: () => {
        set(initialState);
      },
    }),
    {
      name: 'auth-storage', // localStorage key
      storage: createJSONStorage(() => localStorage),
      // Only persist these fields
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        tokenExpiresAt: state.tokenExpiresAt,
        isAuthenticated: state.isAuthenticated,
      }),
      // Set hydration flag when rehydration completes
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);

/**
 * Auto-refresh hook - check token expiry and refresh if needed
 * Call this in your app's root component or auth guard
 */
export const useAutoRefresh = () => {
  const { checkTokenExpiry, refreshAuth, isAuthenticated } = useAuthStore();

  // Set up interval to check token expiry every 30 seconds
  if (typeof window !== 'undefined' && isAuthenticated) {
    const interval = setInterval(async () => {
      const isValid = checkTokenExpiry();

      if (!isValid) {
        // Token expired or about to expire, try to refresh
        const refreshed = await refreshAuth();

        if (!refreshed) {
          // Refresh failed, user will need to login again
          console.warn('Auto-refresh failed, user needs to login');
        }
      }
    }, 30000); // Check every 30 seconds

    // Cleanup interval on unmount
    return () => clearInterval(interval);
  }
};

/**
 * Selector hooks for common use cases
 */
export const useAuth = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const hasHydrated = useAuthStore((state) => state.hasHydrated);
  return { isAuthenticated, user, hasHydrated };
};

export const useAuthActions = () => {
  const login = useAuthStore((state) => state.login);
  const logout = useAuthStore((state) => state.logout);
  const refreshAuth = useAuthStore((state) => state.refreshAuth);
  return { login, logout, refreshAuth };
};
