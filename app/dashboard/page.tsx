"use client";

import { useAuth } from "@/lib/stores/auth-store";

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      {/* Welcome Section */}
      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50 mb-4">
          Dashboard
        </h1>
        <p className="text-slate-600 dark:text-slate-400">
          Welcome back, <span className="font-semibold">{user?.username}</span>!
        </p>
      </div>

      {/* Success Message */}
      <div className="p-4 bg-green-50 dark:bg-green-950 border border-green-200 dark:border-green-800 rounded-md">
        <p className="text-sm text-green-800 dark:text-green-200">
          You have successfully logged in to InvoiceMe. This is a protected route.
        </p>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Total Invoices
          </h3>
          <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
            0
          </p>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            No invoices yet
          </p>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Customers
          </h3>
          <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
            0
          </p>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            No customers yet
          </p>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Revenue
          </h3>
          <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
            $0.00
          </p>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            This month
          </p>
        </div>
      </div>

      {/* Getting Started */}
      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-8">
        <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-50 mb-4">
          Getting Started
        </h2>
        <p className="text-slate-600 dark:text-slate-400 mb-4">
          Start managing your invoices by creating your first invoice or adding customers.
        </p>
        <div className="flex flex-col sm:flex-row gap-3">
          <button
            onClick={() => window.alert("Invoice creation coming soon!")}
            className="px-4 py-2 bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 rounded-md hover:bg-slate-700 dark:hover:bg-slate-200 transition-colors font-medium"
          >
            Create Invoice
          </button>
          <button
            onClick={() => window.alert("Customer management coming soon!")}
            className="px-4 py-2 border border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-50 rounded-md hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors font-medium"
          >
            Add Customer
          </button>
        </div>
      </div>
    </div>
  );
}
