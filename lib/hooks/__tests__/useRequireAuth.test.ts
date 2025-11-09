/**
 * Tests for useRequireAuth Hook
 * Tests authentication checking, redirects, and token refresh
 */

import { renderHook, waitFor } from '@testing-library/react';
import { useRequireAuth } from '../useRequireAuth';
import { useAuthStore } from '@/lib/stores/auth-store';
import { useRouter } from 'next/navigation';

// Mock dependencies
jest.mock('next/navigation');
jest.mock('@/lib/stores/auth-store');

const mockPush = jest.fn();
const mockCheckTokenExpiry = jest.fn();
const mockRefreshAuth = jest.fn();

// Mock window.location.pathname
// eslint-disable-next-line @typescript-eslint/no-explicit-any
delete (window as any).location;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
window.location = { pathname: '/dashboard' } as any;

describe('useRequireAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      prefetch: jest.fn(),
      back: jest.fn(),
    });

    (useAuthStore as unknown as jest.Mock).mockReturnValue({
      isAuthenticated: true,
      checkTokenExpiry: mockCheckTokenExpiry,
      refreshAuth: mockRefreshAuth,
    });
  });

  describe('Authenticated User', () => {
    it('should allow access for authenticated user with valid token', async () => {
      mockCheckTokenExpiry.mockReturnValue(true);

      const { result } = renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(mockPush).not.toHaveBeenCalled();
      expect(mockRefreshAuth).not.toHaveBeenCalled();
    });

    it('should refresh token if expired', async () => {
      mockCheckTokenExpiry.mockReturnValue(false);
      mockRefreshAuth.mockResolvedValue(true);

      const { result } = renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockRefreshAuth).toHaveBeenCalled();
      expect(mockPush).not.toHaveBeenCalled();
      expect(result.current.isAuthenticated).toBe(true);
    });

    it('should redirect if token refresh fails', async () => {
      mockCheckTokenExpiry.mockReturnValue(false);
      mockRefreshAuth.mockResolvedValue(false);

      renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(mockRefreshAuth).toHaveBeenCalled();
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('/login')
        );
      });
    });
  });

  describe('Unauthenticated User', () => {
    it('should redirect to login if not authenticated', async () => {
      (useAuthStore as unknown as jest.Mock).mockReturnValue({
        isAuthenticated: false,
        checkTokenExpiry: mockCheckTokenExpiry,
        refreshAuth: mockRefreshAuth,
      });

      renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('/login')
        );
      });
    });

    it('should include current path in redirect URL', async () => {
      (useAuthStore as unknown as jest.Mock).mockReturnValue({
        isAuthenticated: false,
        checkTokenExpiry: mockCheckTokenExpiry,
        refreshAuth: mockRefreshAuth,
      });

      window.location.pathname = '/dashboard';

      renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('from=%2Fdashboard')
        );
      });
    });

    it('should use custom redirect path if provided', async () => {
      (useAuthStore as unknown as jest.Mock).mockReturnValue({
        isAuthenticated: false,
        checkTokenExpiry: mockCheckTokenExpiry,
        refreshAuth: mockRefreshAuth,
      });

      renderHook(() => useRequireAuth('/custom-page'));

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('from=%2Fcustom-page')
        );
      });
    });
  });

  describe('Loading State', () => {
    it('should start with loading true', () => {
      const { result } = renderHook(() => useRequireAuth());

      expect(result.current.isLoading).toBe(true);
    });

    it('should set loading to false after auth check', async () => {
      mockCheckTokenExpiry.mockReturnValue(true);

      const { result } = renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe('Error Handling', () => {
    it('should redirect on error during auth check', async () => {
      mockCheckTokenExpiry.mockImplementation(() => {
        throw new Error('Token check failed');
      });

      renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('/login')
        );
      });
    });

    it('should handle refresh errors gracefully', async () => {
      mockCheckTokenExpiry.mockReturnValue(false);
      mockRefreshAuth.mockRejectedValue(new Error('Refresh failed'));

      renderHook(() => useRequireAuth());

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith(
          expect.stringContaining('/login')
        );
      });
    });
  });
});
