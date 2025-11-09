/**
 * Bulk PDF Export Hook
 * Manages bulk PDF export operations with sequential downloads and progress tracking
 */

import { useState, useCallback } from 'react';
import { downloadInvoicePdf } from '@/lib/api/invoices';
import type { InvoiceListItemDTO } from '@/lib/api/types';
import type { FailedInvoice } from '@/components/invoices/bulk-pdf-export-results-modal';

export interface BulkExportProgress {
  current: number;
  total: number;
  currentInvoiceNumber: string;
}

export interface BulkExportResult {
  total: number;
  successful: string[];
  failed: FailedInvoice[];
}

interface UseBulkPdfExportOptions {
  onProgress?: (progress: BulkExportProgress) => void;
  delayBetweenDownloads?: number; // milliseconds, default 500
}

export function useBulkPdfExport(options: UseBulkPdfExportOptions = {}) {
  const { onProgress, delayBetweenDownloads = 500 } = options;
  const [isExporting, setIsExporting] = useState(false);

  const exportPdfs = useCallback(
    async (invoices: InvoiceListItemDTO[]): Promise<BulkExportResult> => {
      setIsExporting(true);

      const result: BulkExportResult = {
        total: invoices.length,
        successful: [],
        failed: [],
      };

      for (let i = 0; i < invoices.length; i++) {
        const invoice = invoices[i];

        try {
          // Update progress
          onProgress?.({
            current: i + 1,
            total: invoices.length,
            currentInvoiceNumber: invoice.invoiceNumber,
          });

          // Download PDF
          await downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
          result.successful.push(invoice.id);

          // Delay between downloads to prevent browser blocking
          if (i < invoices.length - 1) {
            await new Promise(resolve => setTimeout(resolve, delayBetweenDownloads));
          }
        } catch (error) {
          console.error(`Failed to download PDF for invoice ${invoice.invoiceNumber}:`, error);

          const errorMessage = error instanceof Error
            ? error.message
            : 'Unknown error occurred';

          result.failed.push({
            invoiceId: invoice.id,
            invoiceNumber: invoice.invoiceNumber,
            customerName: invoice.customerName,
            error: errorMessage,
          });

          // Continue with remaining downloads even if one fails
        }
      }

      setIsExporting(false);
      return result;
    },
    [onProgress, delayBetweenDownloads]
  );

  return {
    exportPdfs,
    isExporting,
  };
}
