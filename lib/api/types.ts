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

// Error response from backend - matches ApiErrorResponse from backend
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  correlationId: string;
  fieldErrors?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue: any;
}

/**
 * Custom error class for API errors
 * Wraps API error responses with helper methods
 */
export class ApiError extends Error {
  public readonly response: ApiErrorResponse;
  public readonly correlationId: string;

  constructor(response: ApiErrorResponse) {
    super(response.message);
    this.name = 'ApiError';
    this.response = response;
    this.correlationId = response.correlationId;
  }

  /**
   * Check if this is a validation error with field-level details
   */
  isValidationError(): boolean {
    return this.response.status === 400 && !!this.response.fieldErrors && this.response.fieldErrors.length > 0;
  }

  /**
   * Check if this is a not found error
   */
  isNotFoundError(): boolean {
    return this.response.status === 404;
  }

  /**
   * Check if this is an unauthorized error
   */
  isUnauthorizedError(): boolean {
    return this.response.status === 401;
  }

  /**
   * Check if this is a forbidden error
   */
  isForbiddenError(): boolean {
    return this.response.status === 403;
  }

  /**
   * Check if this is a conflict error
   */
  isConflictError(): boolean {
    return this.response.status === 409;
  }

  /**
   * Check if this is a server error
   */
  isServerError(): boolean {
    return this.response.status >= 500;
  }

  /**
   * Check if this error is retryable
   */
  isRetryable(): boolean {
    // Retry on server errors and network errors
    return this.isServerError();
  }

  /**
   * Get field errors as a map
   */
  getFieldErrorsMap(): Record<string, string> {
    if (!this.response.fieldErrors) return {};
    return this.response.fieldErrors.reduce((acc, error) => {
      acc[error.field] = error.message;
      return acc;
    }, {} as Record<string, string>);
  }

  /**
   * Get a user-friendly error message
   */
  getUserMessage(): string {
    if (this.isValidationError()) {
      return 'Please check your input and try again.';
    }
    return this.message;
  }
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

// Invoice List Filters
export interface InvoiceListFilters {
  customerId?: string;
  status?: InvoiceStatus;
  startDate?: string; // ISO date string
  endDate?: string; // ISO date string
  search?: string; // Search by invoice number or customer name
}

// Invoice Pagination
export interface InvoicePagination {
  page: number; // 0-indexed
  size: number; // Items per page
  totalElements: number; // Total invoices matching filters
  totalPages: number; // Total pages
}

// Invoice Sorting
export interface InvoiceSorting {
  sortBy: string; // Column name: 'invoiceNumber', 'issueDate', 'dueDate', 'totalAmount', 'status'
  sortDirection: 'ASC' | 'DESC';
}

// Paginated Invoice Response
export interface PagedInvoiceResponse {
  content: InvoiceListItemDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Bulk Action Types
export type BulkActionType = 'send' | 'export' | 'delete';

// Payment domain models
export type PaymentMethod = 'CREDIT_CARD' | 'BANK_TRANSFER' | 'CHECK' | 'CASH';

export interface RecordPaymentDTO {
  paymentDate: string; // ISO date string for API
  amount: number;
  paymentMethod: PaymentMethod;
  reference: string;
  notes?: string;
}

export interface PaymentResponseDTO {
  id: string;
  invoiceId: string;
  invoiceNumber: string;
  customerName: string;
  customerEmail: string;
  paymentDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  reference: string;
  notes?: string;
  invoiceTotal: number;
  remainingBalance: number;
  runningBalance?: number; // Only in payment history queries
  invoiceStatus: InvoiceStatus;
  createdAt: string;
  createdBy: string;
}

// Payment History Filters
export interface PaymentHistoryFilters {
  customerId?: string;
  startDate?: Date;
  endDate?: Date;
  paymentMethod?: PaymentMethod[];
  reference?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// Generic Page Response
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Payment Statistics
export interface PaymentStatistics {
  totalCollected: number;
  collectedToday: number;
  collectedThisMonth: number;
  collectedThisYear: number;
  totalPaymentCount: number;
  byMethod: Record<PaymentMethod, number>;
  byMonth: Record<string, number>;
}

// Dashboard Statistics
export interface DashboardStatsDTO {
  totalCustomers: number;
  totalInvoices: number;
  draftInvoices: number;
  sentInvoices: number;
  paidInvoices: number;
  totalRevenue: number;
  outstandingAmount: number;
  overdueAmount: number;
}
