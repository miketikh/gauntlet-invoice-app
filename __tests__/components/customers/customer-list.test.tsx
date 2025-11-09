/**
 * Customer List Component Tests
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { CustomerList } from '@/components/customers/customer-list';
import {
  useCustomerStore,
  useCustomers,
  useCustomerPagination,
  useCustomerActions,
} from '@/lib/stores/customer-store';
import { useRouter } from 'next/navigation';

// Mock dependencies
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

jest.mock('@/lib/stores/customer-store', () => ({
  useCustomerStore: jest.fn(),
  useCustomers: jest.fn(),
  useCustomerPagination: jest.fn(),
  useCustomerActions: jest.fn(),
}));

jest.mock('@/components/customers/delete-customer-dialog', () => ({
  DeleteCustomerDialog: ({ customer, open }: { customer: { name: string } | null; open: boolean }) => (
    <div data-testid="delete-dialog">
      {open && customer && <span>Delete {customer.name}?</span>}
    </div>
  ),
}));

// Mock toast
jest.mock('sonner', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

describe('CustomerList Component', () => {
  const mockPush = jest.fn();
  const mockFetchCustomers = jest.fn();
  const mockClearError = jest.fn();
  const mockSetPage = jest.fn();
  const mockSetSearchTerm = jest.fn();
  const mockSetSortBy = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });

    // Setup default mocks for store hooks
    (useCustomers as jest.Mock).mockReturnValue({
      customers: [],
      loading: false,
      error: null,
    });

    (useCustomerPagination as jest.Mock).mockReturnValue({
      page: 0,
      size: 20,
      totalPages: 1,
      totalElements: 0,
      setPage: mockSetPage,
      setSize: jest.fn(),
    });

    (useCustomerActions as jest.Mock).mockReturnValue({
      fetchCustomers: mockFetchCustomers,
      clearError: mockClearError,
    });

    (useCustomerStore as unknown as jest.Mock).mockImplementation((selector) => {
      const state = {
        searchTerm: '',
        sortBy: 'name',
        sortDirection: 'asc' as const,
        setSearchTerm: mockSetSearchTerm,
        setSortBy: mockSetSortBy,
      };
      return selector ? selector(state) : state;
    });
  });

  it('should render loading skeleton when loading with no customers', () => {
    (useCustomers as jest.Mock).mockReturnValue({
      customers: [],
      loading: true,
      error: null,
    });

    const { container } = render(<CustomerList />);
    // Should render the component (loading skeleton is shown)
    expect(container).toBeTruthy();
    // Should not render the empty state text when loading
    expect(screen.queryByText('No customers yet.')).not.toBeInTheDocument();
  });

  it('should render empty state when no customers', () => {
    render(<CustomerList />);

    expect(screen.getByText('No customers yet.')).toBeInTheDocument();
    expect(
      screen.getByText('Get started by creating your first customer.')
    ).toBeInTheDocument();
  });

  it('should render customer list with data', () => {
    (useCustomers as jest.Mock).mockReturnValue({
      customers: [
        {
          id: '1',
          name: 'John Doe',
          email: 'john@example.com',
          phone: '555-1234',
          createdAt: '2024-01-01T00:00:00Z',
          totalInvoices: 5,
          outstandingBalance: 1000,
        },
        {
          id: '2',
          name: 'Jane Smith',
          email: 'jane@example.com',
          phone: '555-5678',
          createdAt: '2024-01-02T00:00:00Z',
          totalInvoices: 3,
          outstandingBalance: 500,
        },
      ],
      loading: false,
      error: null,
    });

    (useCustomerPagination as jest.Mock).mockReturnValue({
      page: 0,
      size: 20,
      totalPages: 1,
      totalElements: 2,
      setPage: mockSetPage,
      setSize: jest.fn(),
    });

    render(<CustomerList />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@example.com')).toBeInTheDocument();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('should navigate to create page when clicking Create Customer button', () => {
    render(<CustomerList />);

    const createButton = screen.getByText('Create First Customer');
    fireEvent.click(createButton);

    expect(mockPush).toHaveBeenCalledWith('/customers/new');
  });

  it('should call fetchCustomers on mount', () => {
    render(<CustomerList />);
    expect(mockFetchCustomers).toHaveBeenCalled();
  });
});
