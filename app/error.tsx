'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { AlertCircle } from 'lucide-react';

/**
 * Global Error Page
 * Catches errors that occur in the app directory
 */
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error to console and external error tracking service
    console.error('Global error page:', error);
    // TODO: Log to error tracking service (Sentry, LogRocket, etc.)
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background">
      <div className="text-center">
        {/* Icon */}
        <div className="mb-8 flex justify-center">
          <div className="rounded-full bg-destructive/10 p-6">
            <AlertCircle className="h-20 w-20 text-destructive" />
          </div>
        </div>

        {/* Error Code */}
        <h1 className="mb-2 text-6xl font-bold tracking-tight">500</h1>

        {/* Title */}
        <h2 className="mb-4 text-2xl font-semibold">Server Error</h2>

        {/* Description */}
        <p className="mb-4 max-w-md text-muted-foreground">
          An unexpected error occurred. Please try again.
        </p>

        {/* Error ID for support */}
        {error.digest && (
          <p className="mb-8 text-sm text-muted-foreground">
            Error ID: <span className="font-mono">{error.digest}</span>
          </p>
        )}

        {/* Development mode: Show error details */}
        {process.env.NODE_ENV === 'development' && (
          <div className="mb-8 max-w-2xl overflow-auto rounded-lg bg-muted p-4 text-left">
            <p className="mb-2 font-mono text-sm font-semibold text-destructive">
              {error.name}: {error.message}
            </p>
            {error.stack && (
              <pre className="text-xs text-muted-foreground">
                {error.stack}
              </pre>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-center gap-4">
          <Button onClick={() => window.location.href = '/'} variant="outline">
            Go Home
          </Button>
          <Button onClick={reset}>
            Try Again
          </Button>
        </div>

        {/* Support message */}
        <p className="mt-8 text-sm text-muted-foreground">
          If this problem persists, please contact support.
        </p>
      </div>
    </div>
  );
}
