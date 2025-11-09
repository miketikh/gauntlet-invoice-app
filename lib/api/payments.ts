/**
 * Payments API Service
 * Handles payment recording and retrieval operations
 */

import { apiClient } from './client';
import type {
  RecordPaymentDTO,
  PaymentResponseDTO,
  PaymentHistoryFilters,
  Page,
  PaymentStatistics
} from './types';

/**
 * Record a payment against an invoice
 * @param invoiceId - The invoice ID to record payment for
 * @param dto - Payment details
 * @returns Payment response with updated invoice balance
 */
export async function recordPayment(
  invoiceId: string,
  dto: RecordPaymentDTO
): Promise<PaymentResponseDTO> {
  try {
    const response = await apiClient.post<PaymentResponseDTO>(
      `/invoices/${invoiceId}/payments`,
      dto
    );
    return response.data;
  } catch (error: any) {
    // Handle specific error cases
    if (error.response?.status === 400 || error.response?.status === 403) {
      // Extract the backend's actual error message from ProblemDetail
      const detail = error.response.data?.detail ||
                     error.response.data?.message ||
                     'Invalid payment request';

      // Include specific error properties if available
      const currentStatus = error.response.data?.currentStatus;
      const contextualMessage = currentStatus
        ? `${detail} (Invoice status: ${currentStatus})`
        : detail;

      throw new Error(contextualMessage);
    }
    if (error.response?.status === 404) {
      throw new Error('Invoice not found.');
    }
    throw new Error(
      error.response?.data?.message || 'Failed to record payment. Please try again.'
    );
  }
}

/**
 * Get payment by ID
 * @param paymentId - The payment ID
 * @returns Payment details
 */
export async function getPaymentById(paymentId: string): Promise<PaymentResponseDTO> {
  try {
    const response = await apiClient.get<PaymentResponseDTO>(`/payments/${paymentId}`);
    return response.data;
  } catch (error: any) {
    if (error.response?.status === 404) {
      throw new Error('Payment not found.');
    }
    throw new Error('Failed to fetch payment.');
  }
}

/**
 * Get all payments for an invoice
 * @param invoiceId - The invoice ID
 * @returns List of payments
 */
export async function getPaymentsByInvoice(invoiceId: string): Promise<PaymentResponseDTO[]> {
  try {
    const response = await apiClient.get<PaymentResponseDTO[]>(`/invoices/${invoiceId}/payments`);
    return response.data;
  } catch (error: any) {
    if (error.response?.status === 404) {
      throw new Error('Invoice not found.');
    }
    throw new Error('Failed to fetch payments.');
  }
}

/**
 * Get all payments with optional filters
 * @param filters - Payment history filters
 * @returns Paginated payment response
 */
export async function getPayments(filters?: PaymentHistoryFilters): Promise<Page<PaymentResponseDTO>> {
  try {
    // Build query parameters from filters
    const params = new URLSearchParams();

    if (filters?.customerId) {
      params.append('customerId', filters.customerId);
    }
    if (filters?.startDate) {
      params.append('startDate', filters.startDate.toISOString().split('T')[0]);
    }
    if (filters?.endDate) {
      params.append('endDate', filters.endDate.toISOString().split('T')[0]);
    }
    if (filters?.paymentMethod && filters.paymentMethod.length > 0) {
      filters.paymentMethod.forEach(method => params.append('paymentMethod', method));
    }
    if (filters?.reference) {
      params.append('reference', filters.reference);
    }
    if (filters?.page !== undefined) {
      params.append('page', filters.page.toString());
    }
    if (filters?.size !== undefined) {
      params.append('size', filters.size.toString());
    }
    if (filters?.sort) {
      params.append('sort', filters.sort);
    }

    const queryString = params.toString();
    const url = queryString ? `/payments?${queryString}` : '/payments';

    const response = await apiClient.get<Page<PaymentResponseDTO>>(url);
    return response.data;
  } catch (error: any) {
    throw new Error(error.response?.data?.message || 'Failed to fetch payments.');
  }
}

/**
 * Get payment statistics
 * @param startDate - Optional start date for statistics
 * @param endDate - Optional end date for statistics
 * @returns Payment statistics
 */
export async function getPaymentStatistics(
  startDate?: Date,
  endDate?: Date
): Promise<PaymentStatistics> {
  try {
    const params = new URLSearchParams();

    if (startDate) {
      params.append('startDate', startDate.toISOString().split('T')[0]);
    }
    if (endDate) {
      params.append('endDate', endDate.toISOString().split('T')[0]);
    }

    const queryString = params.toString();
    const url = queryString ? `/payments/statistics?${queryString}` : '/payments/statistics';

    const response = await apiClient.get<PaymentStatistics>(url);
    return response.data;
  } catch (error: any) {
    throw new Error(error.response?.data?.message || 'Failed to fetch payment statistics.');
  }
}
