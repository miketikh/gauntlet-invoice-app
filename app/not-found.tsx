import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { FileQuestion } from 'lucide-react';

/**
 * 404 Not Found Page
 * Displayed when a route doesn't exist
 */
export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background">
      <div className="text-center">
        {/* Icon */}
        <div className="mb-8 flex justify-center">
          <div className="rounded-full bg-muted p-6">
            <FileQuestion className="h-20 w-20 text-muted-foreground" />
          </div>
        </div>

        {/* Error Code */}
        <h1 className="mb-2 text-6xl font-bold tracking-tight">404</h1>

        {/* Title */}
        <h2 className="mb-4 text-2xl font-semibold">Page Not Found</h2>

        {/* Description */}
        <p className="mb-8 max-w-md text-muted-foreground">
          The page you're looking for doesn't exist or has been moved.
        </p>

        {/* Actions */}
        <div className="flex justify-center gap-4">
          <Button asChild variant="outline">
            <Link href="/login">Go to Login</Link>
          </Button>
          <Button asChild>
            <Link href="/">Go Home</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
