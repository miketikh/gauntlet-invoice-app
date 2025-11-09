"use client";

export default function CustomersPage() {
  return (
    <div className="space-y-6">
      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50 mb-4">
          Customers
        </h1>
        <p className="text-slate-600 dark:text-slate-400">
          Manage your customers here. This is a protected route.
        </p>
      </div>

      <div className="p-4 bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 rounded-md">
        <p className="text-sm text-blue-800 dark:text-blue-200">
          Customer management functionality coming soon in future phases.
        </p>
      </div>
    </div>
  );
}
