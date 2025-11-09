/**
 * Customer API Client
 * Provides CRUD operations for customer management
 */

import { apiClient } from './client';
import type {
  CreateCustomerDTO,
  UpdateCustomerDTO,
  CustomerResponseDTO,
  PagedCustomerResponse,
} from './types';

export interface CustomerQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
  search?: string;
}

/**
 * Customer API Client
 */
export const customerApi = {
  /**
   * Get paginated list of customers
   * @param params - Query parameters for pagination, sorting, and filtering
   * @returns Paginated customer response
   */
  getCustomers: async (params: CustomerQueryParams = {}): Promise<PagedCustomerResponse> => {
    const queryParams = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? 'name',
      direction: params.direction ?? 'asc',
      ...(params.search && { search: params.search }),
    };

    const response = await apiClient.get<PagedCustomerResponse>('/customers', {
      params: queryParams,
    });

    return response.data;
  },

  /**
   * Get customer by ID
   * @param id - Customer UUID
   * @returns Customer details with computed fields
   */
  getCustomerById: async (id: string): Promise<CustomerResponseDTO> => {
    const response = await apiClient.get<CustomerResponseDTO>(`/customers/${id}`);
    return response.data;
  },

  /**
   * Create a new customer
   * @param data - Customer creation data
   * @returns Created customer details
   */
  createCustomer: async (data: CreateCustomerDTO): Promise<CustomerResponseDTO> => {
    const response = await apiClient.post<CustomerResponseDTO>('/customers', data);
    return response.data;
  },

  /**
   * Update existing customer
   * @param id - Customer UUID
   * @param data - Customer update data
   * @returns Updated customer details
   */
  updateCustomer: async (
    id: string,
    data: UpdateCustomerDTO
  ): Promise<CustomerResponseDTO> => {
    const response = await apiClient.put<CustomerResponseDTO>(`/customers/${id}`, data);
    return response.data;
  },

  /**
   * Delete customer (soft delete)
   * @param id - Customer UUID
   * @returns void (204 No Content)
   */
  deleteCustomer: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/${id}`);
  },
};
