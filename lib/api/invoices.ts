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
  PagedInvoiceResponse,
  InvoiceListFilters,
  InvoicePagination,
  InvoiceSorting,
} from './types';

const INVOICES_BASE_URL = '/invoices';

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
 * Get list of invoices with filtering, pagination, and sorting
 */
export async function getInvoices(
  filters?: InvoiceListFilters,
  pagination?: Partial<InvoicePagination>,
  sorting?: InvoiceSorting
): Promise<PagedInvoiceResponse> {
  const params = new URLSearchParams();

  // Add filter params
  if (filters?.customerId) {
    params.append('customerId', filters.customerId);
  }
  if (filters?.status) {
    params.append('status', filters.status);
  }
  if (filters?.startDate) {
    params.append('startDate', filters.startDate);
  }
  if (filters?.endDate) {
    params.append('endDate', filters.endDate);
  }
  if (filters?.search) {
    params.append('search', filters.search);
  }

  // Add pagination params
  if (pagination?.page !== undefined) {
    params.append('page', pagination.page.toString());
  }
  if (pagination?.size !== undefined) {
    params.append('size', pagination.size.toString());
  }

  // Add sorting params
  if (sorting?.sortBy) {
    params.append('sortBy', sorting.sortBy);
  }
  if (sorting?.sortDirection) {
    params.append('sortDirection', sorting.sortDirection);
  }

  const queryString = params.toString();
  const url = queryString ? `${INVOICES_BASE_URL}?${queryString}` : INVOICES_BASE_URL;

  const response = await apiClient.get<PagedInvoiceResponse>(url);
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

/**
 * Delete an invoice (only Draft status can be deleted)
 */
export async function deleteInvoice(id: string): Promise<void> {
  await apiClient.delete(`${INVOICES_BASE_URL}/${id}`);
}

/**
 * Copy an invoice to create a new draft
 */
export async function copyInvoice(sourceInvoiceId: string): Promise<InvoiceResponseDTO> {
  // Fetch source invoice
  const sourceInvoice = await getInvoiceById(sourceInvoiceId);

  // Transform to CreateInvoiceDTO
  const today = new Date();
  const paymentTermsDays = parseInt(sourceInvoice.paymentTerms.match(/\d+/)?.[0] || '30');
  const newDueDate = new Date(today);
  newDueDate.setDate(newDueDate.getDate() + paymentTermsDays);

  const createDTO: CreateInvoiceDTO = {
    customerId: sourceInvoice.customerId,
    issueDate: today.toISOString(),
    dueDate: newDueDate.toISOString(),
    paymentTerms: sourceInvoice.paymentTerms,
    lineItems: sourceInvoice.lineItems.map((item): LineItemDTO => ({
      description: item.description,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      discountPercent: item.discountPercent,
      taxRate: item.taxRate,
    })),
    notes: sourceInvoice.notes,
  };

  // Create new invoice
  const response = await apiClient.post<InvoiceResponseDTO>(
    INVOICES_BASE_URL,
    createDTO
  );
  return response.data;
}

/**
 * Download invoice as PDF
 * @param id Invoice ID
 * @param invoiceNumber Invoice number for the filename
 * @returns Promise that resolves when download completes
 */
export async function downloadInvoicePdf(
  id: string,
  invoiceNumber: string
): Promise<void> {
  const response = await apiClient.get(`${INVOICES_BASE_URL}/${id}/pdf`, {
    responseType: 'blob',
    timeout: 30000, // 30 second timeout for PDF generation
  });

  // Create blob from response
  const blob = new Blob([response.data], { type: 'application/pdf' });

  // Create temporary download link
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `Invoice-${invoiceNumber}.pdf`;

  // Trigger download
  document.body.appendChild(link);
  link.click();

  // Cleanup
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
