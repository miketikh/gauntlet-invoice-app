"use client";

/**
 * Invoice Selector Component
 * Modal for selecting an invoice to record a payment
 * Filters to show only Sent invoices with outstanding balance
 */

import { useState, useEffect, useCallback, useRef } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Search, X } from "lucide-react";
import { getInvoices } from "@/lib/api";
import type { InvoiceListItemDTO } from "@/lib/api/types";

interface InvoiceSelectorProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelectInvoice: (invoiceId: string) => void;
}

export function InvoiceSelector({
  open,
  onOpenChange,
  onSelectInvoice,
}: InvoiceSelectorProps) {
  const [invoices, setInvoices] = useState<InvoiceListItemDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [localSearch, setLocalSearch] = useState("");
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch invoices with filters
  const fetchInvoices = useCallback(async (search?: string) => {
    try {
      setLoading(true);
      const response = await getInvoices(
        { status: "Sent", search: search || undefined },
        { page: 0, size: 50 },
        { sortBy: "dueDate", sortDirection: "ASC" }
      );
      // Filter to only show invoices with balance > 0
      const unpaidInvoices = response.content.filter(
        (invoice: InvoiceListItemDTO) => invoice.balance > 0
      );
      setInvoices(unpaidInvoices);
    } catch (err) {
      console.error("Failed to fetch invoices:", err);
      setInvoices([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch invoices when dialog opens
  useEffect(() => {
    if (open) {
      fetchInvoices(searchTerm);
    }
  }, [open, searchTerm, fetchInvoices]);

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
    []
  );

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalSearch(value);
    debouncedSearch(value);
  };

  const handleClearSearch = () => {
    setLocalSearch("");
    setSearchTerm("");
  };

  const handleSelectInvoice = (invoiceId: string) => {
    onOpenChange(false);
    onSelectInvoice(invoiceId);
    // Reset search when closing
    setLocalSearch("");
    setSearchTerm("");
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>Select an Invoice</DialogTitle>
          <DialogDescription>
            Choose an invoice to record a payment. Only showing sent invoices
            with outstanding balance.
          </DialogDescription>
        </DialogHeader>

        {/* Search */}
        <div className="flex gap-2 py-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by invoice # or customer name..."
              value={localSearch}
              onChange={handleSearchChange}
              className="pl-9"
            />
          </div>
          {localSearch && (
            <Button
              variant="outline"
              size="icon"
              onClick={handleClearSearch}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>

        {/* Invoice List */}
        <div className="flex-1 overflow-y-auto border rounded-md">
          {loading ? (
            <div className="p-4 space-y-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : invoices.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground">
                {searchTerm
                  ? "No matching invoices found."
                  : "No outstanding invoices available."}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Invoice #</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Due Date</TableHead>
                  <TableHead className="text-right">Balance</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invoices.map((invoice) => (
                  <TableRow
                    key={invoice.id}
                    className="cursor-pointer hover:bg-muted/50"
                    onClick={() => handleSelectInvoice(invoice.id)}
                  >
                    <TableCell className="font-mono text-sm font-medium">
                      {invoice.invoiceNumber}
                    </TableCell>
                    <TableCell>{invoice.customerName}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {formatDate(invoice.dueDate)}
                        {invoice.daysOverdue && invoice.daysOverdue > 0 && (
                          <Badge variant="destructive" className="text-xs">
                            {invoice.daysOverdue}d overdue
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right font-semibold">
                      {formatCurrency(invoice.balance)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
