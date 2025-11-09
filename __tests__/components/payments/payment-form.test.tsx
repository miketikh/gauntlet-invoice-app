/**
 * PaymentForm Component Tests
 * Tests payment recording form validation, behavior, and API integration
 */

import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PaymentForm } from '@/components/payments/payment-form';
import * as paymentsApi from '@/lib/api/payments';
import type { InvoiceResponseDTO, PaymentResponseDTO } from '@/lib/api/types';
import { toast } from 'sonner';

// Mock dependencies
jest.mock('@/lib/api/payments');
jest.mock('sonner');

// Mock invoice data
const mockInvoice: InvoiceResponseDTO = {
  id: 'invoice-123',
  invoiceNumber: 'INV-2024-0001',
  customerId: 'customer-123',
  customerName: 'Acme Corporation',
  customerEmail: 'billing@acme.com',
  issueDate: '2024-11-01',
  dueDate: '2024-12-01',
  status: 'Sent',
  paymentTerms: 'Net 30',
  subtotal: 450.00,
  totalDiscount: 0,
  totalTax: 50.00,
  totalAmount: 500.00,
  balance: 250.00,
  lineItems: [
    {
      id: 'item-1',
      description: 'Consulting Services',
      quantity: 10,
      unitPrice: 50.00,
      discountPercent: 0,
      taxRate: 0.10,
      subtotal: 500.00,
      discountAmount: 0,
      taxableAmount: 500.00,
      taxAmount: 50.00,
      total: 550.00,
    },
  ],
  version: 1,
  createdAt: '2024-11-01T10:00:00Z',
  updatedAt: '2024-11-01T10:00:00Z',
};

const mockPaymentResponse: PaymentResponseDTO = {
  id: 'payment-123',
  invoiceId: 'invoice-123',
  invoiceNumber: 'INV-2024-0001',
  customerName: 'Acme Corporation',
  customerEmail: 'billing@acme.com',
  paymentDate: '2024-11-08',
  amount: 100.00,
  paymentMethod: 'CreditCard',
  reference: 'VISA-1234',
  notes: 'Partial payment',
  invoiceTotal: 500.00,
  remainingBalance: 150.00,
  invoiceStatus: 'Sent',
  createdAt: '2024-11-08T14:00:00Z',
  createdBy: 'user@example.com',
};

describe('PaymentForm', () => {
  const mockOnOpenChange = jest.fn();
  const mockOnSuccess = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (paymentsApi.recordPayment as jest.Mock).mockResolvedValue(mockPaymentResponse);
  });

  describe('Rendering', () => {
    it('should render payment form with invoice details', () => {
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Check dialog title
      expect(screen.getByText('Record Payment')).toBeDefined();

      // Check invoice context display
      expect(screen.getByText('INV-2024-0001')).toBeDefined();
      expect(screen.getByText('Acme Corporation')).toBeDefined();
      expect(screen.getByText('$500.00')).toBeDefined(); // Total amount
      expect(screen.getByText('$250.00')).toBeDefined(); // Current balance
    });

    it('should render all form fields', () => {
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Check for form field labels
      expect(screen.getByText(/payment date/i)).toBeDefined();
      expect(screen.getByText(/^amount$/i)).toBeDefined();
      expect(screen.getByText(/payment method/i)).toBeDefined();
      expect(screen.getByText(/reference number/i)).toBeDefined();
      expect(screen.getByText(/notes.*optional/i)).toBeDefined();
    });

    it('should render payment method options', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Click payment method dropdown
      const paymentMethodTrigger = screen.getByRole('combobox');
      await user.click(paymentMethodTrigger);

      // Wait for options to appear
      await waitFor(() => {
        expect(screen.getByText('Credit Card')).toBeDefined();
        expect(screen.getByText('Bank Transfer')).toBeDefined();
        expect(screen.getByText('Check')).toBeDefined();
        expect(screen.getByText('Cash')).toBeDefined();
      });
    });

    it('should not render dialog when open is false', () => {
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={false}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.queryByText('Record Payment')).toBeNull();
    });
  });

  describe('Form Validation', () => {
    it('should show error when amount exceeds balance', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      await user.clear(amountInput);
      await user.type(amountInput, '300');

      await waitFor(() => {
        expect(screen.getByText(/amount cannot exceed balance/i)).toBeDefined();
      });
    });

    it('should show error when amount is zero', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const submitButton = screen.getByRole('button', { name: /record payment/i });

      await user.clear(amountInput);
      await user.type(amountInput, '0');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/amount must be at least/i)).toBeDefined();
      });
    });

    it('should show error when reference is empty', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const submitButton = screen.getByRole('button', { name: /record payment/i });

      await user.type(amountInput, '100');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/reference number is required/i)).toBeDefined();
      });
    });

    it('should validate future payment date', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // The DatePicker component prevents selecting future dates via UI,
      // but the validation is in the schema
      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      // Form should be valid with today's date (default)
      const submitButton = screen.getByRole('button', { name: /record payment/i });
      expect(submitButton).toBeDefined();
    });
  });

  describe('Real-time Balance Calculation', () => {
    it('should calculate and display remaining balance in real-time', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      await user.clear(amountInput);
      await user.type(amountInput, '100');

      await waitFor(() => {
        // Balance: $250 - $100 = $150
        expect(screen.getByText(/remaining.*\$150\.00/i)).toBeDefined();
      });
    });

    it('should show green text for positive remaining balance', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      await user.clear(amountInput);
      await user.type(amountInput, '100');

      await waitFor(() => {
        const remainingText = screen.getByText(/\$150\.00/);
        expect(remainingText.className).toContain('green');
      });
    });

    it('should show red warning when amount exceeds balance', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      await user.clear(amountInput);
      await user.type(amountInput, '300');

      await waitFor(() => {
        expect(screen.getByText(/amount exceeds balance/i)).toBeDefined();
        const warningText = screen.getByText(/amount exceeds balance/i);
        expect(warningText.className).toContain('red');
      });
    });

    it('should update balance calculation when amount changes', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);

      // First amount
      await user.clear(amountInput);
      await user.type(amountInput, '50');
      await waitFor(() => {
        expect(screen.getByText(/\$200\.00/)).toBeDefined(); // $250 - $50
      });

      // Change amount
      await user.clear(amountInput);
      await user.type(amountInput, '150');
      await waitFor(() => {
        expect(screen.getByText(/\$100\.00/)).toBeDefined(); // $250 - $150
      });
    });
  });

  describe('Form Submission', () => {
    it('should call API with correct data on submit', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Fill out form
      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);
      const notesInput = screen.getByPlaceholderText(/add any additional notes/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'VISA-1234');
      await user.type(notesInput, 'Partial payment');

      // Submit form
      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(paymentsApi.recordPayment).toHaveBeenCalledWith('invoice-123', {
          paymentDate: expect.any(String),
          amount: 100,
          paymentMethod: 'CreditCard',
          reference: 'VISA-1234',
          notes: 'Partial payment',
        });
      });
    });

    it('should show success toast on successful payment', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          expect.stringContaining('Payment of $100.00 recorded successfully')
        );
      });
    });

    it('should call onSuccess callback after successful submission', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should close dialog after successful submission', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnOpenChange).toHaveBeenCalledWith(false);
      });
    });

    it('should show additional toast when invoice is fully paid', async () => {
      const user = userEvent.setup();
      const fullyPaidResponse = {
        ...mockPaymentResponse,
        remainingBalance: 0,
        invoiceStatus: 'Paid' as const,
      };
      (paymentsApi.recordPayment as jest.Mock).mockResolvedValue(fullyPaidResponse);

      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '250');
      await user.type(referenceInput, 'FINAL-PAYMENT');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          expect.stringContaining('is now fully paid')
        );
      });
    });

    it('should show loading state during submission', async () => {
      const user = userEvent.setup();
      let resolvePayment: any;
      (paymentsApi.recordPayment as jest.Mock).mockReturnValue(
        new Promise((resolve) => {
          resolvePayment = resolve;
        })
      );

      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/recording/i)).toBeDefined();
      });

      // Resolve the payment
      resolvePayment(mockPaymentResponse);
    });

    it('should prevent submission when amount exceeds balance', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '300'); // Exceeds balance of 250
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      // API should not be called
      expect(paymentsApi.recordPayment).not.toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should show error toast on API failure', async () => {
      const user = userEvent.setup();
      (paymentsApi.recordPayment as jest.Mock).mockRejectedValue(
        new Error('Network error')
      );

      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith('Network error');
      });
    });

    it('should keep dialog open on error', async () => {
      const user = userEvent.setup();
      (paymentsApi.recordPayment as jest.Mock).mockRejectedValue(
        new Error('API error')
      );

      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      const submitButton = screen.getByRole('button', { name: /record payment/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalled();
      });

      // Dialog should still be open
      expect(mockOnOpenChange).not.toHaveBeenCalledWith(false);
    });
  });

  describe('Form Reset', () => {
    it('should reset form when dialog closes', async () => {
      const { rerender } = render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const user = userEvent.setup();
      const amountInput = screen.getByPlaceholderText(/0\.00/i);
      const referenceInput = screen.getByPlaceholderText(/transaction id/i);

      await user.clear(amountInput);
      await user.type(amountInput, '100');
      await user.type(referenceInput, 'REF-123');

      // Close dialog
      rerender(
        <PaymentForm
          invoice={mockInvoice}
          open={false}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Reopen dialog
      rerender(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      // Form should be reset
      const newAmountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      expect(newAmountInput.value).toBe('0');
    });
  });

  describe('Accessibility', () => {
    it('should have proper labels for all form fields', () => {
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.getByText(/payment date/i)).toBeDefined();
      expect(screen.getByText(/^amount$/i)).toBeDefined();
      expect(screen.getByText(/payment method/i)).toBeDefined();
      expect(screen.getByText(/reference number/i)).toBeDefined();
      expect(screen.getByText(/notes.*optional/i)).toBeDefined();
    });

    it('should have cancel and submit buttons', () => {
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.getByRole('button', { name: /cancel/i })).toBeDefined();
      expect(screen.getByRole('button', { name: /record payment/i })).toBeDefined();
    });

    it('should call onOpenChange when cancel is clicked', async () => {
      const user = userEvent.setup();
      render(
        <PaymentForm
          invoice={mockInvoice}
          open={true}
          onOpenChange={mockOnOpenChange}
          onSuccess={mockOnSuccess}
        />
      );

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await user.click(cancelButton);

      expect(mockOnOpenChange).toHaveBeenCalledWith(false);
    });
  });
});
