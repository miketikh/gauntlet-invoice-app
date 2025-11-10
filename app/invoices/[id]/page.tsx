/**
 * Invoice Detail Page
 * Displays full invoice details with action buttons
 */

'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useRequireAuth } from '@/lib/hooks/useRequireAuth';
import { useInvoiceStore } from '@/lib/stores/invoice-store';
import { InvoiceDetail } from '@/components/invoices/invoice-detail';

export default function InvoiceDetailPage() {
  useRequireAuth();

  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const {
    currentInvoice,
    fetchInvoice,
    sendInvoice,
    deleteInvoice,
    copyInvoice,
    loading,
    error,
  } = useInvoiceStore();

  useEffect(() => {
    if (id) {
      fetchInvoice(id);
    }
  }, [id, fetchInvoice]);

  // Listen for payment-recorded events from the global modal
  useEffect(() => {
    const handlePaymentRecorded = (event: CustomEvent) => {
      if (event.detail.invoiceId === id) {
        // Refetch invoice to show updated balance and status
        fetchInvoice(id);
      }
    };

    window.addEventListener('payment-recorded' as any, handlePaymentRecorded as EventListener);
    return () => {
      window.removeEventListener('payment-recorded' as any, handlePaymentRecorded as EventListener);
    };
  }, [id, fetchInvoice]);

  const handleSend = async () => {
    await sendInvoice(id);
    // Refresh the invoice to show updated status
    await fetchInvoice(id);
  };

  const handleDelete = async () => {
    await deleteInvoice(id);
    // Navigation happens in the component
  };

  const handleCopy = async () => {
    const newInvoice = await copyInvoice(id);
    // Navigate to edit page for the new invoice
    router.push(`/invoices/${newInvoice.id}/edit`);
  };

  const handlePaymentRecorded = async () => {
    // Refetch invoice to show updated balance and status
    await fetchInvoice(id);
  };

  if (loading) {
    return (
      <div className="container mx-auto py-6 max-w-[1380px]">
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
      <div className="container mx-auto py-6 max-w-[1380px]">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center py-8">
              <h2 className="text-xl font-semibold text-destructive">
                Error Loading Invoice
              </h2>
              <p className="text-muted-foreground mt-2">
                {error || 'Invoice not found'}
              </p>
              <Button onClick={() => router.push('/invoices')} className="mt-4">
                Back to Invoices
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-[1380px]">
      <InvoiceDetail
        invoice={currentInvoice}
        onSend={handleSend}
        onDelete={handleDelete}
        onCopy={handleCopy}
        onPaymentRecorded={handlePaymentRecorded}
      />
    </div>
  );
}
