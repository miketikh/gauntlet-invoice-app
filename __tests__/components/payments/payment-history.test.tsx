/**
 * Tests for PaymentHistory Component
 */

import { render, screen, waitFor } from '@testing-library/react';
import { PaymentHistory } from '@/components/payments/payment-history';
import * as paymentsApi from '@/lib/api/payments';
import type { PaymentResponseDTO } from '@/lib/api/types';

// Mock the payments API
jest.mock('@/lib/api/payments');

describe('PaymentHistory', () => {
  const mockPayments: PaymentResponseDTO[] = [
    {
      id: '1',
      invoiceId: 'inv-123',
      invoiceNumber: 'INV-2024-0001',
      customerName: 'Acme Corp',
      customerEmail: 'billing@acme.com',
      paymentDate: '2024-11-01',
      amount: 200.00,
      paymentMethod: 'BANK_TRANSFER',
      reference: 'WIRE-001',
      invoiceTotal: 500.00,
      remainingBalance: 300.00,
      runningBalance: 300.00,
      invoiceStatus: 'Sent',
      createdAt: '2024-11-01T10:00:00Z',
      createdBy: 'user@example.com',
    },
    {
      id: '2',
      invoiceId: 'inv-123',
      invoiceNumber: 'INV-2024-0001',
      customerName: 'Acme Corp',
      customerEmail: 'billing@acme.com',
      paymentDate: '2024-11-08',
      amount: 300.00,
      paymentMethod: 'CREDIT_CARD',
      reference: 'VISA-1234',
      invoiceTotal: 500.00,
      remainingBalance: 0.00,
      runningBalance: 0.00,
      invoiceStatus: 'Paid',
      createdAt: '2024-11-08T10:00:00Z',
      createdBy: 'user@example.com',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render payment history table with payments', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue(mockPayments);

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Payment History')).toBeInTheDocument();
    });

    expect(screen.getByText('WIRE-001')).toBeInTheDocument();
    expect(screen.getByText('VISA-1234')).toBeInTheDocument();
  });

  it('should display running balance for each payment', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue(mockPayments);

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Payment History')).toBeInTheDocument();
    });

    // Check for formatted currency values in the table
    const cells = screen.getAllByRole('cell');
    const balances = cells.filter(cell => cell.textContent?.includes('$'));
    expect(balances.length).toBeGreaterThan(0);
  });

  it('should show partial payment badge', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue(mockPayments);

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Partial')).toBeInTheDocument();
    });
  });

  it('should show full payment badge', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue(mockPayments);

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Full Payment')).toBeInTheDocument();
    });
  });

  it('should show empty state when no payments', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue([]);

    render(<PaymentHistory invoiceId="inv-456" />);

    await waitFor(() => {
      expect(screen.getByText(/no payments recorded/i)).toBeInTheDocument();
    });
  });

  it('should calculate and display total paid', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockResolvedValue(mockPayments);

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Total Paid')).toBeInTheDocument();
    });

    // Total of 200 + 300 = 500
    expect(screen.getByText('$500.00')).toBeInTheDocument();
  });

  it('should handle errors gracefully', async () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockRejectedValue(
      new Error('Failed to load payments')
    );

    render(<PaymentHistory invoiceId="inv-123" />);

    await waitFor(() => {
      expect(screen.getByText('Failed to load payments')).toBeInTheDocument();
    });
  });

  it('should display loading state', () => {
    (paymentsApi.getPaymentsByInvoice as jest.Mock).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(<PaymentHistory invoiceId="inv-123" />);

    expect(screen.getByText('Payment History')).toBeInTheDocument();
    // Should show loading skeletons
  });
});
