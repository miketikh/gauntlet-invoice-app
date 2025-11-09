"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/lib/stores/auth-store";

/**
 * Auth Provider Component
 *
 * This component initializes the auth state on app load and sets up auto-refresh.
 * It should wrap the entire application in the root layout.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { checkTokenExpiry, refreshAuth, isAuthenticated } = useAuthStore();

  useEffect(() => {
    // Check if we have stored auth and if token needs refresh on mount
    const initAuth = async () => {
      if (isAuthenticated) {
        const isTokenValid = checkTokenExpiry();

        if (!isTokenValid) {
          // Token expired or about to expire, try to refresh
          await refreshAuth();
        }
      }
    };

    initAuth();

    // Set up interval to check token expiry every 30 seconds
    const interval = setInterval(async () => {
      if (isAuthenticated) {
        const isValid = checkTokenExpiry();

        if (!isValid) {
          // Token expired or about to expire, try to refresh
          const refreshed = await refreshAuth();

          if (!refreshed) {
            // Refresh failed, user will need to login again
            console.warn("Auto-refresh failed, user needs to login");
          }
        }
      }
    }, 30000); // Check every 30 seconds

    // Cleanup interval on unmount
    return () => clearInterval(interval);
  }, [isAuthenticated, checkTokenExpiry, refreshAuth]);

  return <>{children}</>;
}
