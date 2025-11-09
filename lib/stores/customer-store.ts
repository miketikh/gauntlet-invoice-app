/**
 * Customer Store (Zustand)
 * Manages customer state with pagination, filtering, and CRUD operations
 */

import { create } from 'zustand';
import { customerApi } from '../api/customers';
import type {
  CustomerListItemDTO,
  CustomerResponseDTO,
  CreateCustomerDTO,
  UpdateCustomerDTO,
} from '../api/types';

/**
 * Customer Store Interface
 */
interface CustomerStore {
  // State
  customers: CustomerListItemDTO[];
  selectedCustomer: CustomerResponseDTO | null;
  loading: boolean;
  error: string | null;

  // Pagination state
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;

  // Filter/sort state
  searchTerm: string;
  sortBy: string;
  sortDirection: 'asc' | 'desc';

  // Actions
  fetchCustomers: () => Promise<void>;
  fetchCustomerById: (id: string) => Promise<void>;
  createCustomer: (data: CreateCustomerDTO) => Promise<CustomerResponseDTO>;
  updateCustomer: (id: string, data: UpdateCustomerDTO) => Promise<CustomerResponseDTO>;
  deleteCustomer: (id: string) => Promise<void>;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  setSearchTerm: (term: string) => void;
  setSortBy: (field: string, direction: 'asc' | 'desc') => void;
  clearError: () => void;
  clearSelectedCustomer: () => void;
}

/**
 * Create the customer store
 */
export const useCustomerStore = create<CustomerStore>((set, get) => ({
  // Initial state
  customers: [],
  selectedCustomer: null,
  loading: false,
  error: null,
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0,
  searchTerm: '',
  sortBy: 'name',
  sortDirection: 'asc',

  /**
   * Fetch customers with current pagination/filter/sort settings
   */
  fetchCustomers: async () => {
    set({ loading: true, error: null });
    try {
      const { page, size, sortBy, sortDirection, searchTerm } = get();
      const response = await customerApi.getCustomers({
        page,
        size,
        sort: sortBy,
        direction: sortDirection,
        search: searchTerm || undefined,
      });

      set({
        customers: response.content,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        loading: false,
      });
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to fetch customers')
          : 'Failed to fetch customers';
      set({ error: errorMessage, loading: false, customers: [] });
    }
  },

  /**
   * Fetch single customer by ID
   * @param id - Customer UUID
   */
  fetchCustomerById: async (id: string) => {
    set({ loading: true, error: null });
    try {
      const customer = await customerApi.getCustomerById(id);
      set({ selectedCustomer: customer, loading: false });
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to fetch customer')
          : 'Failed to fetch customer';
      set({ error: errorMessage, loading: false, selectedCustomer: null });
    }
  },

  /**
   * Create new customer
   * @param data - Customer creation data
   * @returns Created customer
   */
  createCustomer: async (data: CreateCustomerDTO): Promise<CustomerResponseDTO> => {
    set({ loading: true, error: null });
    try {
      const customer = await customerApi.createCustomer(data);

      // Optimistically add to customers list
      const customerListItem: CustomerListItemDTO = {
        id: customer.id,
        name: customer.name,
        email: customer.email,
        phone: customer.phone,
        createdAt: customer.createdAt,
        totalInvoices: customer.totalInvoices,
        outstandingBalance: customer.outstandingBalance,
      };

      set((state) => ({
        customers: [customerListItem, ...state.customers],
        totalElements: state.totalElements + 1,
        loading: false,
      }));

      return customer;
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to create customer')
          : 'Failed to create customer';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  /**
   * Update existing customer
   * @param id - Customer UUID
   * @param data - Customer update data
   * @returns Updated customer
   */
  updateCustomer: async (
    id: string,
    data: UpdateCustomerDTO
  ): Promise<CustomerResponseDTO> => {
    set({ loading: true, error: null });
    try {
      const customer = await customerApi.updateCustomer(id, data);

      // Update customer in list
      set((state) => ({
        customers: state.customers.map((c) =>
          c.id === id
            ? {
                id: customer.id,
                name: customer.name,
                email: customer.email,
                phone: customer.phone,
                createdAt: customer.createdAt,
                totalInvoices: customer.totalInvoices,
                outstandingBalance: customer.outstandingBalance,
              }
            : c
        ),
        selectedCustomer: customer,
        loading: false,
      }));

      return customer;
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to update customer')
          : 'Failed to update customer';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  /**
   * Delete customer (soft delete)
   * @param id - Customer UUID
   */
  deleteCustomer: async (id: string): Promise<void> => {
    set({ loading: true, error: null });
    try {
      await customerApi.deleteCustomer(id);

      // Remove customer from list
      set((state) => ({
        customers: state.customers.filter((c) => c.id !== id),
        totalElements: state.totalElements - 1,
        loading: false,
      }));
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to delete customer')
          : 'Failed to delete customer';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  /**
   * Set current page and refetch
   * @param page - Page number (0-indexed)
   */
  setPage: (page: number) => {
    set({ page });
    get().fetchCustomers();
  },

  /**
   * Set page size and refetch
   * @param size - Page size
   */
  setSize: (size: number) => {
    set({ size, page: 0 }); // Reset to first page when changing size
    get().fetchCustomers();
  },

  /**
   * Set search term and refetch from page 0
   * @param term - Search term
   */
  setSearchTerm: (term: string) => {
    set({ searchTerm: term, page: 0 }); // Reset to first page on new search
    get().fetchCustomers();
  },

  /**
   * Set sort field and direction, then refetch
   * @param field - Field to sort by
   * @param direction - Sort direction
   */
  setSortBy: (field: string, direction: 'asc' | 'desc') => {
    set({ sortBy: field, sortDirection: direction });
    get().fetchCustomers();
  },

  /**
   * Clear error state
   */
  clearError: () => {
    set({ error: null });
  },

  /**
   * Clear selected customer
   */
  clearSelectedCustomer: () => {
    set({ selectedCustomer: null });
  },
}));

/**
 * Selector hooks for common use cases
 */
export const useCustomers = () => {
  const customers = useCustomerStore((state) => state.customers);
  const loading = useCustomerStore((state) => state.loading);
  const error = useCustomerStore((state) => state.error);
  return { customers, loading, error };
};

export const useCustomerPagination = () => {
  const page = useCustomerStore((state) => state.page);
  const size = useCustomerStore((state) => state.size);
  const totalPages = useCustomerStore((state) => state.totalPages);
  const totalElements = useCustomerStore((state) => state.totalElements);
  const setPage = useCustomerStore((state) => state.setPage);
  const setSize = useCustomerStore((state) => state.setSize);
  return { page, size, totalPages, totalElements, setPage, setSize };
};

export const useCustomerActions = () => {
  const fetchCustomers = useCustomerStore((state) => state.fetchCustomers);
  const fetchCustomerById = useCustomerStore((state) => state.fetchCustomerById);
  const createCustomer = useCustomerStore((state) => state.createCustomer);
  const updateCustomer = useCustomerStore((state) => state.updateCustomer);
  const deleteCustomer = useCustomerStore((state) => state.deleteCustomer);
  const clearError = useCustomerStore((state) => state.clearError);
  return {
    fetchCustomers,
    fetchCustomerById,
    createCustomer,
    updateCustomer,
    deleteCustomer,
    clearError,
  };
};
