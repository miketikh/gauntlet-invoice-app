"use client";

/**
 * Customers List Page
 * Displays all customers with search, filter, and pagination
 */

import { CustomerList } from "@/components/customers/customer-list";

export default function CustomersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50">
          Customers
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Manage your customers and their information.
        </p>
      </div>

      <CustomerList />
    </div>
  );
}
