/**
 * Invoice API Service
 * Handles all invoice-related API calls
 */

import { apiClient } from './client';
import type {
  CreateInvoiceDTO,
  UpdateInvoiceDTO,
  InvoiceResponseDTO,
  InvoiceListItemDTO,
  InvoiceFormData,
  LineItemDTO,
} from './types';

const INVOICES_BASE_URL = '/api/invoices';

/**
 * Convert form data to CreateInvoiceDTO for API
 */
function mapFormDataToCreateDTO(formData: InvoiceFormData): CreateInvoiceDTO {
  return {
    customerId: formData.customerId,
    issueDate: formData.issueDate.toISOString(),
    dueDate: formData.dueDate.toISOString(),
    paymentTerms: formData.paymentTerms,
    lineItems: formData.lineItems.map(
      (item): LineItemDTO => ({
        description: item.description,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discountPercent: item.discountPercent,
        taxRate: item.taxRate,
      })
    ),
    notes: formData.notes,
  };
}

/**
 * Convert form data to UpdateInvoiceDTO for API
 */
function mapFormDataToUpdateDTO(
  formData: InvoiceFormData,
  version: number
): UpdateInvoiceDTO {
  return {
    ...mapFormDataToCreateDTO(formData),
    version,
  };
}

/**
 * Create a new invoice
 */
export async function createInvoice(
  formData: InvoiceFormData
): Promise<InvoiceResponseDTO> {
  const dto = mapFormDataToCreateDTO(formData);
  const response = await apiClient.post<InvoiceResponseDTO>(
    INVOICES_BASE_URL,
    dto
  );
  return response.data;
}

/**
 * Update an existing invoice
 */
export async function updateInvoice(
  id: string,
  formData: InvoiceFormData,
  version: number
): Promise<InvoiceResponseDTO> {
  const dto = mapFormDataToUpdateDTO(formData, version);
  const response = await apiClient.put<InvoiceResponseDTO>(
    `${INVOICES_BASE_URL}/${id}`,
    dto
  );
  return response.data;
}

/**
 * Get a single invoice by ID
 */
export async function getInvoiceById(
  id: string
): Promise<InvoiceResponseDTO> {
  const response = await apiClient.get<InvoiceResponseDTO>(
    `${INVOICES_BASE_URL}/${id}`
  );
  return response.data;
}

/**
 * Get list of invoices
 */
export async function getInvoices(): Promise<InvoiceListItemDTO[]> {
  const response = await apiClient.get<InvoiceListItemDTO[]>(
    INVOICES_BASE_URL
  );
  return response.data;
}

/**
 * Send an invoice (change status from Draft to Sent)
 */
export async function sendInvoice(id: string): Promise<InvoiceResponseDTO> {
  const response = await apiClient.post<InvoiceResponseDTO>(
    `${INVOICES_BASE_URL}/${id}/send`
  );
  return response.data;
}
