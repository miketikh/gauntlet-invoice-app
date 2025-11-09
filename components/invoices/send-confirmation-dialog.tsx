"use client";

/**
 * Send Confirmation Dialog Component
 * Confirms sending an invoice to a customer
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

interface SendConfirmationDialogProps {
  invoice: InvoiceResponseDTO | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  loading?: boolean;
}

export function SendConfirmationDialog({
  invoice,
  open,
  onOpenChange,
  onConfirm,
  loading = false,
}: SendConfirmationDialogProps) {
  if (!invoice) return null;

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Send Invoice?</DialogTitle>
          <DialogDescription>
            Are you sure you want to send this invoice to {invoice.customerName}?
            Once sent, the invoice can no longer be edited.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <div className="font-semibold text-muted-foreground">
                Invoice Number
              </div>
              <div>{invoice.invoiceNumber}</div>
            </div>
            <div>
              <div className="font-semibold text-muted-foreground">
                Customer
              </div>
              <div>{invoice.customerName}</div>
            </div>
            <div>
              <div className="font-semibold text-muted-foreground">
                Total Amount
              </div>
              <div className="font-semibold">
                {formatCurrency(invoice.totalAmount)}
              </div>
            </div>
            <div>
              <div className="font-semibold text-muted-foreground">
                Due Date
              </div>
              <div>
                {new Date(invoice.dueDate).toLocaleDateString("en-US", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </div>
            </div>
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
          <Button onClick={onConfirm} disabled={loading}>
            {loading ? "Sending..." : "Send Invoice"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
