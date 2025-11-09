# Testing the Authentication Service Layer

This guide explains how to test the authentication service layer implementation.

## Prerequisites

1. Backend must be running on http://localhost:8080
2. Backend must have CORS configured to allow http://localhost:3000
3. Test user credentials: username=admin, password=admin123

## Backend Status Check

Before testing, verify the backend is running:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected response (200 OK):
```json
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600
}
```

If you get 403 Forbidden, the backend needs CORS configuration for the frontend origin.

## Manual Testing in Browser Console

Once the frontend is running (npm run dev), open browser console and test:

### 1. Test Login
```javascript
// Import the store
const { useAuthStore } = await import('/lib/stores/auth-store');

// Get the login function
const login = useAuthStore.getState().login;

// Attempt login
try {
  await login('admin', 'admin123');
  console.log('Login successful!');
  console.log('Auth state:', useAuthStore.getState());
} catch (error) {
  console.error('Login failed:', error.message);
}
```

### 2. Check Authentication State
```javascript
const { useAuthStore } = await import('/lib/stores/auth-store');
const state = useAuthStore.getState();

console.log('Is authenticated:', state.isAuthenticated);
console.log('User:', state.user);
console.log('Token:', state.token?.substring(0, 20) + '...');
console.log('Token expires at:', new Date(state.tokenExpiresAt));
```

### 3. Test Token Refresh
```javascript
const { useAuthStore } = await import('/lib/stores/auth-store');
const refreshAuth = useAuthStore.getState().refreshAuth;

const success = await refreshAuth();
console.log('Refresh successful:', success);
console.log('New token:', useAuthStore.getState().token?.substring(0, 20) + '...');
```

### 4. Test Logout
```javascript
const { useAuthStore } = await import('/lib/stores/auth-store');
const logout = useAuthStore.getState().logout;

logout();
console.log('Is authenticated:', useAuthStore.getState().isAuthenticated);
console.log('User:', useAuthStore.getState().user);
```

### 5. Test API Client with Interceptors
```javascript
const { apiClient } = await import('/lib/api/client');

// First login
const { useAuthStore } = await import('/lib/stores/auth-store');
await useAuthStore.getState().login('admin', 'admin123');

// Make an authenticated request (replace with actual endpoint)
try {
  const response = await apiClient.get('/invoices');
  console.log('Response:', response.data);
} catch (error) {
  console.error('Request failed:', error.message);
}
```

## Automated Testing (Phase 4)

Automated tests will be implemented in Phase 4. This will include:

- Unit tests for auth API client
- Unit tests for auth store actions
- Integration tests for login/logout flow
- Integration tests for token refresh
- E2E tests for complete authentication flow

## Expected Behaviors

### Login Success
- User state is populated with username
- Token and refresh token are stored
- isAuthenticated becomes true
- Tokens are persisted to localStorage
- Token expiry is calculated and stored

### Login Failure
- AuthError is thrown with error message
- Auth state remains cleared
- isAuthenticated stays false
- No data in localStorage

### Token Refresh
- New token and refresh token received
- Token expiry is recalculated
- User remains authenticated
- localStorage is updated

### Auto-Refresh
- Runs every 30 seconds (when useAutoRefresh is active)
- Checks token expiry
- Refreshes if token expires in < 60 seconds
- Logs out if refresh fails

### API Interceptors
- Request interceptor adds Bearer token automatically
- Response interceptor catches 401 errors
- Attempts token refresh on 401
- Retries original request with new token
- Redirects to /login if refresh fails

## Common Issues

### CORS Errors
If you see CORS errors in the console:
- Backend needs to allow http://localhost:3000 origin
- Check backend CORS configuration
- Ensure preflight OPTIONS requests are allowed

### 401 Loop
If you see repeated 401 errors:
- Check that refresh token is still valid
- Verify backend refresh endpoint is working
- Clear localStorage and login again

### Network Errors
If requests fail to reach the backend:
- Verify backend is running on port 8080
- Check that API_BASE_URL is correct
- Ensure no firewall blocking localhost

### TypeScript Errors
If you see TypeScript errors:
- Run: `pnpm tsc --noEmit`
- Verify all dependencies are installed
- Check that types are properly exported
