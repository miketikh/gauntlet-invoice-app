/**
 * Payments API Service
 * Handles payment recording and retrieval operations
 */

import { apiClient } from './client';
import type { RecordPaymentDTO, PaymentResponseDTO } from './types';

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
