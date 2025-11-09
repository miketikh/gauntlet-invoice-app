/**
 * Customer Form Component Tests (Simplified)
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CustomerForm } from '@/components/customers/customer-form';
import { useCustomerStore, useCustomerActions } from '@/lib/stores/customer-store';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

// Mock dependencies
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

jest.mock('@/lib/stores/customer-store', () => ({
  useCustomerStore: jest.fn(),
  useCustomerActions: jest.fn(),
}));

jest.mock('sonner', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

describe('CustomerForm Component', () => {
  const mockPush = jest.fn();
  const mockCreateCustomer = jest.fn();
  const mockUpdateCustomer = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });

    (useCustomerActions as jest.Mock).mockReturnValue({
      createCustomer: mockCreateCustomer,
      updateCustomer: mockUpdateCustomer,
    });

    (useCustomerStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        selectedCustomer: null,
        loading: false,
      };
      return selector ? selector(state) : state;
    });
  });

  describe('Create Mode', () => {
    it('should render form in create mode', () => {
      render(<CustomerForm mode="create" />);

      expect(screen.getByLabelText(/Name/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Email/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Phone/)).toBeInTheDocument();
      expect(screen.getByText('Create Customer')).toBeInTheDocument();
    });

    it('should validate required fields', async () => {
      const user = userEvent.setup();
      render(<CustomerForm mode="create" />);

      const submitButton = screen.getByText('Create Customer');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Name is required')).toBeInTheDocument();
        expect(screen.getByText('Email is required')).toBeInTheDocument();
      });
    });

    it('should submit valid form data', async () => {
      const user = userEvent.setup();
      const mockCustomer = {
        id: '1',
        name: 'John Doe',
        email: 'john@example.com',
        phone: '555-1234',
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

      mockCreateCustomer.mockResolvedValue(mockCustomer);

      render(<CustomerForm mode="create" />);

      await user.type(screen.getByLabelText(/Name/), 'John Doe');
      await user.type(screen.getByLabelText(/Email/), 'john@example.com');
      await user.type(screen.getByLabelText(/Phone/), '555-1234');

      const submitButton = screen.getByText('Create Customer');
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockCreateCustomer).toHaveBeenCalled();
        expect(toast.success).toHaveBeenCalledWith('Customer created successfully');
        expect(mockPush).toHaveBeenCalledWith('/customers');
      });
    });

    it('should navigate back on cancel', async () => {
      const user = userEvent.setup();
      render(<CustomerForm mode="create" />);

      const cancelButton = screen.getByText('Cancel');
      await user.click(cancelButton);

      expect(mockPush).toHaveBeenCalledWith('/customers');
    });
  });

  describe('Edit Mode', () => {
    const mockSelectedCustomer = {
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

    beforeEach(() => {
      (useCustomerStore as unknown as jest.Mock).mockImplementation((selector) => {
        const state = {
          selectedCustomer: mockSelectedCustomer,
          loading: false,
        };
        return selector ? selector(state) : state;
      });
    });

    it('should render form in edit mode with pre-populated data', () => {
      render(<CustomerForm mode="edit" customerId="1" />);

      expect(screen.getByText('Update Customer')).toBeInTheDocument();
      expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
      expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
      expect(screen.getByDisplayValue('555-1234')).toBeInTheDocument();
    });
  });
});
