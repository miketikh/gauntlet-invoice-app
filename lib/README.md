# Authentication Service Layer

This directory contains the frontend authentication service layer for the InvoiceMe application.

## Structure

```
lib/
├── api/
│   ├── auth.ts        # Authentication API client
│   ├── client.ts      # Main API client with interceptors
│   ├── types.ts       # TypeScript interfaces for API contracts
│   └── index.ts       # API module exports
├── stores/
│   ├── auth-store.ts  # Zustand authentication store
│   └── index.ts       # Store module exports
└── utils.ts           # Utility functions (existing)
```

## Features

### API Client (`lib/api/`)

#### Authentication API (`auth.ts`)
- **login(username, password)**: Authenticates user and returns JWT tokens
- **refresh(refreshToken)**: Refreshes expired access tokens
- **logout()**: Client-side logout placeholder
- Custom `AuthError` class for error handling
- Automatic error transformation from Axios errors

#### Main API Client (`client.ts`)
- Axios instance configured for http://localhost:8080/api
- **Request Interceptor**: Automatically injects Bearer token from localStorage
- **Response Interceptor**:
  - Detects 401 errors
  - Attempts automatic token refresh
  - Retries failed request with new token
  - Redirects to login if refresh fails
- Handles token refresh without interceptor recursion

#### Types (`types.ts`)
- `LoginRequest` / `LoginResponse`: Login flow types
- `RefreshRequest` / `RefreshResponse`: Token refresh types
- `ApiError`: Backend error response structure
- `User`: User profile state
- `AuthState`: Complete authentication state

### Authentication Store (`lib/stores/`)

#### Zustand Store (`auth-store.ts`)
State management with persistence and auto-refresh capabilities:

**State:**
- `user`: User profile (username)
- `token`: JWT access token
- `refreshToken`: JWT refresh token
- `isAuthenticated`: Authentication status
- `tokenExpiresAt`: Token expiration timestamp

**Actions:**
- `login(username, password)`: Authenticate user
- `logout()`: Clear authentication state
- `setTokens(...)`: Manually set tokens (for refresh)
- `refreshAuth()`: Refresh access token
- `checkTokenExpiry()`: Check if token is still valid
- `clearAuth()`: Clear all auth state

**Features:**
- Persists to localStorage using `zustand/middleware`
- Token expiry tracking with 60-second safety buffer
- Auto-refresh check every 30 seconds via `useAutoRefresh` hook
- Selector hooks for common use cases

## Usage Examples

### 1. Login
```typescript
import { useAuthStore } from '@/lib/stores';

const LoginForm = () => {
  const login = useAuthStore((state) => state.login);

  const handleLogin = async (username: string, password: string) => {
    try {
      await login(username, password);
      // Redirect to dashboard
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  // ...
};
```

### 2. Check Authentication Status
```typescript
import { useAuth } from '@/lib/stores';

const Dashboard = () => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    // Redirect to login
    return null;
  }

  return <div>Welcome, {user?.username}!</div>;
};
```

### 3. Logout
```typescript
import { useAuthActions } from '@/lib/stores';

const Header = () => {
  const { logout } = useAuthActions();

  const handleLogout = () => {
    logout();
    // Redirect to login
  };

  // ...
};
```

### 4. Auto-Refresh Setup
```typescript
// In your root layout or app component
import { useAutoRefresh } from '@/lib/stores';

export default function RootLayout({ children }) {
  useAutoRefresh(); // Set up automatic token refresh

  return (
    <html>
      <body>{children}</body>
    </html>
  );
}
```

### 5. Make Authenticated API Calls
```typescript
import { apiClient } from '@/lib/api';

const fetchInvoices = async () => {
  try {
    // Token is automatically injected by interceptor
    const response = await apiClient.get('/invoices');
    return response.data;
  } catch (error) {
    // 401 errors are automatically handled by interceptor
    // If refresh fails, user is redirected to login
    console.error('Failed to fetch invoices:', error);
  }
};
```

## Backend API Contract

The service layer is configured to work with the following backend endpoints:

- **POST /api/auth/login**
  - Request: `{ username: string, password: string }`
  - Response: `{ token: string, refreshToken: string, expiresIn: number }`

- **POST /api/auth/refresh**
  - Request: `{ refreshToken: string }`
  - Response: `{ token: string, refreshToken: string, expiresIn: number }`

## Test Credentials

- Username: `admin`
- Password: `admin123`

## Security Considerations

1. **Token Storage**: Tokens are stored in localStorage (acceptable for JWT)
2. **Token Expiry**: Tokens are refreshed 60 seconds before expiry
3. **Auto-Refresh**: Background check every 30 seconds for token validity
4. **Interceptor Protection**: Prevents infinite loops during refresh
5. **Error Handling**: Failed refresh attempts redirect to login

## Edge Cases Handled

1. **Network Errors**: Caught and transformed to AuthError
2. **Expired Refresh Tokens**: Triggers logout and redirect
3. **401 During Refresh**: Prevents retry loop with `_retry` flag
4. **SSR Compatibility**: localStorage checks for window object
5. **Concurrent Requests**: All benefit from single token refresh

## Next Steps

This is Phase 1 - Service Layer only. Next phases:
- Phase 2: Login/Logout UI components
- Phase 3: Protected routes and auth guards
- Phase 4: Unit and integration tests
