"use client";

/**
 * Bulk PDF Export Confirmation Modal
 * Confirms bulk PDF export action before proceeding
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
import { AlertCircle } from "lucide-react";

interface BulkPdfExportConfirmationModalProps {
  open: boolean;
  invoiceCount: number;
  invoiceNumbers: string[];
  onConfirm: () => void;
  onCancel: () => void;
}

export function BulkPdfExportConfirmationModal({
  open,
  invoiceCount,
  invoiceNumbers,
  onConfirm,
  onCancel,
}: BulkPdfExportConfirmationModalProps) {
  const displayNumbers = invoiceNumbers.slice(0, 5);
  const remaining = invoiceCount - displayNumbers.length;

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onCancel()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Export {invoiceCount} Invoice{invoiceCount === 1 ? '' : 's'} as PDF?</DialogTitle>
          <DialogDescription>
            This will download {invoiceCount} PDF file{invoiceCount === 1 ? '' : 's'} to your computer.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Invoice List */}
          <div className="bg-muted p-4 rounded-lg">
            <p className="font-medium text-sm mb-2">Selected Invoices:</p>
            <ul className="text-sm space-y-1">
              {displayNumbers.map((number, index) => (
                <li key={index} className="text-muted-foreground">
                  {number}
                </li>
              ))}
              {remaining > 0 && (
                <li className="text-muted-foreground italic">
                  and {remaining} more...
                </li>
              )}
            </ul>
          </div>

          {/* Warning Message */}
          <div className="flex items-start gap-2 p-3 bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 rounded-lg">
            <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
            <div className="text-sm text-amber-900 dark:text-amber-100">
              <p className="font-medium">Downloads will be sequential</p>
              <p className="text-amber-700 dark:text-amber-300 mt-1">
                Each PDF will download separately to prevent browser blocking. This may take a moment for large batches.
              </p>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onCancel}>
            Cancel
          </Button>
          <Button onClick={onConfirm}>
            Export {invoiceCount} PDF{invoiceCount === 1 ? '' : 's'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
