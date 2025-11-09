"use client";

/**
 * Invoice List Component
 * Displays invoices in a table with filtering, pagination, and sorting
 */

import { useEffect, useState, useCallback, useRef } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
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
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { useInvoiceStore } from "@/lib/stores/invoice-store";
import {
  Plus,
  ChevronLeft,
  ChevronRight,
  Search,
  X,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
} from "lucide-react";
import type { InvoiceListItemDTO, InvoiceStatus, BulkActionType, CustomerListItemDTO } from "@/lib/api/types";
import { customerApi } from "@/lib/api";
import { BulkActionsMenu } from "./bulk-actions-menu";
import { toast } from "sonner";

interface StatusBadgeProps {
  status: InvoiceStatus;
}

function StatusBadge({ status }: StatusBadgeProps) {
  const variants: Record<InvoiceStatus, string> = {
    Draft: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-100",
    Sent: "bg-blue-100 text-blue-800 dark:bg-blue-800 dark:text-blue-100",
    Paid: "bg-green-100 text-green-800 dark:bg-green-800 dark:text-green-100",
  };

  return (
    <Badge className={variants[status]} variant="secondary">
      {status}
    </Badge>
  );
}

export function InvoiceList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const {
    invoices,
    loading,
    error,
    filters,
    pagination,
    sorting,
    selectedInvoiceIds,
    fetchInvoices,
    setFilters,
    clearFilters,
    setPagination,
    setSorting,
    toggleSelectInvoice,
    selectAllInvoices,
    deselectAllInvoices,
  } = useInvoiceStore();

  const [customers, setCustomers] = useState<CustomerListItemDTO[]>([]);
  const [localSearch, setLocalSearch] = useState(filters.search || "");
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Load URL params on mount
  useEffect(() => {
    const urlFilters: typeof filters = {};
    const urlStatus = searchParams.get("status");
    const urlCustomerId = searchParams.get("customerId");
    const urlStartDate = searchParams.get("startDate");
    const urlEndDate = searchParams.get("endDate");
    const urlSearch = searchParams.get("search");
    const urlPage = searchParams.get("page");
    const urlSize = searchParams.get("size");
    const urlSortBy = searchParams.get("sortBy");
    const urlSortDirection = searchParams.get("sortDirection");

    if (urlStatus) urlFilters.status = urlStatus as InvoiceStatus;
    if (urlCustomerId) urlFilters.customerId = urlCustomerId;
    if (urlStartDate) urlFilters.startDate = urlStartDate;
    if (urlEndDate) urlFilters.endDate = urlEndDate;
    if (urlSearch) urlFilters.search = urlSearch;

    // Set filters if any exist
    if (Object.keys(urlFilters).length > 0) {
      setFilters(urlFilters);
    }

    // Set pagination
    if (urlPage || urlSize) {
      setPagination({
        page: urlPage ? parseInt(urlPage) : undefined,
        size: urlSize ? parseInt(urlSize) : undefined,
      });
    }

    // Set sorting
    if (urlSortBy || urlSortDirection) {
      setSorting({
        sortBy: urlSortBy || undefined,
        sortDirection: (urlSortDirection as "ASC" | "DESC") || undefined,
      });
    }
  }, []); // Only run on mount

  // Fetch customers for filter dropdown
  useEffect(() => {
    const loadCustomers = async () => {
      try {
        const response = await customerApi.getCustomers({ size: 100 });
        setCustomers(response.content);
      } catch (err) {
        console.error("Failed to load customers", err);
      }
    };
    loadCustomers();
  }, []);

  // Fetch invoices on mount
  useEffect(() => {
    fetchInvoices();
  }, [fetchInvoices]);

  // Update URL when filters/pagination/sorting change
  useEffect(() => {
    const params = new URLSearchParams();

    if (filters.status) params.set("status", filters.status);
    if (filters.customerId) params.set("customerId", filters.customerId);
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);
    if (filters.search) params.set("search", filters.search);
    if (pagination.page > 0) params.set("page", pagination.page.toString());
    if (pagination.size !== 20) params.set("size", pagination.size.toString());
    if (sorting.sortBy !== "issueDate")
      params.set("sortBy", sorting.sortBy);
    if (sorting.sortDirection !== "DESC")
      params.set("sortDirection", sorting.sortDirection);

    const queryString = params.toString();
    const newUrl = queryString ? `${pathname}?${queryString}` : pathname;

    // Use replaceState to avoid adding to browser history
    window.history.replaceState({}, "", newUrl);
  }, [filters, pagination, sorting, pathname]);

  // Debounced search handler
  const debouncedSearch = useCallback(
    (value: string) => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
      searchTimeoutRef.current = setTimeout(() => {
        setFilters({ search: value || undefined });
      }, 300);
    },
    [setFilters]
  );

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalSearch(value);
    debouncedSearch(value);
  };

  const handleSort = (field: string) => {
    const newDirection =
      sorting.sortBy === field && sorting.sortDirection === "ASC"
        ? "DESC"
        : "ASC";
    setSorting({ sortBy: field, sortDirection: newDirection });
  };

  const handleRowClick = (id: string) => {
    router.push(`/invoices/${id}`);
  };

  const handleCreateNew = () => {
    router.push("/invoices/new");
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const activeFilterCount = Object.keys(filters).filter(
    (key) => filters[key as keyof typeof filters]
  ).length;

  const allSelected =
    invoices.length > 0 && selectedInvoiceIds.length === invoices.length;
  const someSelected = selectedInvoiceIds.length > 0 && !allSelected;

  const handleBulkAction = (action: BulkActionType) => {
    // Placeholder for future bulk actions
    toast.info(`Bulk ${action} action coming soon!`);
  };

  const renderSortIcon = (field: string) => {
    if (sorting.sortBy !== field) {
      return <ArrowUpDown className="h-4 w-4 ml-1 opacity-50" />;
    }
    return sorting.sortDirection === "ASC" ? (
      <ArrowUp className="h-4 w-4 ml-1" />
    ) : (
      <ArrowDown className="h-4 w-4 ml-1" />
    );
  };

  if (error) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="text-center py-8">
            <p className="text-destructive">{error}</p>
            <Button onClick={() => fetchInvoices()} className="mt-4">
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Invoices</h1>
          <p className="text-muted-foreground mt-1">
            Manage and track your invoices
          </p>
        </div>
        <Button onClick={handleCreateNew}>
          <Plus className="h-4 w-4 mr-2" />
          Create Invoice
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search invoices..."
                value={localSearch}
                onChange={handleSearchChange}
                className="pl-9"
              />
            </div>

            {/* Status Filter */}
            <Select
              value={filters.status || "all"}
              onValueChange={(value) =>
                setFilters({ status: value === "all" ? undefined : (value as InvoiceStatus) })
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                <SelectItem value="Draft">Draft</SelectItem>
                <SelectItem value="Sent">Sent</SelectItem>
                <SelectItem value="Paid">Paid</SelectItem>
              </SelectContent>
            </Select>

            {/* Customer Filter */}
            <Select
              value={filters.customerId || "all"}
              onValueChange={(value) =>
                setFilters({ customerId: value === "all" ? undefined : value })
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Customers" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Customers</SelectItem>
                {customers.map((customer) => (
                  <SelectItem key={customer.id} value={customer.id}>
                    {customer.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Clear Filters */}
            {activeFilterCount > 0 && (
              <Button variant="outline" onClick={clearFilters}>
                <X className="h-4 w-4 mr-2" />
                Clear Filters ({activeFilterCount})
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Bulk Actions */}
      {selectedInvoiceIds.length > 0 && (
        <BulkActionsMenu
          selectedCount={selectedInvoiceIds.length}
          onAction={handleBulkAction}
          onClearSelection={deselectAllInvoices}
        />
      )}

      {/* Table */}
      <Card>
        <CardContent className="pt-6">
          {loading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : invoices.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground text-lg">
                No invoices found.
              </p>
              <p className="text-muted-foreground mt-2">
                Create your first invoice to get started.
              </p>
              <Button onClick={handleCreateNew} className="mt-4">
                <Plus className="h-4 w-4 mr-2" />
                Create Invoice
              </Button>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-12">
                      <Checkbox
                        checked={allSelected}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            selectAllInvoices();
                          } else {
                            deselectAllInvoices();
                          }
                        }}
                        aria-label="Select all"
                      />
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("invoiceNumber")}
                    >
                      <div className="flex items-center">
                        Invoice #
                        {renderSortIcon("invoiceNumber")}
                      </div>
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("customerName")}
                    >
                      <div className="flex items-center">
                        Customer
                        {renderSortIcon("customerName")}
                      </div>
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("issueDate")}
                    >
                      <div className="flex items-center">
                        Issue Date
                        {renderSortIcon("issueDate")}
                      </div>
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("dueDate")}
                    >
                      <div className="flex items-center">
                        Due Date
                        {renderSortIcon("dueDate")}
                      </div>
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("status")}
                    >
                      <div className="flex items-center">
                        Status
                        {renderSortIcon("status")}
                      </div>
                    </TableHead>
                    <TableHead
                      className="text-right cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("totalAmount")}
                    >
                      <div className="flex items-center justify-end">
                        Total
                        {renderSortIcon("totalAmount")}
                      </div>
                    </TableHead>
                    <TableHead className="text-right">Balance</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invoices.map((invoice) => (
                    <TableRow
                      key={invoice.id}
                      className={`cursor-pointer hover:bg-muted/50 ${
                        invoice.daysOverdue && invoice.daysOverdue > 0
                          ? "border-l-4 border-l-red-500"
                          : ""
                      }`}
                      onClick={() => handleRowClick(invoice.id)}
                    >
                      <TableCell onClick={(e) => e.stopPropagation()}>
                        <Checkbox
                          checked={selectedInvoiceIds.includes(invoice.id)}
                          onCheckedChange={() =>
                            toggleSelectInvoice(invoice.id)
                          }
                          aria-label={`Select invoice ${invoice.invoiceNumber}`}
                        />
                      </TableCell>
                      <TableCell className="font-medium">
                        {invoice.invoiceNumber}
                      </TableCell>
                      <TableCell>{invoice.customerName}</TableCell>
                      <TableCell>{formatDate(invoice.issueDate)}</TableCell>
                      <TableCell>
                        <div className="flex flex-col gap-1">
                          {formatDate(invoice.dueDate)}
                          {invoice.daysOverdue && invoice.daysOverdue > 0 && (
                            <span className="text-xs text-red-600 dark:text-red-400">
                              {invoice.daysOverdue} days overdue
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={invoice.status} />
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatCurrency(invoice.totalAmount)}
                      </TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(invoice.balance)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4 pt-4 border-t">
                <div className="text-sm text-muted-foreground">
                  Showing {pagination.page * pagination.size + 1} to{" "}
                  {Math.min(
                    (pagination.page + 1) * pagination.size,
                    pagination.totalElements
                  )}{" "}
                  of {pagination.totalElements} results
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">
                      Rows per page:
                    </span>
                    <Select
                      value={pagination.size.toString()}
                      onValueChange={(value) =>
                        setPagination({ size: parseInt(value), page: 0 })
                      }
                    >
                      <SelectTrigger className="w-20">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="10">10</SelectItem>
                        <SelectItem value="20">20</SelectItem>
                        <SelectItem value="50">50</SelectItem>
                        <SelectItem value="100">100</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setPagination({ page: pagination.page - 1 })
                      }
                      disabled={pagination.page === 0}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <span className="text-sm">
                      Page {pagination.page + 1} of {pagination.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setPagination({ page: pagination.page + 1 })
                      }
                      disabled={pagination.page >= pagination.totalPages - 1}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
