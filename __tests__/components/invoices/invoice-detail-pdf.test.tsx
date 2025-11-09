/**
 * Tests for InvoiceDetail PDF Download Functionality
 */

/* eslint-disable @typescript-eslint/no-explicit-any */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InvoiceDetail } from '@/components/invoices/invoice-detail';
import { downloadInvoicePdf } from '@/lib/api/invoices';
import type { InvoiceResponseDTO } from '@/lib/api/types';
import { toast } from 'sonner';

// Mock dependencies
jest.mock('@/lib/api/invoices', () => ({
  downloadInvoicePdf: jest.fn(),
}));

jest.mock('@/lib/api/payments', () => ({
  getPaymentsByInvoice: jest.fn(() => Promise.resolve([])),
}));

jest.mock('@/lib/stores/payment-modal-store', () => ({
  usePaymentModalStore: jest.fn(() => ({
    openPaymentModal: jest.fn(),
  })),
}));

jest.mock('sonner', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
    info: jest.fn(),
  },
}));

jest.mock('next/navigation', () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn(),
  })),
}));

// Mock window.print
global.print = jest.fn();

// Helper to create test invoice
const createTestInvoice = (overrides?: Partial<InvoiceResponseDTO>): InvoiceResponseDTO => ({
  id: 'test-invoice-id',
  invoiceNumber: 'INV-2025-0001',
  customerId: 'test-customer-id',
  customerName: 'Test Customer',
  customerEmail: 'customer@example.com',
  issueDate: '2025-01-01',
  dueDate: '2025-01-31',
  paymentTerms: 'Net 30',
  status: 'Sent',
  subtotal: 1000,
  totalDiscount: 0,
  totalTax: 100,
  totalAmount: 1100,
  balance: 1100,
  lineItems: [
    {
      id: 'line-item-1',
      description: 'Test Service',
      quantity: 10,
      unitPrice: 100,
      discountPercent: 0,
      discountAmount: 0,
      taxRate: 0.1,
      taxAmount: 100,
      total: 1100,
    },
  ],
  notes: 'Test notes',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
  version: 1,
  ...overrides,
});

describe('InvoiceDetail - PDF Download', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render Export PDF button', () => {
    const invoice = createTestInvoice();

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    expect(pdfButton).toBeInTheDocument();
    expect(pdfButton).toHaveTextContent('Export PDF');
  });

  it('should trigger PDF download when button clicked', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    (downloadInvoicePdf as jest.Mock).mockResolvedValue(undefined);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(downloadInvoicePdf).toHaveBeenCalledWith(
        'test-invoice-id',
        'INV-2025-0001'
      );
    });
  });

  it('should show loading state during PDF download', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();

    // Create a promise that we can control
    let resolveDownload: () => void;
    const downloadPromise = new Promise<void>((resolve) => {
      resolveDownload = resolve;
    });

    (downloadInvoicePdf as jest.Mock).mockReturnValue(downloadPromise);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });

    // Click the button
    await user.click(pdfButton);

    // Should show loading state
    await waitFor(() => {
      expect(pdfButton).toHaveTextContent('Generating PDF...');
      expect(pdfButton).toBeDisabled();
    });

    // Resolve the download
    resolveDownload!();

    // Should return to normal state
    await waitFor(() => {
      expect(pdfButton).toHaveTextContent('Export PDF');
      expect(pdfButton).not.toBeDisabled();
    });
  });

  it('should display success toast on successful download', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    (downloadInvoicePdf as jest.Mock).mockResolvedValue(undefined);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('PDF downloaded successfully');
    });
  });

  it('should display error toast on download failure', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    const error = new Error('Network error');
    (downloadInvoicePdf as jest.Mock).mockRejectedValue(error);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Failed to download PDF. Please try again.',
        expect.objectContaining({
          action: expect.objectContaining({
            label: 'Retry',
          }),
        })
      );
    });
  });

  it('should display specific error message for 404 errors', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    const error = {
      response: {
        status: 404,
      },
    };
    (downloadInvoicePdf as jest.Mock).mockRejectedValue(error);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Invoice not found.',
        expect.any(Object)
      );
    });
  });

  it('should display specific error message for 500 errors', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    const error = {
      response: {
        status: 500,
      },
    };
    (downloadInvoicePdf as jest.Mock).mockRejectedValue(error);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'PDF generation failed. Please contact support.',
        expect.any(Object)
      );
    });
  });

  it('should log errors to console on failure', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    const error = new Error('Test error');
    (downloadInvoicePdf as jest.Mock).mockRejectedValue(error);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    await user.click(pdfButton);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('PDF download failed:', error);
    });

    consoleErrorSpy.mockRestore();
  });

  it('should be available for Draft invoices', () => {
    const invoice = createTestInvoice({ status: 'Draft' });

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    expect(pdfButton).toBeInTheDocument();
    expect(pdfButton).not.toBeDisabled();
  });

  it('should be available for Sent invoices', () => {
    const invoice = createTestInvoice({ status: 'Sent' });

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    expect(pdfButton).toBeInTheDocument();
    expect(pdfButton).not.toBeDisabled();
  });

  it('should be available for Paid invoices', () => {
    const invoice = createTestInvoice({ status: 'Paid' });

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    expect(pdfButton).toBeInTheDocument();
    expect(pdfButton).not.toBeDisabled();
  });

  it('should prevent multiple simultaneous downloads', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();

    // Create a promise that we can control
    let resolveDownload: () => void;
    const downloadPromise = new Promise<void>((resolve) => {
      resolveDownload = resolve;
    });

    (downloadInvoicePdf as jest.Mock).mockReturnValue(downloadPromise);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });

    // Click the button twice rapidly
    await user.click(pdfButton);
    await user.click(pdfButton);

    // Should only be called once
    await waitFor(() => {
      expect(downloadInvoicePdf).toHaveBeenCalledTimes(1);
    });

    // Resolve the download
    resolveDownload!();
  });

  it('should have proper ARIA label for accessibility', () => {
    const invoice = createTestInvoice();

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });
    expect(pdfButton).toHaveAttribute('aria-label', 'Download invoice PDF');
  });

  it('should be keyboard accessible', async () => {
    const user = userEvent.setup();
    const invoice = createTestInvoice();
    (downloadInvoicePdf as jest.Mock).mockResolvedValue(undefined);

    render(<InvoiceDetail invoice={invoice} />);

    const pdfButton = screen.getByRole('button', { name: /download invoice pdf/i });

    // Focus the button
    pdfButton.focus();
    expect(pdfButton).toHaveFocus();

    // Press Enter to trigger download
    await user.keyboard('{Enter}');

    await waitFor(() => {
      expect(downloadInvoicePdf).toHaveBeenCalled();
    });
  });
});
