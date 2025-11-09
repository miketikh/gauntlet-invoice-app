/**
 * Delete Customer Dialog Component Tests
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DeleteCustomerDialog } from '@/components/customers/delete-customer-dialog';
import { useCustomerActions } from '@/lib/stores/customer-store';
import { toast } from 'sonner';
import type { CustomerListItemDTO } from '@/lib/api/types';

// Mock dependencies
jest.mock('@/lib/stores/customer-store', () => ({
  useCustomerActions: jest.fn(),
}));

jest.mock('sonner', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

describe('DeleteCustomerDialog Component', () => {
  const mockDeleteCustomer = jest.fn();
  const mockOnOpenChange = jest.fn();
  const mockOnSuccess = jest.fn();

  const mockCustomer: CustomerListItemDTO = {
    id: '1',
    name: 'John Doe',
    email: 'john@example.com',
    phone: '555-1234',
    createdAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useCustomerActions as jest.Mock).mockReturnValue({
      deleteCustomer: mockDeleteCustomer,
    });
  });

  it('should render when open', () => {
    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.getByText('Delete Customer')).toBeInTheDocument();
    expect(screen.getByText(/John Doe/)).toBeInTheDocument();
    expect(
      screen.getByText(/This action cannot be undone/)
    ).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={false}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.queryByText('Delete Customer')).not.toBeInTheDocument();
  });

  it('should handle successful deletion', async () => {
    mockDeleteCustomer.mockResolvedValue(undefined);

    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockDeleteCustomer).toHaveBeenCalledWith('1');
      expect(toast.success).toHaveBeenCalledWith(
        'Customer "John Doe" has been deleted.'
      );
      expect(mockOnSuccess).toHaveBeenCalled();
    });
  });

  it('should handle deletion errors', async () => {
    const errorMessage = 'Cannot delete customer with active invoices';
    const error = Object.assign(new Error(errorMessage), {
      response: { data: { message: errorMessage } },
    });
    mockDeleteCustomer.mockRejectedValue(error);

    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockDeleteCustomer).toHaveBeenCalledWith('1');
      expect(toast.error).toHaveBeenCalledWith(errorMessage);
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });
  });

  it('should handle generic deletion errors', async () => {
    mockDeleteCustomer.mockRejectedValue(new Error('Network error'));

    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Failed to delete customer');
    });
  });

  it('should show loading state during deletion', async () => {
    mockDeleteCustomer.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    );

    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    // Check for loading text
    await waitFor(() => {
      expect(screen.getByText('Deleting...')).toBeInTheDocument();
    });
  });

  it('should disable buttons during deletion', async () => {
    mockDeleteCustomer.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    );

    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      const cancelButton = screen.getByText('Cancel');
      const deletingButton = screen.getByText('Deleting...');
      expect(cancelButton).toBeDisabled();
      expect(deletingButton).toBeDisabled();
    });
  });

  it('should not delete if customer is null', async () => {
    render(
      <DeleteCustomerDialog
        customer={null}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    // Dialog should still render but without customer name
    expect(screen.getByText('Delete Customer')).toBeInTheDocument();

    const deleteButton = screen.getByText('Delete');
    fireEvent.click(deleteButton);

    // Should not call delete
    await waitFor(() => {
      expect(mockDeleteCustomer).not.toHaveBeenCalled();
    });
  });

  it('should call onOpenChange when cancel is clicked', () => {
    render(
      <DeleteCustomerDialog
        customer={mockCustomer}
        open={true}
        onOpenChange={mockOnOpenChange}
        onSuccess={mockOnSuccess}
      />
    );

    const cancelButton = screen.getByText('Cancel');
    fireEvent.click(cancelButton);

    expect(mockOnOpenChange).toHaveBeenCalledWith(false);
  });
});
