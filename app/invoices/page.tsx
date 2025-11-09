"use client";

/**
 * Invoices List Page
 * Displays all invoices with filtering, pagination, and sorting
 */

import { InvoiceList } from "@/components/invoices/invoice-list";
import { useRequireAuth } from "@/lib/hooks/useRequireAuth";

export default function InvoicesPage() {
  const { isLoading } = useRequireAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 dark:border-slate-100 mx-auto mb-4"></div>
          <p className="text-slate-600 dark:text-slate-400">
            Loading invoices...
          </p>
        </div>
      </div>
    );
  }

  return <InvoiceList />;
}
