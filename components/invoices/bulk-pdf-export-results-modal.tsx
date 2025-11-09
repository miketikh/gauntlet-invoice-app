"use client";

/**
 * Bulk PDF Export Results Modal
 * Shows summary of bulk PDF export operation with success/failure counts
 */

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { CheckCircle2, XCircle, ChevronDown, ChevronUp } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

export interface FailedInvoice {
  invoiceId: string;
  invoiceNumber: string;
  customerName: string;
  error: string;
}

interface BulkPdfExportResultsModalProps {
  open: boolean;
  totalAttempted: number;
  successCount: number;
  failedInvoices: FailedInvoice[];
  onClose: () => void;
  onRetryFailed: () => void;
}

export function BulkPdfExportResultsModal({
  open,
  totalAttempted,
  successCount,
  failedInvoices,
  onClose,
  onRetryFailed,
}: BulkPdfExportResultsModalProps) {
  const [showFailedList, setShowFailedList] = useState(false);
  const failedCount = failedInvoices.length;
  const allSuccess = failedCount === 0;

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {allSuccess ? (
              <>
                <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                Export Complete
              </>
            ) : (
              <>
                <XCircle className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                Export Partially Complete
              </>
            )}
          </DialogTitle>
          <DialogDescription>
            {allSuccess
              ? `All ${successCount} PDFs downloaded successfully`
              : `${successCount} of ${totalAttempted} PDFs downloaded successfully`}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Summary Stats */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-green-50 dark:bg-green-950 border border-green-200 dark:border-green-800 rounded-lg p-4">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                <div>
                  <p className="text-2xl font-bold text-green-900 dark:text-green-100">
                    {successCount}
                  </p>
                  <p className="text-sm text-green-700 dark:text-green-300">
                    Successful
                  </p>
                </div>
              </div>
            </div>

            {failedCount > 0 && (
              <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg p-4">
                <div className="flex items-center gap-2">
                  <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
                  <div>
                    <p className="text-2xl font-bold text-red-900 dark:text-red-100">
                      {failedCount}
                    </p>
                    <p className="text-sm text-red-700 dark:text-red-300">
                      Failed
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Failed Invoices List */}
          {failedCount > 0 && (
            <div className="border rounded-lg">
              <button
                onClick={() => setShowFailedList(!showFailedList)}
                className={cn(
                  "w-full flex items-center justify-between p-4 hover:bg-muted/50 transition-colors",
                  showFailedList && "border-b"
                )}
              >
                <span className="font-medium text-sm">
                  View Failed Invoices ({failedCount})
                </span>
                {showFailedList ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
              </button>

              {showFailedList && (
                <div className="p-4 space-y-3 max-h-[200px] overflow-y-auto">
                  {failedInvoices.map((invoice, index) => (
                    <div
                      key={invoice.invoiceId}
                      className="text-sm space-y-1 pb-3 border-b last:border-b-0 last:pb-0"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="font-medium">{invoice.invoiceNumber}</p>
                          <p className="text-muted-foreground">
                            {invoice.customerName}
                          </p>
                        </div>
                      </div>
                      <p className="text-xs text-red-600 dark:text-red-400">
                        Error: {invoice.error}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          {failedCount > 0 && (
            <Button
              variant="outline"
              onClick={onRetryFailed}
              className="w-full sm:w-auto"
            >
              Retry Failed ({failedCount})
            </Button>
          )}
          <Button onClick={onClose} className="w-full sm:w-auto">
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
