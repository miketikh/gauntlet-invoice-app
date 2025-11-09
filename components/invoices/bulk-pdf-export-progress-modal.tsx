"use client";

/**
 * Bulk PDF Export Progress Modal
 * Shows real-time progress during bulk PDF export
 */

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Progress } from "@/components/ui/progress";
import { Loader2 } from "lucide-react";

interface BulkPdfExportProgressModalProps {
  open: boolean;
  current: number;
  total: number;
  currentInvoiceNumber: string;
}

export function BulkPdfExportProgressModal({
  open,
  current,
  total,
  currentInvoiceNumber,
}: BulkPdfExportProgressModalProps) {
  const percentage = total > 0 ? Math.round((current / total) * 100) : 0;

  return (
    <Dialog open={open} modal>
      <DialogContent
        className="sm:max-w-[425px]"
        onPointerDownOutside={(e) => e.preventDefault()}
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Loader2 className="h-5 w-5 animate-spin" />
            Exporting Invoices
          </DialogTitle>
          <DialogDescription>
            Please wait while we prepare your PDFs
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <Progress value={percentage} className="w-full" />

          <div className="text-center space-y-2">
            <p className="text-sm font-medium">
              Exporting {current} of {total} invoices
            </p>
            <p className="text-sm text-muted-foreground">
              {currentInvoiceNumber ? (
                <>Currently downloading: <span className="font-medium">{currentInvoiceNumber}</span></>
              ) : (
                'Starting export...'
              )}
            </p>
            <p className="text-lg font-bold text-primary">
              {percentage}%
            </p>
          </div>
        </div>

        <div
          role="status"
          aria-live="polite"
          aria-atomic="true"
          className="sr-only"
        >
          Exporting {current} of {total} invoices. {percentage}% complete.
        </div>
      </DialogContent>
    </Dialog>
  );
}
