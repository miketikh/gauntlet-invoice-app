/**
 * Customer Store Tests
 */

import { renderHook, act, waitFor } from '@testing-library/react';
import { useCustomerStore } from '@/lib/stores/customer-store';
import { customerApi } from '@/lib/api/customers';
import type { PagedCustomerResponse, CustomerResponseDTO } from '@/lib/api/types';

// Mock the customer API
jest.mock('@/lib/api/customers');

const mockCustomerApi = customerApi as jest.Mocked<typeof customerApi>;

describe('Customer Store', () => {
  beforeEach(() => {
    // Reset store state before each test
    const { result } = renderHook(() => useCustomerStore());
    act(() => {
      result.current.customers = [];
      result.current.selectedCustomer = null;
      result.current.loading = false;
      result.current.error = null;
      result.current.page = 0;
      result.current.size = 20;
      result.current.totalPages = 0;
      result.current.totalElements = 0;
      result.current.searchTerm = '';
      result.current.sortBy = 'name';
      result.current.sortDirection = 'asc';
    });
    jest.clearAllMocks();
  });

  describe('fetchCustomers', () => {
    it('should fetch customers successfully', async () => {
      const mockResponse: PagedCustomerResponse = {
        content: [
          {
            id: '1',
            name: 'John Doe',
            email: 'john@example.com',
            phone: '555-1234',
            createdAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
        numberOfElements: 1,
        first: true,
        last: true,
      };

      mockCustomerApi.getCustomers.mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        await result.current.fetchCustomers();
      });

      expect(result.current.customers).toEqual(mockResponse.content);
      expect(result.current.totalElements).toBe(1);
      expect(result.current.totalPages).toBe(1);
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle errors when fetching customers', async () => {
      const errorMessage = 'Failed to fetch';
      const error = Object.assign(new Error(errorMessage), {
        response: { data: { message: errorMessage } },
      });
      mockCustomerApi.getCustomers.mockRejectedValue(error);

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        await result.current.fetchCustomers();
      });

      expect(result.current.customers).toEqual([]);
      expect(result.current.error).toBe(errorMessage);
      expect(result.current.loading).toBe(false);
    });

    it('should set loading state during fetch', async () => {
      mockCustomerApi.getCustomers.mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCustomerStore());

      act(() => {
        result.current.fetchCustomers();
      });

      expect(result.current.loading).toBe(true);
    });
  });

  describe('fetchCustomerById', () => {
    it('should fetch a single customer successfully', async () => {
      const mockCustomer: CustomerResponseDTO = {
        id: '1',
        name: 'John Doe',
        email: 'john@example.com',
        phone: '555-1234',
        address: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          postalCode: '10001',
          country: 'USA',
        },
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        isDeleted: false,
      };

      mockCustomerApi.getCustomerById.mockResolvedValue(mockCustomer);

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        await result.current.fetchCustomerById('1');
      });

      expect(result.current.selectedCustomer).toEqual(mockCustomer);
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle errors when fetching customer by ID', async () => {
      const error = Object.assign(new Error('Customer not found'), {
        response: { data: { message: 'Customer not found' } },
      });
      mockCustomerApi.getCustomerById.mockRejectedValue(error);

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        await result.current.fetchCustomerById('999');
      });

      expect(result.current.selectedCustomer).toBeNull();
      expect(result.current.error).toBe('Customer not found');
    });
  });

  describe('createCustomer', () => {
    it('should create a customer and add to list', async () => {
      const newCustomer = {
        name: 'Jane Doe',
        email: 'jane@example.com',
      };

      const mockResponse: CustomerResponseDTO = {
        id: '2',
        ...newCustomer,
        phone: '',
        address: {
          street: '',
          city: '',
          state: '',
          postalCode: '',
          country: '',
        },
        createdAt: '2024-01-02T00:00:00Z',
        updatedAt: '2024-01-02T00:00:00Z',
        isDeleted: false,
      };

      mockCustomerApi.createCustomer.mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        const created = await result.current.createCustomer(newCustomer);
        expect(created).toEqual(mockResponse);
      });

      expect(result.current.customers).toHaveLength(1);
      expect(result.current.customers[0].name).toBe('Jane Doe');
      expect(result.current.totalElements).toBe(1);
    });

    it('should handle errors when creating customer', async () => {
      const error = Object.assign(new Error('Email already exists'), {
        response: { data: { message: 'Email already exists' } },
      });
      mockCustomerApi.createCustomer.mockRejectedValue(error);

      const { result } = renderHook(() => useCustomerStore());

      let thrownError = false;
      try {
        await act(async () => {
          await result.current.createCustomer({
            name: 'Test',
            email: 'test@example.com',
          });
        });
      } catch {
        thrownError = true;
      }

      expect(thrownError).toBe(true);
      expect(result.current.error).toBe('Email already exists');
    });
  });

  describe('updateCustomer', () => {
    it('should update a customer in the list', async () => {
      const mockResponse: CustomerResponseDTO = {
        id: '1',
        name: 'John Updated',
        email: 'john.updated@example.com',
        phone: '555-9999',
        address: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          postalCode: '10001',
          country: 'USA',
        },
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-02T00:00:00Z',
        isDeleted: false,
      };

      mockCustomerApi.updateCustomer.mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useCustomerStore());

      // Set initial customer
      act(() => {
        result.current.customers = [
          {
            id: '1',
            name: 'John Doe',
            email: 'john@example.com',
            phone: '555-1234',
            createdAt: '2024-01-01T00:00:00Z',
          },
        ];
      });

      await act(async () => {
        await result.current.updateCustomer('1', {
          name: 'John Updated',
          email: 'john.updated@example.com',
          phone: '555-9999',
        });
      });

      expect(result.current.customers[0].name).toBe('John Updated');
      expect(result.current.customers[0].email).toBe('john.updated@example.com');
      expect(result.current.selectedCustomer).toEqual(mockResponse);
    });
  });

  describe('deleteCustomer', () => {
    it('should remove customer from list', async () => {
      mockCustomerApi.deleteCustomer.mockResolvedValue();

      const { result } = renderHook(() => useCustomerStore());

      // Set initial customers
      act(() => {
        result.current.customers = [
          {
            id: '1',
            name: 'John Doe',
            email: 'john@example.com',
            phone: '555-1234',
            createdAt: '2024-01-01T00:00:00Z',
          },
          {
            id: '2',
            name: 'Jane Doe',
            email: 'jane@example.com',
            phone: '555-5678',
            createdAt: '2024-01-02T00:00:00Z',
          },
        ];
        result.current.totalElements = 2;
      });

      await act(async () => {
        await result.current.deleteCustomer('1');
      });

      expect(result.current.customers).toHaveLength(1);
      expect(result.current.customers[0].id).toBe('2');
      expect(result.current.totalElements).toBe(1);
    });

    it('should handle errors when deleting customer', async () => {
      const error = Object.assign(new Error('Cannot delete customer'), {
        response: { data: { message: 'Cannot delete customer' } },
      });
      mockCustomerApi.deleteCustomer.mockRejectedValue(error);

      const { result } = renderHook(() => useCustomerStore());

      let thrownError = false;
      try {
        await act(async () => {
          await result.current.deleteCustomer('1');
        });
      } catch {
        thrownError = true;
      }

      expect(thrownError).toBe(true);
      expect(result.current.error).toBe('Cannot delete customer');
    });
  });

  describe('Pagination and filters', () => {
    it('should update page and refetch', async () => {
      mockCustomerApi.getCustomers.mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 1,
        size: 20,
        number: 1,
        numberOfElements: 0,
        first: false,
        last: true,
      });

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        result.current.setPage(1);
      });

      await waitFor(() => {
        expect(result.current.page).toBe(1);
        expect(mockCustomerApi.getCustomers).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1 })
        );
      });
    });

    it('should update search term and reset to page 0', async () => {
      mockCustomerApi.getCustomers.mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 1,
        size: 20,
        number: 0,
        numberOfElements: 0,
        first: true,
        last: true,
      });

      const { result } = renderHook(() => useCustomerStore());

      // Set initial page
      act(() => {
        result.current.page = 2;
      });

      await act(async () => {
        result.current.setSearchTerm('test');
      });

      await waitFor(() => {
        expect(result.current.searchTerm).toBe('test');
        expect(result.current.page).toBe(0);
        expect(mockCustomerApi.getCustomers).toHaveBeenCalledWith(
          expect.objectContaining({ search: 'test', page: 0 })
        );
      });
    });

    it('should update sort and refetch', async () => {
      mockCustomerApi.getCustomers.mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 1,
        size: 20,
        number: 0,
        numberOfElements: 0,
        first: true,
        last: true,
      });

      const { result } = renderHook(() => useCustomerStore());

      await act(async () => {
        result.current.setSortBy('email', 'desc');
      });

      await waitFor(() => {
        expect(result.current.sortBy).toBe('email');
        expect(result.current.sortDirection).toBe('desc');
        expect(mockCustomerApi.getCustomers).toHaveBeenCalledWith(
          expect.objectContaining({ sort: 'email', direction: 'desc' })
        );
      });
    });
  });

  describe('Utility actions', () => {
    it('should clear error', () => {
      const { result } = renderHook(() => useCustomerStore());

      act(() => {
        result.current.error = 'Test error';
      });

      expect(result.current.error).toBe('Test error');

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });

    it('should clear selected customer', () => {
      const { result } = renderHook(() => useCustomerStore());

      act(() => {
        result.current.selectedCustomer = {
          id: '1',
          name: 'Test',
          email: 'test@example.com',
          phone: '',
          address: {
            street: '',
            city: '',
            state: '',
            postalCode: '',
            country: '',
          },
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          isDeleted: false,
        };
      });

      expect(result.current.selectedCustomer).toBeTruthy();

      act(() => {
        result.current.clearSelectedCustomer();
      });

      expect(result.current.selectedCustomer).toBeNull();
    });
  });
});
