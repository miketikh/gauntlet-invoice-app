/**
 * Invoice Detail Page
 * Displays invoice details (placeholder for Story 2.5)
 */

'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Loader2, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useRequireAuth } from '@/lib/hooks/useRequireAuth';
import { useInvoiceStore } from '@/lib/stores/invoice-store';
import { formatCurrency } from '@/lib/utils/invoice-calculations';

export default function InvoiceDetailPage() {
  useRequireAuth();

  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { currentInvoice, fetchInvoice, loading, error } = useInvoiceStore();

  useEffect(() => {
    if (id) {
      fetchInvoice(id);
    }
  }, [id, fetchInvoice]);

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-center gap-2 py-8">
              <Loader2 className="h-6 w-6 animate-spin" />
              <span>Loading invoice...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !currentInvoice) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center py-8">
              <h2 className="text-xl font-semibold text-destructive">
                Error Loading Invoice
              </h2>
              <p className="text-muted-foreground mt-2">{error || 'Invoice not found'}</p>
              <Button
                onClick={() => router.push('/invoices')}
                className="mt-4"
              >
                Back to Invoices
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="outline"
          size="icon"
          onClick={() => router.push('/invoices')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">Invoice #{currentInvoice.invoiceNumber}</h1>
          <p className="text-muted-foreground">{currentInvoice.customerName}</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Invoice Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <div className="text-sm text-muted-foreground">Status</div>
              <div className="font-semibold">{currentInvoice.status}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">Total Amount</div>
              <div className="font-semibold">{formatCurrency(currentInvoice.totalAmount)}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">Issue Date</div>
              <div>{new Date(currentInvoice.issueDate).toLocaleDateString()}</div>
            </div>
            <div>
              <div className="text-sm text-muted-foreground">Due Date</div>
              <div>{new Date(currentInvoice.dueDate).toLocaleDateString()}</div>
            </div>
          </div>

          <div className="flex gap-2">
            {currentInvoice.status === 'Draft' && (
              <Button onClick={() => router.push(`/invoices/${id}/edit`)}>
                Edit Invoice
              </Button>
            )}
            <Button variant="outline" onClick={() => router.push('/invoices')}>
              Back to List
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
