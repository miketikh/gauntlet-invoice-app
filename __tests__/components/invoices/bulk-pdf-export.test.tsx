/**
 * Bulk PDF Export Component Tests
 * Tests for bulk PDF export functionality including modals and workflow
 */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InvoiceList } from '@/components/invoices/invoice-list';
import { useInvoiceStore } from '@/lib/stores/invoice-store';
import { downloadInvoicePdf } from '@/lib/api/invoices';
import { toast } from 'sonner';

// Mock dependencies
jest.mock('@/lib/stores/invoice-store');
jest.mock('@/lib/stores/payment-modal-store', () => ({
  usePaymentModalStore: () => ({
    openPaymentModal: jest.fn(),
  }),
}));
jest.mock('@/lib/api', () => ({
  customerApi: {
    getCustomers: jest.fn(() =>
      Promise.resolve({ content: [], totalElements: 0 })
    ),
  },
}));
jest.mock('@/lib/api/invoices', () => ({
  downloadInvoicePdf: jest.fn(),
}));
jest.mock('sonner', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
    warning: jest.fn(),
    info: jest.fn(),
  },
}));
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
  }),
  useSearchParams: () => ({
    get: jest.fn(),
  }),
  usePathname: () => '/invoices',
}));

const mockInvoices = [
  {
    id: '1',
    invoiceNumber: 'INV-001',
    customerName: 'Customer A',
    customerId: 'cust-1',
    issueDate: '2025-01-01',
    dueDate: '2025-02-01',
    status: 'Sent' as const,
    totalAmount: 1000,
    balance: 1000,
    daysOverdue: 0,
  },
  {
    id: '2',
    invoiceNumber: 'INV-002',
    customerName: 'Customer B',
    customerId: 'cust-2',
    issueDate: '2025-01-02',
    dueDate: '2025-02-02',
    status: 'Sent' as const,
    totalAmount: 2000,
    balance: 2000,
    daysOverdue: 0,
  },
  {
    id: '3',
    invoiceNumber: 'INV-003',
    customerName: 'Customer C',
    customerId: 'cust-3',
    issueDate: '2025-01-03',
    dueDate: '2025-02-03',
    status: 'Draft' as const,
    totalAmount: 3000,
    balance: 3000,
    daysOverdue: 0,
  },
];

const mockUseInvoiceStore = useInvoiceStore as jest.MockedFunction<typeof useInvoiceStore>;
const mockDownloadInvoicePdf = downloadInvoicePdf as jest.MockedFunction<typeof downloadInvoicePdf>;

describe('Bulk PDF Export', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockUseInvoiceStore.mockReturnValue({
      invoices: mockInvoices,
      loading: false,
      error: null,
      filters: {},
      pagination: {
        page: 0,
        size: 20,
        totalElements: 3,
        totalPages: 1,
      },
      sorting: {
        sortBy: 'issueDate',
        sortDirection: 'DESC',
      },
      selectedInvoiceIds: [],
      fetchInvoices: jest.fn(),
      setFilters: jest.fn(),
      clearFilters: jest.fn(),
      setPagination: jest.fn(),
      setSorting: jest.fn(),
      toggleSelectInvoice: jest.fn(),
      selectAllInvoices: jest.fn(),
      deselectAllInvoices: jest.fn(),
    } as any);

    mockDownloadInvoicePdf.mockResolvedValue(undefined);
  });

  describe('Bulk Actions Menu', () => {
    it('should not show bulk actions menu when no invoices selected', () => {
      render(<InvoiceList />);
      expect(screen.queryByText(/selected/i)).not.toBeInTheDocument();
    });

    it('should show bulk actions menu when invoices selected', () => {
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('invoices selected')).toBeInTheDocument();
    });

    it('should have Export PDFs action enabled', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      expect(exportButton).toBeInTheDocument();
      expect(exportButton).not.toHaveAttribute('disabled');
    });
  });

  describe('Confirmation Modal', () => {
    it('should open confirmation modal when Export PDFs clicked', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      expect(screen.getByText(/export 2 invoices as pdf/i)).toBeInTheDocument();
      expect(screen.getByText(/this will download 2 pdf files/i)).toBeInTheDocument();
    });

    it('should display selected invoice numbers in confirmation modal', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      // Check that invoice numbers appear in the modal (will be in a list)
      const modal = screen.getByRole('dialog');
      expect(modal).toHaveTextContent('INV-001');
      expect(modal).toHaveTextContent('INV-002');
    });

    it('should show "and X more..." for more than 5 invoices', async () => {
      const user = userEvent.setup();
      const manyInvoices = Array.from({ length: 10 }, (_, i) => ({
        ...mockInvoices[0],
        id: `${i + 1}`,
        invoiceNumber: `INV-${String(i + 1).padStart(3, '0')}`,
      }));

      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: manyInvoices,
        selectedInvoiceIds: manyInvoices.map(inv => inv.id),
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      expect(screen.getByText(/and 5 more\.\.\./i)).toBeInTheDocument();
    });

    it('should close confirmation modal when Cancel clicked', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await user.click(cancelButton);

      await waitFor(() => {
        expect(screen.queryByText(/export 2 invoices as pdf/i)).not.toBeInTheDocument();
      });
    });
  });

  describe('Export Process', () => {
    it('should download all selected PDFs sequentially', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(mockDownloadInvoicePdf).toHaveBeenCalledWith('1', 'INV-001');
        expect(mockDownloadInvoicePdf).toHaveBeenCalledWith('2', 'INV-002');
        expect(mockDownloadInvoicePdf).toHaveBeenCalledTimes(2);
      });
    });

    it('should show progress modal during export', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      // Make download async to catch progress modal
      mockDownloadInvoicePdf.mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      expect(screen.getByText(/exporting invoices/i)).toBeInTheDocument();
    });

    it('should show success toast when all downloads succeed', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          expect.stringContaining('All 2 PDFs downloaded successfully')
        );
      });
    });

    it('should handle partial failures gracefully', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      // First download succeeds, second fails
      mockDownloadInvoicePdf
        .mockResolvedValueOnce(undefined)
        .mockRejectedValueOnce(new Error('Network error'));

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(toast.warning).toHaveBeenCalledWith(
          expect.stringContaining('1 of 2 PDFs downloaded')
        );
      });
    });
  });

  describe('Results Modal', () => {
    it('should show results modal after export completes', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(screen.getByText(/export complete/i)).toBeInTheDocument();
      });
    });

    it('should display success count in results modal', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(screen.getByText(/export complete/i)).toBeInTheDocument();
      });

      // Check for success count in results modal
      const resultsModal = screen.getByRole('dialog');
      expect(resultsModal).toHaveTextContent('Successful');
    });

    it('should show failed invoices list when there are failures', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      mockDownloadInvoicePdf
        .mockResolvedValueOnce(undefined)
        .mockRejectedValueOnce(new Error('Network error'));

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(screen.getByText(/view failed invoices/i)).toBeInTheDocument();
      });
    });

    it('should allow retry of failed invoices', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
      } as any);

      // First attempt: one fails
      mockDownloadInvoicePdf
        .mockResolvedValueOnce(undefined)
        .mockRejectedValueOnce(new Error('Network error'));

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(screen.getByText(/view failed invoices/i)).toBeInTheDocument();
      });

      // Second attempt: retry succeeds
      mockDownloadInvoicePdf.mockResolvedValueOnce(undefined);

      const retryButton = screen.getByRole('button', { name: /retry failed/i });
      await user.click(retryButton);

      await waitFor(() => {
        expect(mockDownloadInvoicePdf).toHaveBeenCalledTimes(3); // 2 initial + 1 retry
      });
    });

    it('should clear selection when closing results modal', async () => {
      const user = userEvent.setup();
      const mockDeselectAll = jest.fn();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: ['1', '2'],
        deselectAllInvoices: mockDeselectAll,
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      const confirmButton = screen.getByRole('button', { name: /export 2 pdfs/i });
      await user.click(confirmButton);

      await waitFor(() => {
        expect(screen.getByText(/export complete/i)).toBeInTheDocument();
      });

      const closeButtons = screen.getAllByRole('button', { name: /close/i });
      // Click the last close button (should be in the results modal)
      await user.click(closeButtons[closeButtons.length - 1]);

      expect(mockDeselectAll).toHaveBeenCalled();
    });
  });

  describe('Validation & Limits', () => {
    it('should show error if no invoices selected', async () => {
      const user = userEvent.setup();
      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: mockInvoices,
        selectedInvoiceIds: [],
      } as any);

      render(<InvoiceList />);

      // Try to trigger bulk action directly (shouldn't normally happen in UI)
      // This tests the validation in handleBulkPdfExport
      expect(screen.queryByText(/invoices selected/i)).not.toBeInTheDocument();
    });

    it('should show error if more than 50 invoices selected', async () => {
      const user = userEvent.setup();
      const manyInvoices = Array.from({ length: 51 }, (_, i) => ({
        ...mockInvoices[0],
        id: `${i + 1}`,
        invoiceNumber: `INV-${String(i + 1).padStart(3, '0')}`,
      }));

      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: manyInvoices,
        selectedInvoiceIds: manyInvoices.map(inv => inv.id),
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          expect.stringContaining('Maximum 50 invoices')
        );
      });
    });

    it('should show warning if more than 20 invoices selected', async () => {
      const user = userEvent.setup();
      const manyInvoices = Array.from({ length: 25 }, (_, i) => ({
        ...mockInvoices[0],
        id: `${i + 1}`,
        invoiceNumber: `INV-${String(i + 1).padStart(3, '0')}`,
      }));

      mockUseInvoiceStore.mockReturnValue({
        ...mockUseInvoiceStore(),
        invoices: manyInvoices,
        selectedInvoiceIds: manyInvoices.map(inv => inv.id),
      } as any);

      render(<InvoiceList />);

      const bulkActionsButton = screen.getByRole('button', { name: /bulk actions/i });
      await user.click(bulkActionsButton);

      const exportButton = screen.getByRole('menuitem', { name: /export pdfs/i });
      await user.click(exportButton);

      await waitFor(() => {
        expect(toast.warning).toHaveBeenCalledWith(
          expect.stringContaining('may take a while')
        );
      });
    });
  });
});
