/**
 * API Types - TypeScript interfaces matching backend DTOs
 */

// Authentication Request/Response types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  expiresIn: number; // seconds until token expires
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
}

// Error response from backend
export interface ApiError {
  message: string;
  status?: number;
  timestamp?: string;
  path?: string;
}

// User state
export interface User {
  username: string;
}

// Auth state
export interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  tokenExpiresAt: number | null; // timestamp when token expires
}
