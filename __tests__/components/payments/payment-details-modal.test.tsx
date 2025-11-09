/**
 * Tests for PaymentDetailsModal Component
 */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PaymentDetailsModal } from '@/components/payments/payment-details-modal';
import * as paymentsApi from '@/lib/api/payments';
import type { PaymentResponseDTO } from '@/lib/api/types';

// Mock the payments API
jest.mock('@/lib/api/payments');

// Mock Next.js router
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    prefetch: jest.fn(),
  }),
  usePathname: () => '/payments',
}));

describe('PaymentDetailsModal', () => {
  const mockPayment: PaymentResponseDTO = {
    id: 'payment-123',
    invoiceId: 'inv-123',
    invoiceNumber: 'INV-2024-0001',
    customerName: 'Acme Corp',
    customerEmail: 'billing@acme.com',
    paymentDate: '2024-11-08',
    amount: 250.00,
    paymentMethod: 'CREDIT_CARD',
    reference: 'VISA-1234',
    notes: 'Partial payment received',
    invoiceTotal: 500.00,
    remainingBalance: 250.00,
    invoiceStatus: 'Sent',
    createdAt: '2024-11-08T14:30:00Z',
    createdBy: 'user@example.com',
  };

  const mockOnOpenChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render payment details when loaded', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockResolvedValue(mockPayment);

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Details')).toBeInTheDocument();
    });

    expect(screen.getByText('$250.00')).toBeInTheDocument();
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('INV-2024-0001')).toBeInTheDocument();
  });

  it('should display payment ID and reference', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockResolvedValue(mockPayment);

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('payment-123')).toBeInTheDocument();
    });

    expect(screen.getByText('VISA-1234')).toBeInTheDocument();
  });

  it('should show payment notes if present', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockResolvedValue(mockPayment);

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Partial payment received')).toBeInTheDocument();
    });
  });

  it('should display invoice context information', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockResolvedValue(mockPayment);

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Invoice Information')).toBeInTheDocument();
    });

    expect(screen.getByText('$500.00')).toBeInTheDocument(); // Invoice total
    expect(screen.getByText('Sent')).toBeInTheDocument(); // Invoice status
  });

  it('should handle errors gracefully', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockRejectedValue(
      new Error('Payment not found')
    );

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Payment not found')).toBeInTheDocument();
    });
  });

  it('should call onOpenChange when Close button is clicked', async () => {
    (paymentsApi.getPaymentById as jest.Mock).mockResolvedValue(mockPayment);

    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={true}
        onOpenChange={mockOnOpenChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Details')).toBeInTheDocument();
    });

    const closeButton = screen.getByRole('button', { name: /close/i });
    await userEvent.click(closeButton);

    expect(mockOnOpenChange).toHaveBeenCalledWith(false);
  });

  it('should not fetch payment when modal is closed', () => {
    render(
      <PaymentDetailsModal
        paymentId="payment-123"
        open={false}
        onOpenChange={mockOnOpenChange}
      />
    );

    expect(paymentsApi.getPaymentById).not.toHaveBeenCalled();
  });
});
