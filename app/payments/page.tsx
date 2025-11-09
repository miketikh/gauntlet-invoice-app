"use client";

/**
 * Payments Page
 * Global payment list with filters, statistics, and payment details
 */

import { useState, useEffect } from 'react';
import { Loader2, Search, X, DollarSign, TrendingUp, Calendar, CreditCard } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { useRequireAuth } from '@/lib/hooks/useRequireAuth';
import { formatCurrency, formatDate } from '@/lib/utils';
import { getPayments, getPaymentStatistics } from '@/lib/api/payments';
import { PaymentDetailsModal } from '@/components/payments/payment-details-modal';
import { PaymentTypeBadge, getPaymentType } from '@/components/payments/payment-type-badge';
import { PaymentMethodIcon, getPaymentMethodLabel } from '@/components/payments/payment-method-icon';
import { InvoiceSelector } from '@/components/invoices/invoice-selector';
import { usePaymentModalStore } from '@/lib/stores/payment-modal-store';
import type {
  PaymentResponseDTO,
  PaymentHistoryFilters,
  Page,
  PaymentStatistics,
  PaymentMethod,
} from '@/lib/api/types';

const PAYMENT_METHODS: PaymentMethod[] = ['CREDIT_CARD', 'BANK_TRANSFER', 'CHECK', 'CASH'];

export default function PaymentsPage() {
  useRequireAuth();

  const { openPaymentModal } = usePaymentModalStore();
  const [payments, setPayments] = useState<Page<PaymentResponseDTO> | null>(null);
  const [statistics, setStatistics] = useState<PaymentStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPaymentId, setSelectedPaymentId] = useState<string | null>(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [invoiceSelectorOpen, setInvoiceSelectorOpen] = useState(false);

  // Filters state
  const [filters, setFilters] = useState<PaymentHistoryFilters>({
    page: 0,
    size: 20,
    sort: 'paymentDate,desc',
  });
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMethod, setSelectedMethod] = useState<string>('all');

  // Fetch payments
  useEffect(() => {
    const fetchPayments = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getPayments(filters);
        setPayments(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load payments');
      } finally {
        setLoading(false);
      }
    };

    fetchPayments();
  }, [filters]);

  // Fetch statistics
  useEffect(() => {
    const fetchStatistics = async () => {
      try {
        setStatsLoading(true);
        const data = await getPaymentStatistics();
        setStatistics(data);
      } catch (err: any) {
        console.error('Failed to load statistics:', err);
      } finally {
        setStatsLoading(false);
      }
    };

    fetchStatistics();
  }, []);

  // Listen for payment-recorded events to refresh payment list
  useEffect(() => {
    const handlePaymentRecorded = async () => {
      // Refresh payments list
      try {
        const data = await getPayments(filters);
        setPayments(data);
      } catch (err: any) {
        console.error('Failed to refresh payments:', err);
      }
      // Refresh statistics
      try {
        const stats = await getPaymentStatistics();
        setStatistics(stats);
      } catch (err: any) {
        console.error('Failed to refresh statistics:', err);
      }
    };
    window.addEventListener('payment-recorded', handlePaymentRecorded as EventListener);
    return () => window.removeEventListener('payment-recorded', handlePaymentRecorded as EventListener);
  }, [filters]);

  const handleSearch = () => {
    setFilters((prev) => ({
      ...prev,
      reference: searchTerm || undefined,
      page: 0,
    }));
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setSelectedMethod('all');
    setFilters({
      page: 0,
      size: 20,
      sort: 'paymentDate,desc',
    });
  };

  const handleMethodFilter = (value: string) => {
    setSelectedMethod(value);
    setFilters((prev) => ({
      ...prev,
      paymentMethod: value === 'all' ? undefined : [value as PaymentMethod],
      page: 0,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const handleRowClick = (paymentId: string) => {
    setSelectedPaymentId(paymentId);
    setDetailsModalOpen(true);
  };

  const handleExportCSV = () => {
    // Placeholder for CSV export
    alert('CSV export functionality coming soon!');
  };

  const handleSelectInvoice = (invoiceId: string) => {
    openPaymentModal(invoiceId);
  };

  const activeFiltersCount =
    (filters.reference ? 1 : 0) +
    (filters.paymentMethod?.length ? 1 : 0);

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Payment History</h1>
          <p className="text-muted-foreground mt-1">
            View and manage all payment transactions
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button onClick={() => setInvoiceSelectorOpen(true)}>
            <DollarSign className="h-4 w-4 mr-2" />
            Record Payment
          </Button>
          <Button variant="outline" onClick={handleExportCSV}>
            Export CSV
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statsLoading ? (
          <>
            {[1, 2, 3, 4].map((i) => (
              <Card key={i}>
                <CardHeader className="pb-3">
                  <Skeleton className="h-4 w-24" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-8 w-32" />
                </CardContent>
              </Card>
            ))}
          </>
        ) : statistics ? (
          <>
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <DollarSign className="h-4 w-4" />
                    Total Collected
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatCurrency(statistics.totalCollected)}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {statistics.totalPaymentCount} payments
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4" />
                    Collected Today
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatCurrency(statistics.collectedToday)}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4" />
                    This Month
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatCurrency(statistics.collectedThisMonth)}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <CreditCard className="h-4 w-4" />
                    This Year
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatCurrency(statistics.collectedThisYear)}
                </div>
              </CardContent>
            </Card>
          </>
        ) : null}
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">Filters</CardTitle>
            {activeFiltersCount > 0 && (
              <Badge variant="secondary">{activeFiltersCount} active</Badge>
            )}
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col md:flex-row gap-4">
            {/* Search by Reference */}
            <div className="flex-1">
              <div className="flex gap-2">
                <Input
                  placeholder="Search by reference number..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                />
                <Button onClick={handleSearch}>
                  <Search className="h-4 w-4" />
                </Button>
              </div>
            </div>

            {/* Payment Method Filter */}
            <div className="w-full md:w-48">
              <Select value={selectedMethod} onValueChange={handleMethodFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Payment Method" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Methods</SelectItem>
                  {PAYMENT_METHODS.map((method) => (
                    <SelectItem key={method} value={method}>
                      {getPaymentMethodLabel(method)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Clear Filters */}
            {activeFiltersCount > 0 && (
              <Button variant="outline" onClick={handleClearFilters}>
                <X className="h-4 w-4 mr-2" />
                Clear
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Payments Table */}
      <Card>
        <CardHeader>
          <CardTitle>
            {loading ? 'Loading payments...' :
             payments ? `Showing ${payments.content.length} of ${payments.totalElements} payments` :
             'Payments'}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : error ? (
            <div className="text-center py-8 text-destructive">
              <p>{error}</p>
            </div>
          ) : payments && payments.content.length > 0 ? (
            <>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Invoice #</TableHead>
                      <TableHead>Customer</TableHead>
                      <TableHead>Amount</TableHead>
                      <TableHead>Method</TableHead>
                      <TableHead>Reference</TableHead>
                      <TableHead>Balance</TableHead>
                      <TableHead>Type</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {payments.content.map((payment) => (
                      <TableRow
                        key={payment.id}
                        className="cursor-pointer hover:bg-muted/50"
                        onClick={() => handleRowClick(payment.id)}
                      >
                        <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                        <TableCell className="font-mono text-sm">
                          {payment.invoiceNumber}
                        </TableCell>
                        <TableCell>{payment.customerName}</TableCell>
                        <TableCell className="font-semibold">
                          {formatCurrency(payment.amount)}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <PaymentMethodIcon method={payment.paymentMethod} />
                            <span className="hidden md:inline">
                              {getPaymentMethodLabel(payment.paymentMethod)}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell className="font-mono text-sm">
                          {payment.reference}
                        </TableCell>
                        <TableCell className="font-semibold">
                          {formatCurrency(payment.runningBalance ?? payment.remainingBalance)}
                        </TableCell>
                        <TableCell>
                          <PaymentTypeBadge
                            type={getPaymentType(payment.runningBalance ?? payment.remainingBalance)}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Pagination */}
              {payments.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <div className="text-sm text-muted-foreground">
                    Page {payments.page + 1} of {payments.totalPages}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handlePageChange(payments.page - 1)}
                      disabled={payments.page === 0}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handlePageChange(payments.page + 1)}
                      disabled={payments.page >= payments.totalPages - 1}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-8">
              <p className="text-muted-foreground">No payments found</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Payment Details Modal */}
      <PaymentDetailsModal
        paymentId={selectedPaymentId}
        open={detailsModalOpen}
        onOpenChange={setDetailsModalOpen}
      />

      {/* Invoice Selector Modal */}
      <InvoiceSelector
        open={invoiceSelectorOpen}
        onOpenChange={setInvoiceSelectorOpen}
        onSelectInvoice={handleSelectInvoice}
      />
    </div>
  );
}
