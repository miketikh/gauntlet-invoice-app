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

// Customer domain models
export interface Address {
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  phone: string;
  address: Address;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
}

// Customer DTOs for API operations
export interface CreateCustomerDTO {
  name: string;
  email: string;
  phone?: string;
  address?: Address;
}

export interface UpdateCustomerDTO {
  name: string;
  email: string;
  phone?: string;
  address?: Address;
}

// Response DTO with computed fields
export interface CustomerResponseDTO extends Customer {
  totalInvoices?: number;
  outstandingBalance?: number;
}

// List item DTO (lighter than full response)
export interface CustomerListItemDTO {
  id: string;
  name: string;
  email: string;
  phone: string;
  createdAt: string;
  totalInvoices?: number;
  outstandingBalance?: number;
}

// Paginated response from backend
export interface PagedCustomerResponse {
  content: CustomerListItemDTO[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
}
