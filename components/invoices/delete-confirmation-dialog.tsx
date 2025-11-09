"use client";

/**
 * Delete Confirmation Dialog Component
 * Confirms deletion of a draft invoice
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
import type { InvoiceResponseDTO } from "@/lib/api/types";

interface DeleteConfirmationDialogProps {
  invoice: InvoiceResponseDTO | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  loading?: boolean;
}

export function DeleteConfirmationDialog({
  invoice,
  open,
  onOpenChange,
  onConfirm,
  loading = false,
}: DeleteConfirmationDialogProps) {
  if (!invoice) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete Invoice?</DialogTitle>
          <DialogDescription>
            Are you sure you want to delete invoice {invoice.invoiceNumber}?
            This action cannot be undone.
          </DialogDescription>
        </DialogHeader>

        <div className="py-4">
          <div className="text-sm">
            <div className="font-semibold text-muted-foreground mb-2">
              Customer
            </div>
            <div>{invoice.customerName}</div>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? "Deleting..." : "Delete Invoice"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
