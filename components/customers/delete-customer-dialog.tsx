"use client";

/**
 * Delete Customer Dialog Component
 * Confirmation dialog for customer deletion
 */

import { useState } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useCustomerActions } from "@/lib/stores/customer-store";
import { toast } from "sonner";
import type { CustomerListItemDTO } from "@/lib/api/types";

interface DeleteCustomerDialogProps {
  customer: CustomerListItemDTO | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function DeleteCustomerDialog({
  customer,
  open,
  onOpenChange,
  onSuccess,
}: DeleteCustomerDialogProps) {
  const { deleteCustomer } = useCustomerActions();
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    if (!customer) return;

    setDeleting(true);
    try {
      await deleteCustomer(customer.id);
      toast.success(`Customer "${customer.name}" has been deleted.`);
      onSuccess();
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message || "Failed to delete customer")
          : "Failed to delete customer";
      toast.error(errorMessage);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete Customer</AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to delete{" "}
            <strong className="text-slate-900 dark:text-slate-50">
              {customer?.name}
            </strong>
            ? This action cannot be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={deleting}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={(e) => {
              e.preventDefault();
              handleDelete();
            }}
            disabled={deleting}
            className="bg-red-600 hover:bg-red-700 focus:ring-red-600"
          >
            {deleting ? "Deleting..." : "Delete"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
