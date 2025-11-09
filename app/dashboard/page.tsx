"use client";

import { useAuth } from "@/lib/stores/auth-store";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { dashboardApi, type DashboardStatsDTO } from "@/lib/api";

export default function DashboardPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [stats, setStats] = useState<DashboardStatsDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoading(true);
        const data = await dashboardApi.getDashboardStats();
        setStats(data);
        setError(null);
      } catch (err) {
        console.error("Failed to fetch dashboard stats:", err);
        setError("Failed to load dashboard statistics");
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

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

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-md">
          <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
        </div>
      )}

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Total Invoices
          </h3>
          {isLoading ? (
            <div className="animate-pulse">
              <div className="h-9 bg-slate-200 dark:bg-slate-700 rounded w-16 mb-2"></div>
              <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded w-24"></div>
            </div>
          ) : (
            <>
              <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
                {stats?.totalInvoices ?? 0}
              </p>
              <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                {stats?.draftInvoices ?? 0} draft, {stats?.sentInvoices ?? 0}{" "}
                sent, {stats?.paidInvoices ?? 0} paid
              </p>
            </>
          )}
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Customers
          </h3>
          {isLoading ? (
            <div className="animate-pulse">
              <div className="h-9 bg-slate-200 dark:bg-slate-700 rounded w-16 mb-2"></div>
              <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded w-24"></div>
            </div>
          ) : (
            <>
              <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
                {stats?.totalCustomers ?? 0}
              </p>
              <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                {stats?.totalCustomers === 0
                  ? "No customers yet"
                  : "Active customers"}
              </p>
            </>
          )}
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
            Revenue
          </h3>
          {isLoading ? (
            <div className="animate-pulse">
              <div className="h-9 bg-slate-200 dark:bg-slate-700 rounded w-24 mb-2"></div>
              <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded w-28"></div>
            </div>
          ) : (
            <>
              <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
                {formatCurrency(stats?.totalRevenue ?? 0)}
              </p>
              <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                Total paid
              </p>
            </>
          )}
        </div>
      </div>

      {/* Additional Stats */}
      {!isLoading && stats && (stats.outstandingAmount > 0 || stats.overdueAmount > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
              Outstanding Amount
            </h3>
            <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {formatCurrency(stats.outstandingAmount)}
            </p>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              From sent invoices
            </p>
          </div>

          <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50 mb-2">
              Overdue Amount
            </h3>
            <p className="text-2xl font-bold text-red-600 dark:text-red-400">
              {formatCurrency(stats.overdueAmount)}
            </p>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              Past due date
            </p>
          </div>
        </div>
      )}

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
            onClick={() => router.push("/invoices/new")}
            className="px-4 py-2 bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 rounded-md hover:bg-slate-700 dark:hover:bg-slate-200 transition-colors font-medium"
          >
            Create Invoice
          </button>
          <button
            onClick={() => router.push("/customers/new")}
            className="px-4 py-2 border border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-50 rounded-md hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors font-medium"
          >
            Add Customer
          </button>
        </div>
      </div>
    </div>
  );
}
