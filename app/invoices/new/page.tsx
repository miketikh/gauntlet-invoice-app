/**
 * Create New Invoice Page
 * Protected route for creating invoices
 */

'use client';

import { InvoiceForm } from '@/components/invoices/invoice-form';
import { useRequireAuth } from '@/lib/hooks/useRequireAuth';

export default function NewInvoicePage() {
  // Protect route - redirect to login if not authenticated
  useRequireAuth();

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Create New Invoice</h1>
        <p className="text-muted-foreground">
          Fill in the details below to create a new invoice.
        </p>
      </div>

      <InvoiceForm mode="create" />
    </div>
  );
}
