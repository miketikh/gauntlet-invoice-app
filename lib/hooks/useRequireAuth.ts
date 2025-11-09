"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../stores/auth-store';

/**
 * Hook for client-side route protection
 *
 * This hook checks if the user is authenticated and redirects to /login if not.
 * Use this in page components that require authentication.
 *
 * @param redirectTo - Optional redirect path after login (defaults to current path)
 * @returns Object with isLoading state and isAuthenticated status
 *
 * @example
 * ```tsx
 * export default function ProtectedPage() {
 *   const { isLoading } = useRequireAuth();
 *
 *   if (isLoading) {
 *     return <div>Loading...</div>;
 *   }
 *
 *   return <div>Protected content</div>;
 * }
 * ```
 */
export function useRequireAuth(redirectTo?: string) {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const { isAuthenticated, checkTokenExpiry, refreshAuth, hasHydrated } = useAuthStore();

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // CRITICAL: Wait for Zustand to rehydrate from localStorage before checking auth
        if (!hasHydrated) {
          // Store hasn't loaded from localStorage yet, keep loading
          return;
        }

        // Check if user is authenticated
        if (!isAuthenticated) {
          // Not authenticated - redirect to login
          const currentPath = redirectTo || window.location.pathname;
          router.push(`/login?from=${encodeURIComponent(currentPath)}`);
          return;
        }

        // Check if token is expired
        const isTokenValid = checkTokenExpiry();

        if (!isTokenValid) {
          // Token expired - try to refresh
          const refreshed = await refreshAuth();

          if (!refreshed) {
            // Refresh failed - redirect to login
            const currentPath = redirectTo || window.location.pathname;
            router.push(`/login?from=${encodeURIComponent(currentPath)}`);
            return;
          }
        }

        // Auth check complete
        setIsLoading(false);
      } catch (error) {
        console.error('Auth check failed:', error);
        // On error, redirect to login
        const currentPath = redirectTo || window.location.pathname;
        router.push(`/login?from=${encodeURIComponent(currentPath)}`);
      }
    };

    checkAuth();
  }, [isAuthenticated, checkTokenExpiry, refreshAuth, router, redirectTo, hasHydrated]);

  return {
    isLoading,
    isAuthenticated,
  };
}
