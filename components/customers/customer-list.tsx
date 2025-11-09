"use client";

/**
 * Customer List Component
 * Displays customers in a data table with search, sort, and pagination
 */

import { useEffect, useState, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useCustomerStore,
  useCustomers,
  useCustomerPagination,
  useCustomerActions,
} from "@/lib/stores/customer-store";
import { Pencil, Trash2, Plus, ChevronLeft, ChevronRight } from "lucide-react";
import { DeleteCustomerDialog } from "./delete-customer-dialog";
import type { CustomerListItemDTO } from "@/lib/api/types";

export function CustomerList() {
  const router = useRouter();
  const { customers, loading, error } = useCustomers();
  const { page, size, totalPages, totalElements, setPage } = useCustomerPagination();
  const { fetchCustomers, clearError } = useCustomerActions();

  const searchTerm = useCustomerStore((state) => state.searchTerm);
  const sortBy = useCustomerStore((state) => state.sortBy);
  const sortDirection = useCustomerStore((state) => state.sortDirection);
  const setSearchTerm = useCustomerStore((state) => state.setSearchTerm);
  const setSortBy = useCustomerStore((state) => state.setSortBy);

  const [localSearchTerm, setLocalSearchTerm] = useState(searchTerm);
  const [customerToDelete, setCustomerToDelete] = useState<CustomerListItemDTO | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch customers on mount
  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  // Debounced search handler
  const debouncedSearch = useCallback(
    (value: string) => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
      searchTimeoutRef.current = setTimeout(() => {
        setSearchTerm(value);
      }, 300);
    },
    [setSearchTerm]
  );

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalSearchTerm(value);
    debouncedSearch(value);
  };

  const handleSort = (field: string) => {
    // Toggle sort direction if clicking on the same field
    const newDirection =
      sortBy === field && sortDirection === "asc" ? "desc" : "asc";
    setSortBy(field, newDirection);
  };

  const handleEdit = (id: string) => {
    router.push(`/customers/${id}`);
  };

  const handleDelete = (customer: CustomerListItemDTO) => {
    setCustomerToDelete(customer);
    setDeleteDialogOpen(true);
  };

  const handleDeleteSuccess = () => {
    setDeleteDialogOpen(false);
    setCustomerToDelete(null);
  };

  const handleCreateNew = () => {
    router.push("/customers/new");
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatCurrency = (amount?: number) => {
    if (amount === undefined || amount === null) return "-";
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  // Render loading skeleton
  if (loading && customers.length === 0) {
    return (
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-10 w-32" />
        </div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  // Render error state
  if (error && customers.length === 0) {
    return (
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <Input
            placeholder="Search customers..."
            value={localSearchTerm}
            onChange={handleSearchChange}
            className="max-w-sm"
          />
          <Button onClick={handleCreateNew}>
            <Plus className="h-4 w-4 mr-2" />
            Create Customer
          </Button>
        </div>
        <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg p-6">
          <p className="text-red-800 dark:text-red-200">{error}</p>
          <Button
            variant="outline"
            onClick={() => {
              clearError();
              fetchCustomers();
            }}
            className="mt-4"
          >
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  // Render empty state
  if (!loading && customers.length === 0) {
    return (
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <Input
            placeholder="Search customers..."
            value={localSearchTerm}
            onChange={handleSearchChange}
            className="max-w-sm"
          />
          <Button onClick={handleCreateNew}>
            <Plus className="h-4 w-4 mr-2" />
            Create Customer
          </Button>
        </div>
        <div className="bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg p-12 text-center">
          <p className="text-slate-600 dark:text-slate-400 text-lg mb-4">
            {searchTerm ? "No customers found matching your search." : "No customers yet."}
          </p>
          <p className="text-slate-500 dark:text-slate-500 mb-6">
            {searchTerm
              ? "Try adjusting your search term."
              : "Get started by creating your first customer."}
          </p>
          {!searchTerm && (
            <Button onClick={handleCreateNew}>
              <Plus className="h-4 w-4 mr-2" />
              Create First Customer
            </Button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header with search and create button */}
      <div className="flex justify-between items-center">
        <Input
          placeholder="Search customers by name or email..."
          value={localSearchTerm}
          onChange={handleSearchChange}
          className="max-w-sm"
        />
        <Button onClick={handleCreateNew}>
          <Plus className="h-4 w-4 mr-2" />
          Create Customer
        </Button>
      </div>

      {/* Data table */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead
                className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => handleSort("name")}
              >
                Name {sortBy === "name" && (sortDirection === "asc" ? "↑" : "↓")}
              </TableHead>
              <TableHead
                className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => handleSort("email")}
              >
                Email {sortBy === "email" && (sortDirection === "asc" ? "↑" : "↓")}
              </TableHead>
              <TableHead>Phone</TableHead>
              <TableHead
                className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => handleSort("createdAt")}
              >
                Created {sortBy === "createdAt" && (sortDirection === "asc" ? "↑" : "↓")}
              </TableHead>
              <TableHead className="text-right">Invoices</TableHead>
              <TableHead className="text-right">Outstanding</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {customers.map((customer) => (
              <TableRow key={customer.id}>
                <TableCell className="font-medium">{customer.name}</TableCell>
                <TableCell>{customer.email}</TableCell>
                <TableCell>{customer.phone || "-"}</TableCell>
                <TableCell>{formatDate(customer.createdAt)}</TableCell>
                <TableCell className="text-right">{customer.totalInvoices ?? 0}</TableCell>
                <TableCell className="text-right">
                  {formatCurrency(customer.outstandingBalance)}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleEdit(customer.id)}
                      aria-label={`Edit ${customer.name}`}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(customer)}
                      aria-label={`Delete ${customer.name}`}
                    >
                      <Trash2 className="h-4 w-4 text-red-600" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between px-2">
        <div className="text-sm text-slate-600 dark:text-slate-400">
          Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of{" "}
          {totalElements} customers
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page - 1)}
            disabled={page === 0 || loading}
          >
            <ChevronLeft className="h-4 w-4 mr-1" />
            Previous
          </Button>
          <div className="text-sm text-slate-600 dark:text-slate-400">
            Page {page + 1} of {totalPages || 1}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1 || loading}
          >
            Next
            <ChevronRight className="h-4 w-4 ml-1" />
          </Button>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <DeleteCustomerDialog
        customer={customerToDelete}
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onSuccess={handleDeleteSuccess}
      />
    </div>
  );
}
