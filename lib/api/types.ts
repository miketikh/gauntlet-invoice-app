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

// Invoice domain models
export type InvoiceStatus = 'Draft' | 'Sent' | 'Paid';

export interface LineItemFormData {
  id?: string; // temporary UI id for React keys
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number; // 0-1 decimal (e.g., 0.10 = 10%)
  taxRate: number; // 0-1 decimal (e.g., 0.08 = 8%)
  // Calculated fields (computed, read-only in UI)
  subtotal?: number;
  discountAmount?: number;
  taxableAmount?: number;
  taxAmount?: number;
  total?: number;
}

export interface InvoiceFormData {
  customerId: string;
  issueDate: Date;
  dueDate: Date;
  paymentTerms: string;
  lineItems: LineItemFormData[];
  notes?: string;
}

// Invoice DTOs for API operations
export interface LineItemDTO {
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  taxRate?: number;
}

export interface CreateInvoiceDTO {
  customerId: string;
  issueDate: string; // ISO string for API
  dueDate: string; // ISO string for API
  paymentTerms: string;
  lineItems: LineItemDTO[];
  notes?: string;
}

export interface UpdateInvoiceDTO extends CreateInvoiceDTO {
  version: number; // for optimistic locking
}

// Response DTOs
export interface LineItemResponseDTO {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  taxRate: number;
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  taxAmount: number;
  total: number;
}

export interface InvoiceResponseDTO {
  id: string;
  invoiceNumber: string;
  customerId: string;
  customerName: string;
  customerEmail: string;
  issueDate: string;
  dueDate: string;
  status: InvoiceStatus;
  paymentTerms: string;
  subtotal: number;
  totalDiscount: number;
  totalTax: number;
  totalAmount: number;
  balance: number;
  lineItems: LineItemResponseDTO[];
  notes?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  daysOverdue?: number;
}

export interface InvoiceListItemDTO {
  id: string;
  invoiceNumber: string;
  customerName: string;
  customerEmail: string;
  issueDate: string;
  dueDate: string;
  status: InvoiceStatus;
  totalAmount: number;
  balance: number;
  daysOverdue?: number;
}

export interface InvoiceTotals {
  subtotal: number;
  totalDiscount: number;
  totalTax: number;
  totalAmount: number;
}

export interface CalculatedLineItem extends LineItemFormData {
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  taxAmount: number;
  total: number;
}
