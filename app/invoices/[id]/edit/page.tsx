/**
 * Edit Invoice Page
 * Protected route for editing draft invoices
 */

'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { InvoiceForm } from '@/components/invoices/invoice-form';
import { useRequireAuth } from '@/lib/hooks/useRequireAuth';
import { useInvoiceStore } from '@/lib/stores/invoice-store';
import { Card, CardContent } from '@/components/ui/card';

export default function EditInvoicePage() {
  // Protect route - redirect to login if not authenticated
  useRequireAuth();

  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { currentInvoice, fetchInvoice, loading, error } = useInvoiceStore();
  const [canEdit, setCanEdit] = useState<boolean | null>(null);

  // Fetch invoice on mount
  useEffect(() => {
    if (id) {
      fetchInvoice(id).then((invoice) => {
        // Check if invoice can be edited (only Draft status)
        if (invoice.status !== 'Draft') {
          setCanEdit(false);
        } else {
          setCanEdit(true);
        }
      });
    }
  }, [id, fetchInvoice]);

  // Loading state
  if (loading || canEdit === null) {
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

  // Error state
  if (error) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center py-8">
              <h2 className="text-xl font-semibold text-destructive">
                Error Loading Invoice
              </h2>
              <p className="text-muted-foreground mt-2">{error}</p>
              <button
                onClick={() => router.push('/invoices')}
                className="mt-4 text-primary hover:underline"
              >
                Back to Invoices
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Cannot edit non-draft invoices
  if (!canEdit || !currentInvoice) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center py-8">
              <h2 className="text-xl font-semibold">Cannot Edit Invoice</h2>
              <p className="text-muted-foreground mt-2">
                {currentInvoice?.status === 'Sent'
                  ? 'This invoice has been sent and cannot be edited.'
                  : currentInvoice?.status === 'Paid'
                  ? 'This invoice has been paid and cannot be edited.'
                  : 'Only draft invoices can be edited.'}
              </p>
              <button
                onClick={() => router.push(`/invoices/${id}`)}
                className="mt-4 text-primary hover:underline"
              >
                View Invoice
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Edit Invoice</h1>
        <p className="text-muted-foreground">
          Invoice #{currentInvoice.invoiceNumber}
        </p>
      </div>

      <InvoiceForm mode="edit" initialData={currentInvoice} />
    </div>
  );
}
