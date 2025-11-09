"use client";

/**
 * Create Customer Page
 * Form for creating a new customer
 */

import { CustomerForm } from "@/components/customers/customer-form";

export default function NewCustomerPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50">
          Create Customer
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Add a new customer to your database.
        </p>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
        <CustomerForm mode="create" />
      </div>
    </div>
  );
}
