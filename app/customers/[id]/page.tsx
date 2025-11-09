"use client";

/**
 * Edit Customer Page
 * Form for editing an existing customer
 */

import { useEffect } from "react";
import { useParams } from "next/navigation";
import { CustomerForm } from "@/components/customers/customer-form";
import { useCustomerStore, useCustomerActions } from "@/lib/stores/customer-store";
import { Skeleton } from "@/components/ui/skeleton";

export default function EditCustomerPage() {
  const params = useParams();
  const customerId = params.id as string;

  const { fetchCustomerById } = useCustomerActions();
  const selectedCustomer = useCustomerStore((state) => state.selectedCustomer);
  const loading = useCustomerStore((state) => state.loading);
  const error = useCustomerStore((state) => state.error);

  useEffect(() => {
    if (customerId) {
      fetchCustomerById(customerId);
    }
  }, [customerId, fetchCustomerById]);

  if (loading && !selectedCustomer) {
    return (
      <div className="space-y-6">
        <div>
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-6 w-96 mt-2" />
        </div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50">
            Edit Customer
          </h1>
        </div>
        <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg p-6">
          <p className="text-red-800 dark:text-red-200">{error}</p>
        </div>
      </div>
    );
  }

  if (!selectedCustomer) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50">
            Edit Customer
          </h1>
        </div>
        <div className="bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800 rounded-lg p-6">
          <p className="text-yellow-800 dark:text-yellow-200">
            Customer not found.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-50">
          Edit Customer
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Update customer information for {selectedCustomer.name}.
        </p>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-md p-6">
        <CustomerForm mode="edit" customerId={customerId} />
      </div>
    </div>
  );
}
