/**
 * API Module Exports
 */

export { authApi, AuthError, authClient } from './auth';
export { apiClient, createApiClient } from './client';
export * from './invoices';
export * from './customers';
export * from './payments';
export * from './dashboard';
export type {
  LoginRequest,
  LoginResponse,
  RefreshRequest,
  RefreshResponse,
  ApiError,
  User,
  AuthState,
  Customer,
  CreateCustomerDTO,
  UpdateCustomerDTO,
  CustomerResponseDTO,
  CustomerListItemDTO,
  PagedCustomerResponse,
  InvoiceStatus,
  LineItemFormData,
  InvoiceFormData,
  LineItemDTO,
  CreateInvoiceDTO,
  UpdateInvoiceDTO,
  LineItemResponseDTO,
  InvoiceResponseDTO,
  InvoiceListItemDTO,
  InvoiceTotals,
  CalculatedLineItem,
  PaymentMethod,
  RecordPaymentDTO,
  PaymentResponseDTO,
  DashboardStatsDTO,
} from './types';
