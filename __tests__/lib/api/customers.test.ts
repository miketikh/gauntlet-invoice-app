/**
 * Customer API Client Tests
 */

import { customerApi } from '@/lib/api/customers';
import { apiClient } from '@/lib/api/client';
import type { CreateCustomerDTO, UpdateCustomerDTO } from '@/lib/api/types';

// Mock the API client
jest.mock('@/lib/api/client', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('Customer API Client', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getCustomers', () => {
    it('should fetch customers with default parameters', async () => {
      const mockResponse = {
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          numberOfElements: 0,
          first: true,
          last: true,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await customerApi.getCustomers();

      expect(apiClient.get).toHaveBeenCalledWith('/customers', {
        params: {
          page: 0,
          size: 20,
          sort: 'name',
          direction: 'asc',
        },
      });
      expect(result).toEqual(mockResponse.data);
    });

    it('should fetch customers with custom parameters', async () => {
      const mockResponse = {
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 1,
          numberOfElements: 0,
          first: false,
          last: true,
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValue(mockResponse);

      await customerApi.getCustomers({
        page: 1,
        size: 10,
        sort: 'email',
        direction: 'desc',
        search: 'test',
      });

      expect(apiClient.get).toHaveBeenCalledWith('/customers', {
        params: {
          page: 1,
          size: 10,
          sort: 'email',
          direction: 'desc',
          search: 'test',
        },
      });
    });

    it('should omit search parameter when not provided', async () => {
      const mockResponse = { data: { content: [] } };
      (apiClient.get as jest.Mock).mockResolvedValue(mockResponse);

      await customerApi.getCustomers({ page: 0 });

      expect(apiClient.get).toHaveBeenCalledWith('/customers', {
        params: {
          page: 0,
          size: 20,
          sort: 'name',
          direction: 'asc',
        },
      });
    });
  });

  describe('getCustomerById', () => {
    it('should fetch a single customer by ID', async () => {
      const mockCustomer = {
        id: '123',
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

      const mockResponse = { data: mockCustomer };
      (apiClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await customerApi.getCustomerById('123');

      expect(apiClient.get).toHaveBeenCalledWith('/customers/123');
      expect(result).toEqual(mockCustomer);
    });
  });

  describe('createCustomer', () => {
    it('should create a new customer', async () => {
      const newCustomer: CreateCustomerDTO = {
        name: 'Jane Doe',
        email: 'jane@example.com',
        phone: '555-5678',
        address: {
          street: '456 Oak Ave',
          city: 'Boston',
          state: 'MA',
          postalCode: '02101',
          country: 'USA',
        },
      };

      const mockResponse = {
        data: {
          id: '456',
          ...newCustomer,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          isDeleted: false,
        },
      };

      (apiClient.post as jest.Mock).mockResolvedValue(mockResponse);

      const result = await customerApi.createCustomer(newCustomer);

      expect(apiClient.post).toHaveBeenCalledWith('/customers', newCustomer);
      expect(result).toEqual(mockResponse.data);
    });

    it('should create a customer with minimal fields', async () => {
      const newCustomer: CreateCustomerDTO = {
        name: 'Minimal User',
        email: 'minimal@example.com',
      };

      const mockResponse = {
        data: {
          id: '789',
          ...newCustomer,
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
        },
      };

      (apiClient.post as jest.Mock).mockResolvedValue(mockResponse);

      await customerApi.createCustomer(newCustomer);

      expect(apiClient.post).toHaveBeenCalledWith('/customers', newCustomer);
    });
  });

  describe('updateCustomer', () => {
    it('should update an existing customer', async () => {
      const updateData: UpdateCustomerDTO = {
        name: 'John Updated',
        email: 'john.updated@example.com',
        phone: '555-9999',
      };

      const mockResponse = {
        data: {
          id: '123',
          ...updateData,
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
        },
      };

      (apiClient.put as jest.Mock).mockResolvedValue(mockResponse);

      const result = await customerApi.updateCustomer('123', updateData);

      expect(apiClient.put).toHaveBeenCalledWith('/customers/123', updateData);
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('deleteCustomer', () => {
    it('should delete a customer', async () => {
      (apiClient.delete as jest.Mock).mockResolvedValue({ status: 204 });

      await customerApi.deleteCustomer('123');

      expect(apiClient.delete).toHaveBeenCalledWith('/customers/123');
    });
  });

  describe('Error handling', () => {
    it('should propagate errors from API client', async () => {
      const error = new Error('Network error');
      (apiClient.get as jest.Mock).mockRejectedValue(error);

      await expect(customerApi.getCustomers()).rejects.toThrow('Network error');
    });
  });
});
