import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Next.js Middleware for Protected Routes
 *
 * This middleware runs on the Edge runtime before any page renders.
 * It checks authentication status and redirects as needed.
 *
 * Note: Middleware cannot directly access browser localStorage or Zustand stores,
 * so we check for the presence of the auth token in cookies or headers.
 * Client-side route protection is handled by the useRequireAuth hook.
 */

// Routes that require authentication
const PROTECTED_ROUTES = [
  '/dashboard',
  '/invoices',
  '/customers',
  '/settings',
];

// Routes that are publicly accessible (defined for future use)
// const PUBLIC_ROUTES = [
//   '/',
//   '/login',
// ];

/**
 * Helper to check if a path matches any of the given route patterns
 */
function matchesRoute(pathname: string, routes: string[]): boolean {
  return routes.some((route) => {
    // Exact match or starts with route path (for nested routes)
    return pathname === route || pathname.startsWith(`${route}/`);
  });
}

/**
 * Check if user is authenticated by looking for auth data
 * Since middleware runs on the edge, we can't access localStorage directly.
 * We'll check cookies or local storage via request headers if available.
 */
function isAuthenticated(request: NextRequest): boolean {
  // Try to get auth token from cookies
  const authCookie = request.cookies.get('auth-token');

  // For now, since we're using localStorage in the client,
  // we'll rely on the client-side hook for protection
  // and only use this middleware as a first-level check

  // If there's a cookie, user might be authenticated
  if (authCookie) {
    return true;
  }

  // Check for authorization header (for API routes)
  const authHeader = request.headers.get('authorization');
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return true;
  }

  // Since we're using localStorage, we can't reliably check auth in middleware
  // This middleware will serve as a baseline, but client-side protection is primary
  return false;
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Check if the route is protected
  const isProtectedRoute = matchesRoute(pathname, PROTECTED_ROUTES);

  // If it's a protected route, check authentication
  if (isProtectedRoute) {
    // Note: Since we're using localStorage for auth state,
    // we can't reliably check authentication in middleware
    // The primary protection will come from the useRequireAuth hook
    // This middleware serves as a baseline check

    // For now, we'll allow the request through and let client-side
    // protection handle it, since localStorage isn't accessible in middleware

    // In a production app, you'd want to use httpOnly cookies
    // that can be checked here in the middleware

    return NextResponse.next();
  }

  // If user is trying to access login while authenticated, redirect to dashboard
  if (pathname === '/login') {
    // Check if authenticated (this won't work reliably with localStorage)
    // This is a placeholder - in production use cookies
    if (isAuthenticated(request)) {
      return NextResponse.redirect(new URL('/dashboard', request.url));
    }
  }

  // Allow all other requests
  return NextResponse.next();
}

// Configure which routes this middleware runs on
export const config = {
  // Match all routes except static files and api routes
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (images, etc.)
     */
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\.png$|.*\\.jpg$|.*\\.jpeg$|.*\\.svg$|.*\\.gif$).*)',
  ],
};
