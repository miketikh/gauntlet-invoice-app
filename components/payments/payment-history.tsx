"use client";

/**
 * PaymentHistory Component
 * Displays payment history for a specific invoice
 */

import { useEffect, useState } from 'react';
import { Loader2, Receipt } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { formatCurrency, formatDate } from '@/lib/utils';
import { getPaymentsByInvoice } from '@/lib/api/payments';
import { PaymentTypeBadge, getPaymentType } from './payment-type-badge';
import { PaymentMethodIcon, getPaymentMethodLabel } from './payment-method-icon';
import type { PaymentResponseDTO, InvoiceResponseDTO } from '@/lib/api/types';

interface PaymentHistoryProps {
  invoiceId: string;
  invoice?: InvoiceResponseDTO;
}

export function PaymentHistory({ invoiceId, invoice }: PaymentHistoryProps) {
  const [payments, setPayments] = useState<PaymentResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPayments = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getPaymentsByInvoice(invoiceId);
        setPayments(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load payments');
      } finally {
        setLoading(false);
      }
    };

    fetchPayments();
  }, [invoiceId]);

  // Calculate total paid
  const totalPaid = payments.reduce((sum, payment) => sum + payment.amount, 0);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8 text-destructive">
            <p>{error}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (payments.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <Receipt className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold">No payments recorded yet</h3>
            <p className="text-muted-foreground mt-2">
              Payments will appear here once they are recorded.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Payment History</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Method</TableHead>
                <TableHead>Reference</TableHead>
                <TableHead>Balance</TableHead>
                <TableHead>Type</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {payments.map((payment) => (
                <TableRow key={payment.id}>
                  <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                  <TableCell className="font-semibold">
                    {formatCurrency(payment.amount)}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <PaymentMethodIcon method={payment.paymentMethod} />
                      <span>{getPaymentMethodLabel(payment.paymentMethod)}</span>
                    </div>
                  </TableCell>
                  <TableCell className="font-mono text-sm">
                    {payment.reference}
                  </TableCell>
                  <TableCell className="font-semibold">
                    {formatCurrency(payment.runningBalance ?? payment.remainingBalance)}
                  </TableCell>
                  <TableCell>
                    <PaymentTypeBadge
                      type={getPaymentType(payment.runningBalance ?? payment.remainingBalance)}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {/* Summary */}
        <div className="mt-4 pt-4 border-t">
          <div className="flex justify-between items-center">
            <div>
              <p className="text-sm text-muted-foreground">Total Paid</p>
              <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                {formatCurrency(totalPaid)}
              </p>
            </div>
            {invoice && (
              <div className="text-right">
                <p className="text-sm text-muted-foreground">Remaining Balance</p>
                <p className={`text-2xl font-bold ${
                  invoice.balance > 0
                    ? 'text-orange-600 dark:text-orange-400'
                    : 'text-green-600 dark:text-green-400'
                }`}>
                  {formatCurrency(invoice.balance)}
                </p>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
