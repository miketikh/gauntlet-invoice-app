"use client";

/**
 * PaymentForm Component
 * Global modal dialog form for recording payments against invoices
 * Fetches invoice data internally based on invoiceId from Zustand store
 */

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { Loader2, DollarSign } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { DatePicker } from "@/components/invoices/date-picker";
import { formatCurrency } from "@/lib/utils";
import { recordPayment } from "@/lib/api/payments";
import { getInvoiceById } from "@/lib/api/invoices";
import { usePaymentModalStore } from "@/lib/stores/payment-modal-store";
import type { InvoiceResponseDTO, PaymentMethod } from "@/lib/api/types";

interface PaymentFormProps {
  onSuccess?: () => void; // Optional callback for additional success handling
}

// Payment method options
const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CREDIT_CARD", label: "Credit Card" },
  { value: "BANK_TRANSFER", label: "Bank Transfer" },
  { value: "CHECK", label: "Check" },
  { value: "CASH", label: "Cash" },
];

// Validation schema with Zod
const paymentFormSchema = z.object({
  paymentDate: z.date().refine((date) => date <= new Date(), {
    message: "Payment date cannot be in the future",
  }),
  amount: z.number().min(0.01, "Amount must be at least $0.01"),
  paymentMethod: z.enum(["CREDIT_CARD", "BANK_TRANSFER", "CHECK", "CASH"]),
  reference: z.string().min(1, "Reference number is required"),
  notes: z.string().optional(),
});

type PaymentFormValues = z.infer<typeof paymentFormSchema>;

export function PaymentForm({ onSuccess }: PaymentFormProps) {
  // Connect to Zustand store
  const { isOpen, invoiceId, closePaymentModal } = usePaymentModalStore();

  // Local state
  const [invoice, setInvoice] = useState<InvoiceResponseDTO | null>(null);
  const [loadingInvoice, setLoadingInvoice] = useState(false);
  const [loadingPayment, setLoadingPayment] = useState(false);
  const [remainingBalance, setRemainingBalance] = useState(0);

  const form = useForm<PaymentFormValues>({
    resolver: zodResolver(paymentFormSchema),
    defaultValues: {
      paymentDate: new Date(),
      amount: 0,
      paymentMethod: "CREDIT_CARD",
      reference: "",
      notes: "",
    },
  });

  // Fetch invoice data when invoiceId changes
  useEffect(() => {
    const fetchInvoice = async () => {
      if (!invoiceId) {
        setInvoice(null);
        return;
      }

      setLoadingInvoice(true);
      try {
        const data = await getInvoiceById(invoiceId);
        setInvoice(data);
        setRemainingBalance(data.balance);
      } catch (error: any) {
        toast.error(error.message || "Failed to load invoice");
        closePaymentModal();
      } finally {
        setLoadingInvoice(false);
      }
    };

    fetchInvoice();
  }, [invoiceId, closePaymentModal]);

  // Custom validation for amount not exceeding balance
  const validateAmount = (amount: number): boolean => {
    if (!invoice) return false;

    if (amount > invoice.balance) {
      form.setError("amount", {
        type: "manual",
        message: `Amount cannot exceed balance due (${formatCurrency(invoice.balance)})`,
      });
      return false;
    }
    form.clearErrors("amount");
    return true;
  };

  // Watch amount field for real-time balance calculation
  const watchAmount = form.watch("amount");

  useEffect(() => {
    if (!invoice) return;

    const amount = watchAmount || 0;
    const remaining = invoice.balance - amount;
    setRemainingBalance(remaining);

    // Validate amount on change
    if (amount > 0) {
      validateAmount(amount);
    }
  }, [watchAmount, invoice]);

  // Reset form when dialog opens/closes
  useEffect(() => {
    if (!isOpen) {
      form.reset({
        paymentDate: new Date(),
        amount: 0,
        paymentMethod: "CREDIT_CARD",
        reference: "",
        notes: "",
      });
      if (invoice) {
        setRemainingBalance(invoice.balance);
      }
    }
  }, [isOpen, form, invoice]);

  const onSubmit = async (values: PaymentFormValues) => {
    if (!invoice) return;

    // Final validation
    if (!validateAmount(values.amount)) {
      return;
    }

    setLoadingPayment(true);
    try {
      const response = await recordPayment(invoice.id, {
        paymentDate: values.paymentDate.toISOString().split("T")[0],
        amount: values.amount,
        paymentMethod: values.paymentMethod,
        reference: values.reference,
        notes: values.notes || undefined,
      });

      // Show success toast
      toast.success(`Payment of ${formatCurrency(values.amount)} recorded successfully`);

      // Check if invoice is now fully paid
      if (response.remainingBalance === 0) {
        toast.success(`Invoice #${invoice.invoiceNumber} is now fully paid`);
      }

      // Call optional success callback
      if (onSuccess) {
        onSuccess();
      }

      // Dispatch custom event for listeners (like invoice detail page)
      window.dispatchEvent(new CustomEvent('payment-recorded', {
        detail: { invoiceId: invoice.id }
      }));

      // Close dialog
      closePaymentModal();
    } catch (error: any) {
      toast.error(error.message || "Failed to record payment");
    } finally {
      setLoadingPayment(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={closePaymentModal}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Record Payment</DialogTitle>
          <DialogDescription>
            Record a payment for this invoice. The invoice balance will be updated
            automatically.
          </DialogDescription>
        </DialogHeader>

        {/* Loading State */}
        {loadingInvoice && (
          <div className="space-y-4">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        )}

        {/* Invoice Context Display */}
        {!loadingInvoice && invoice && (
          <>
            <div className="bg-muted/50 rounded-lg p-4 space-y-2">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">Invoice Number:</span>
                  <div className="font-semibold">{invoice.invoiceNumber}</div>
                </div>
                <div>
                  <span className="text-muted-foreground">Customer:</span>
                  <div className="font-semibold">{invoice.customerName}</div>
                </div>
                <div>
                  <span className="text-muted-foreground">Invoice Total:</span>
                  <div className="font-semibold">{formatCurrency(invoice.totalAmount)}</div>
                </div>
                <div>
                  <span className="text-muted-foreground">Current Balance:</span>
                  <div className="font-semibold text-orange-600 dark:text-orange-400">
                    {formatCurrency(invoice.balance)}
                  </div>
                </div>
              </div>
            </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            {/* Payment Date */}
            <FormField
              control={form.control}
              name="paymentDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Payment Date</FormLabel>
                  <FormControl>
                    <DatePicker
                      date={field.value}
                      onDateChange={field.onChange}
                      placeholder="Select payment date"
                      toDate={new Date()}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Amount */}
            <FormField
              control={form.control}
              name="amount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Amount</FormLabel>
                  <FormControl>
                    <div className="relative">
                      <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        className="pl-9 text-lg font-semibold"
                        {...field}
                        onChange={(e) => {
                          const value = e.target.value === '' ? 0 : parseFloat(e.target.value);
                          field.onChange(value);
                        }}
                      />
                    </div>
                  </FormControl>
                  <FormMessage />

                  {/* Remaining Balance Display */}
                  {watchAmount > 0 && (
                    <div className="text-sm">
                      <span className="text-muted-foreground">Remaining Balance: </span>
                      <span
                        className={
                          remainingBalance >= 0
                            ? "font-semibold text-green-600 dark:text-green-400"
                            : "font-semibold text-red-600 dark:text-red-400"
                        }
                      >
                        {formatCurrency(remainingBalance)}
                      </span>
                      {remainingBalance < 0 && (
                        <div className="text-red-600 dark:text-red-400 font-medium mt-1">
                          Amount exceeds balance!
                        </div>
                      )}
                    </div>
                  )}
                </FormItem>
              )}
            />

            {/* Payment Method */}
            <FormField
              control={form.control}
              name="paymentMethod"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Payment Method</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select payment method" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {PAYMENT_METHODS.map((method) => (
                        <SelectItem key={method.value} value={method.value}>
                          {method.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Reference Number */}
            <FormField
              control={form.control}
              name="reference"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Reference Number</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Transaction ID, check number, etc."
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Notes (Optional) */}
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notes (Optional)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Add any additional notes about this payment..."
                      className="resize-none"
                      rows={3}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={closePaymentModal}
                disabled={loadingPayment}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={loadingPayment}>
                {loadingPayment && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {loadingPayment ? "Recording..." : "Record Payment"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
