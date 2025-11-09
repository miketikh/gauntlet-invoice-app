"use client";

/**
 * PaymentDetailsModal Component
 * Shows detailed information for a single payment
 */

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Loader2, ExternalLink } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { formatCurrency, formatDate } from '@/lib/utils';
import { getPaymentById } from '@/lib/api/payments';
import { PaymentTypeBadge, getPaymentType } from './payment-type-badge';
import { PaymentMethodIcon, getPaymentMethodLabel } from './payment-method-icon';
import type { PaymentResponseDTO } from '@/lib/api/types';

interface PaymentDetailsModalProps {
  paymentId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function PaymentDetailsModal({
  paymentId,
  open,
  onOpenChange,
}: PaymentDetailsModalProps) {
  const [payment, setPayment] = useState<PaymentResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPayment = async () => {
      if (!paymentId || !open) {
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const data = await getPaymentById(paymentId);
        setPayment(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load payment details');
      } finally {
        setLoading(false);
      }
    };

    fetchPayment();
  }, [paymentId, open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Payment Details</DialogTitle>
        </DialogHeader>

        {loading && (
          <div className="space-y-4">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-20 w-full" />
          </div>
        )}

        {error && (
          <div className="text-center py-8 text-destructive">
            <p>{error}</p>
          </div>
        )}

        {!loading && !error && payment && (
          <div className="space-y-6">
            {/* Payment Amount (emphasized) */}
            <div className="text-center bg-muted/50 rounded-lg p-6">
              <p className="text-sm text-muted-foreground mb-2">Amount Paid</p>
              <p className="text-4xl font-bold text-green-600 dark:text-green-400">
                {formatCurrency(payment.amount)}
              </p>
              <div className="mt-4">
                <PaymentTypeBadge type={getPaymentType(payment.remainingBalance)} />
              </div>
            </div>

            <Separator />

            {/* Payment Information */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground mb-1">Payment ID</p>
                <p className="font-mono text-sm">{payment.id}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground mb-1">Payment Date</p>
                <p className="font-semibold">{formatDate(payment.paymentDate)}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground mb-1">Payment Method</p>
                <div className="flex items-center gap-2">
                  <PaymentMethodIcon method={payment.paymentMethod} />
                  <span className="font-semibold">
                    {getPaymentMethodLabel(payment.paymentMethod)}
                  </span>
                </div>
              </div>
              <div>
                <p className="text-sm text-muted-foreground mb-1">Reference Number</p>
                <p className="font-mono text-sm">{payment.reference}</p>
              </div>
            </div>

            <Separator />

            {/* Invoice Context */}
            <div>
              <h3 className="font-semibold mb-3">Invoice Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Invoice Number</p>
                  <Link
                    href={`/invoices/${payment.invoiceId}`}
                    className="font-semibold text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
                  >
                    {payment.invoiceNumber}
                    <ExternalLink className="h-3 w-3" />
                  </Link>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Invoice Status</p>
                  <p className="font-semibold">{payment.invoiceStatus}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Invoice Total</p>
                  <p className="font-semibold">{formatCurrency(payment.invoiceTotal)}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Remaining Balance</p>
                  <p
                    className={`font-semibold ${
                      payment.remainingBalance > 0
                        ? 'text-orange-600 dark:text-orange-400'
                        : 'text-green-600 dark:text-green-400'
                    }`}
                  >
                    {formatCurrency(payment.remainingBalance)}
                  </p>
                </div>
              </div>
            </div>

            <Separator />

            {/* Customer Information */}
            <div>
              <h3 className="font-semibold mb-3">Customer Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Customer Name</p>
                  <p className="font-semibold">{payment.customerName}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Customer Email</p>
                  <p className="text-sm">{payment.customerEmail}</p>
                </div>
              </div>
            </div>

            {/* Notes (if any) */}
            {payment.notes && (
              <>
                <Separator />
                <div>
                  <h3 className="font-semibold mb-2">Notes</h3>
                  <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                    {payment.notes}
                  </p>
                </div>
              </>
            )}

            <Separator />

            {/* Metadata */}
            <div className="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
              <div>
                <p className="mb-1">Created At</p>
                <p>{formatDate(payment.createdAt)}</p>
              </div>
              <div>
                <p className="mb-1">Created By</p>
                <p>{payment.createdBy}</p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-between gap-2 pt-4">
              <Button
                variant="outline"
                asChild
              >
                <Link href={`/invoices/${payment.invoiceId}`}>
                  View Invoice
                </Link>
              </Button>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
