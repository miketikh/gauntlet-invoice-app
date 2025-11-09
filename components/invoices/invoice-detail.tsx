"use client";

/**
 * Invoice Detail Component
 * Displays full invoice details with action buttons and print layout
 */

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import {
  Edit,
  Send,
  Copy,
  Trash2,
  Printer,
  Download,
  ArrowLeft,
  DollarSign,
} from "lucide-react";
import type { InvoiceResponseDTO, InvoiceStatus } from "@/lib/api/types";
import { SendConfirmationDialog } from "./send-confirmation-dialog";
import { DeleteConfirmationDialog } from "./delete-confirmation-dialog";
import { PaymentForm } from "@/components/payments/payment-form";
import { PaymentHistory } from "@/components/payments/payment-history";
import { getPaymentsByInvoice } from "@/lib/api/payments";
import { toast } from "sonner";

interface InvoiceDetailProps {
  invoice: InvoiceResponseDTO;
  onSend?: () => Promise<void>;
  onDelete?: () => Promise<void>;
  onCopy?: () => Promise<void>;
  onPaymentRecorded?: () => Promise<void>;
}

function StatusBadge({ status }: { status: InvoiceStatus }) {
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

export function InvoiceDetail({
  invoice,
  onSend,
  onDelete,
  onCopy,
  onPaymentRecorded,
}: InvoiceDetailProps) {
  const router = useRouter();
  const [sendDialogOpen, setSendDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [paymentCount, setPaymentCount] = useState<number>(0);

  // Fetch payment count for the badge
  useEffect(() => {
    const fetchPaymentCount = async () => {
      try {
        const payments = await getPaymentsByInvoice(invoice.id);
        setPaymentCount(payments.length);
      } catch (error) {
        // Silently fail - payment count is not critical
        setPaymentCount(0);
      }
    };
    fetchPaymentCount();
  }, [invoice.id]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const handleSendConfirm = async () => {
    if (!onSend) return;
    setLoading(true);
    try {
      await onSend();
      toast.success("Invoice sent successfully");
      setSendDialogOpen(false);
    } catch (error) {
      toast.error("Failed to send invoice");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!onDelete) return;
    setLoading(true);
    try {
      await onDelete();
      toast.success("Invoice deleted successfully");
      setDeleteDialogOpen(false);
      router.push("/invoices");
    } catch (error) {
      toast.error("Failed to delete invoice");
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    if (!onCopy) return;
    try {
      await onCopy();
      toast.success("Invoice copied. Redirecting to edit...");
    } catch (error) {
      toast.error("Failed to copy invoice");
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const handleExportPDF = () => {
    toast.info("PDF export coming soon!");
  };

  const isDraft = invoice.status === "Draft";
  const isSent = invoice.status === "Sent";
  const isPaid = invoice.status === "Paid";
  const isOverdue = invoice.daysOverdue && invoice.daysOverdue > 0;

  // Determine if payment button should be disabled
  const canRecordPayment = isSent && invoice.balance > 0;

  // Get tooltip message for disabled payment button
  const getPaymentButtonTooltip = () => {
    if (isDraft) return "Cannot record payment for draft invoices. Send invoice first.";
    if (isPaid || invoice.balance <= 0) return "This invoice is fully paid.";
    return "";
  };

  const handlePaymentSuccess = async () => {
    if (onPaymentRecorded) {
      await onPaymentRecorded();
    }
    // Refresh payment count
    try {
      const payments = await getPaymentsByInvoice(invoice.id);
      setPaymentCount(payments.length);
    } catch (error) {
      // Silently fail
    }
  };

  return (
    <div className="space-y-6">
      {/* Action Buttons (hidden when printing) */}
      <div className="flex items-center justify-between print:hidden">
        <Button
          variant="outline"
          size="sm"
          onClick={() => router.push("/invoices")}
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Back to Invoices
        </Button>

        <div className="flex items-center gap-2">
          {/* Edit - only for Draft */}
          {isDraft && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push(`/invoices/${invoice.id}/edit`)}
            >
              <Edit className="h-4 w-4 mr-2" />
              Edit
            </Button>
          )}

          {/* Send - only for Draft */}
          {isDraft && onSend && (
            <Button
              variant="default"
              size="sm"
              onClick={() => setSendDialogOpen(true)}
            >
              <Send className="h-4 w-4 mr-2" />
              Send
            </Button>
          )}

          {/* Record Payment - only for Sent invoices with balance */}
          {onPaymentRecorded && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div>
                    <Button
                      variant="default"
                      size="sm"
                      onClick={() => setPaymentDialogOpen(true)}
                      disabled={!canRecordPayment}
                    >
                      <DollarSign className="h-4 w-4 mr-2" />
                      Record Payment
                    </Button>
                  </div>
                </TooltipTrigger>
                {!canRecordPayment && (
                  <TooltipContent>
                    <p>{getPaymentButtonTooltip()}</p>
                  </TooltipContent>
                )}
              </Tooltip>
            </TooltipProvider>
          )}

          {/* Copy - available for all */}
          {onCopy && (
            <Button variant="outline" size="sm" onClick={handleCopy}>
              <Copy className="h-4 w-4 mr-2" />
              Copy
            </Button>
          )}

          {/* Print - available for all */}
          <Button variant="outline" size="sm" onClick={handlePrint}>
            <Printer className="h-4 w-4 mr-2" />
            Print
          </Button>

          {/* Export PDF - placeholder */}
          <Button variant="outline" size="sm" onClick={handleExportPDF}>
            <Download className="h-4 w-4 mr-2" />
            Export PDF
          </Button>

          {/* Delete - only for Draft */}
          {isDraft && onDelete && (
            <Button
              variant="destructive"
              size="sm"
              onClick={() => setDeleteDialogOpen(true)}
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Delete
            </Button>
          )}
        </div>
      </div>

      {/* Invoice Header */}
      <Card>
        <CardHeader>
          <div className="flex items-start justify-between">
            <div>
              <CardTitle className="text-3xl">
                Invoice {invoice.invoiceNumber}
              </CardTitle>
              <p className="text-muted-foreground mt-2">
                {invoice.customerName}
              </p>
            </div>
            <div className="text-right">
              <StatusBadge status={invoice.status} />
              {isOverdue && (
                <div className="mt-2 text-sm font-semibold text-red-600 dark:text-red-400">
                  Overdue by {invoice.daysOverdue} days
                </div>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <div className="text-sm font-semibold text-muted-foreground mb-1">
                Issue Date
              </div>
              <div>{formatDate(invoice.issueDate)}</div>
            </div>
            <div>
              <div className="text-sm font-semibold text-muted-foreground mb-1">
                Due Date
              </div>
              <div>{formatDate(invoice.dueDate)}</div>
            </div>
            <div>
              <div className="text-sm font-semibold text-muted-foreground mb-1">
                Payment Terms
              </div>
              <div>{invoice.paymentTerms}</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tabs for Details and Payments */}
      <Tabs defaultValue="details" className="w-full">
        <TabsList>
          <TabsTrigger value="details">Details</TabsTrigger>
          <TabsTrigger value="payments">
            Payments {paymentCount > 0 && `(${paymentCount})`}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="details" className="space-y-6 mt-6">
          {/* Details Tab Content - existing invoice details */}

      {/* Customer Information */}
      <Card>
        <CardHeader>
          <CardTitle>Bill To</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-1">
            <div className="font-semibold">{invoice.customerName}</div>
            <div className="text-sm text-muted-foreground">
              {invoice.customerEmail}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Line Items */}
      <Card>
        <CardHeader>
          <CardTitle>Line Items</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Description</TableHead>
                <TableHead className="text-right">Quantity</TableHead>
                <TableHead className="text-right">Unit Price</TableHead>
                <TableHead className="text-right">Discount</TableHead>
                <TableHead className="text-right">Tax</TableHead>
                <TableHead className="text-right">Total</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {invoice.lineItems.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>{item.description}</TableCell>
                  <TableCell className="text-right">{item.quantity}</TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(item.unitPrice)}
                  </TableCell>
                  <TableCell className="text-right">
                    {item.discountPercent > 0
                      ? `${(item.discountPercent * 100).toFixed(0)}% (${formatCurrency(item.discountAmount)})`
                      : "-"}
                  </TableCell>
                  <TableCell className="text-right">
                    {item.taxRate > 0
                      ? `${(item.taxRate * 100).toFixed(0)}% (${formatCurrency(item.taxAmount)})`
                      : "-"}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(item.total)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Totals */}
      <Card>
        <CardContent className="pt-6">
          <div className="space-y-2 max-w-md ml-auto">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Subtotal</span>
              <span>{formatCurrency(invoice.subtotal)}</span>
            </div>
            {invoice.totalDiscount > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Total Discount</span>
                <span className="text-green-600 dark:text-green-400">
                  -{formatCurrency(invoice.totalDiscount)}
                </span>
              </div>
            )}
            {invoice.totalTax > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Total Tax</span>
                <span>{formatCurrency(invoice.totalTax)}</span>
              </div>
            )}
            <Separator />
            <div className="flex justify-between text-lg font-semibold">
              <span>Total Amount</span>
              <span>{formatCurrency(invoice.totalAmount)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Balance Due</span>
              <span
                className={
                  invoice.balance > 0
                    ? "font-semibold text-orange-600 dark:text-orange-400"
                    : "text-green-600 dark:text-green-400"
                }
              >
                {formatCurrency(invoice.balance)}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

          {/* Notes */}
          {invoice.notes && (
            <Card>
              <CardHeader>
                <CardTitle>Notes</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm whitespace-pre-wrap">{invoice.notes}</p>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="payments" className="mt-6">
          {/* Payments Tab Content */}
          <PaymentHistory invoiceId={invoice.id} invoice={invoice} />
        </TabsContent>
      </Tabs>

      {/* Dialogs */}
      <SendConfirmationDialog
        invoice={invoice}
        open={sendDialogOpen}
        onOpenChange={setSendDialogOpen}
        onConfirm={handleSendConfirm}
        loading={loading}
      />

      <DeleteConfirmationDialog
        invoice={invoice}
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDeleteConfirm}
        loading={loading}
      />

      <PaymentForm
        invoice={invoice}
        open={paymentDialogOpen}
        onOpenChange={setPaymentDialogOpen}
        onSuccess={handlePaymentSuccess}
      />
    </div>
  );
}
