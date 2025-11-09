/**
 * InvoiceForm Component
 * Main form for creating and editing invoices
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2, Save } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { LineItemManager } from './line-item-manager';
import { DatePicker } from './date-picker';
import { useInvoiceStore } from '@/lib/stores/invoice-store';
import { customerApi } from '@/lib/api/customers';
import { useAutoSave, loadDraft } from '@/lib/hooks/useAutoSave';
import {
  invoiceFormSchema,
  type InvoiceFormValues,
} from '@/lib/schemas/invoice-schemas';
import {
  calculateInvoiceTotals,
  formatCurrency,
} from '@/lib/utils/invoice-calculations';
import type { InvoiceResponseDTO, CustomerListItemDTO } from '@/lib/api/types';

interface InvoiceFormProps {
  mode: 'create' | 'edit';
  initialData?: InvoiceResponseDTO;
  onSuccess?: (invoice: InvoiceResponseDTO) => void;
}

export function InvoiceForm({
  mode,
  initialData,
  onSuccess,
}: InvoiceFormProps) {
  const router = useRouter();
  const [customers, setCustomers] = useState<CustomerListItemDTO[]>([]);
  const [loadingCustomers, setLoadingCustomers] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showDraftRestore, setShowDraftRestore] = useState(false);

  const { createInvoice, updateInvoice, loading, error } = useInvoiceStore();

  // Storage key for auto-save
  const storageKey = `invoice-draft-${mode}-${initialData?.id || 'new'}-${Date.now()}`;

  // Initialize form with default values or initial data
  const defaultValues: InvoiceFormValues = initialData
    ? {
        customerId: initialData.customerId,
        issueDate: new Date(initialData.issueDate),
        dueDate: new Date(initialData.dueDate),
        paymentTerms: initialData.paymentTerms,
        lineItems: initialData.lineItems.map((item) => ({
          description: item.description,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          discountPercent: item.discountPercent,
          taxRate: item.taxRate,
        })),
        notes: initialData.notes,
      }
    : {
        customerId: '',
        issueDate: new Date(),
        dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
        paymentTerms: 'Net 30',
        lineItems: [],
        notes: '',
      };

  const form = useForm<InvoiceFormValues>({
    resolver: zodResolver(invoiceFormSchema),
    defaultValues,
  });

  const { watch, control } = form;

  // Watch form data for auto-save
  const formData = watch();

  // Auto-save functionality
  const { saveStatus, lastSaved, manualSave, clearDraft } = useAutoSave({
    storageKey,
    data: formData,
    delay: 30000, // 30 seconds
    enabled: mode === 'create' || initialData?.status === 'Draft',
  });

  // Calculate invoice totals in real-time
  const totals = calculateInvoiceTotals(formData.lineItems || []);

  // Fetch customers on mount
  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        setLoadingCustomers(true);
        const response = await customerApi.getCustomers({ size: 100 });
        setCustomers(response.content);
      } catch (err) {
        toast.error('Failed to load customers');
        console.error('Error loading customers:', err);
      } finally {
        setLoadingCustomers(false);
      }
    };

    fetchCustomers();
  }, []);

  // Check for draft on mount (create mode only)
  useEffect(() => {
    if (mode === 'create') {
      const draft = loadDraft<InvoiceFormValues>(storageKey);
      if (draft) {
        setShowDraftRestore(true);
      }
    }
  }, [mode, storageKey]);

  // Restore draft
  const restoreDraft = () => {
    const draft = loadDraft<InvoiceFormValues>(storageKey);
    if (draft) {
      form.reset(draft.data);
      setShowDraftRestore(false);
      toast.success('Draft restored');
    }
  };

  // Dismiss draft
  const dismissDraft = () => {
    clearDraft();
    setShowDraftRestore(false);
  };

  // Handle form submission
  const onSubmit = async (data: InvoiceFormValues) => {
    // Check if editing non-draft invoice
    if (mode === 'edit' && initialData && initialData.status !== 'Draft') {
      toast.error('Only draft invoices can be edited');
      return;
    }

    setIsSubmitting(true);
    try {
      let invoice: InvoiceResponseDTO;

      if (mode === 'create') {
        invoice = await createInvoice(data);
        toast.success('Invoice created successfully');
      } else if (mode === 'edit' && initialData) {
        invoice = await updateInvoice(initialData.id, data, initialData.version);
        toast.success('Invoice updated successfully');
      } else {
        throw new Error('Invalid mode or missing data');
      }

      // Clear draft after successful submission
      clearDraft();

      // Call success callback if provided
      if (onSuccess) {
        onSuccess(invoice);
      }

      // Redirect to invoice detail page
      router.push(`/invoices/${invoice.id}`);
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string; errors?: unknown } }; message?: string };
      // Handle specific errors
      if (error.response?.status === 409) {
        toast.error('Invoice was updated by another user. Please reload and try again.');
      } else if (error.response?.status === 400) {
        const validationErrors = error.response?.data?.errors;
        if (validationErrors) {
          toast.error('Validation errors occurred. Please check the form.');
        } else {
          toast.error(error.response?.data?.message || 'Validation failed');
        }
      } else {
        toast.error(`Failed to ${mode} invoice`);
      }
      console.error(`Error ${mode}ing invoice:`, err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    router.push('/invoices');
  };

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        manualSave();
      }
      if (e.key === 'Escape') {
        router.push('/invoices');
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [manualSave, router]);

  return (
    <div className="space-y-6">
      {/* Draft Restore Banner */}
      {showDraftRestore && (
        <Card className="border-blue-500">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-semibold">Draft Found</h3>
                <p className="text-sm text-muted-foreground">
                  We found a saved draft. Would you like to restore it?
                </p>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={dismissDraft}>
                  Dismiss
                </Button>
                <Button size="sm" onClick={restoreDraft}>
                  Restore
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
          {/* Invoice Details Section */}
          <Card>
            <CardHeader>
              <CardTitle>Invoice Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Customer Selection */}
                <FormField
                  control={control}
                  name="customerId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Customer *</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                        disabled={loadingCustomers}
                      >
                        <FormControl>
                          <SelectTrigger>
                            {loadingCustomers ? (
                              <Skeleton className="h-4 w-full" />
                            ) : (
                              <SelectValue placeholder="Select a customer" />
                            )}
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {customers.map((customer) => (
                            <SelectItem key={customer.id} value={customer.id}>
                              {customer.name} ({customer.email})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Payment Terms */}
                <FormField
                  control={control}
                  name="paymentTerms"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Payment Terms *</FormLabel>
                      <FormControl>
                        <Input {...field} placeholder="e.g., Net 30" />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Issue Date */}
                <FormField
                  control={control}
                  name="issueDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Issue Date *</FormLabel>
                      <FormControl>
                        <DatePicker
                          date={field.value}
                          onDateChange={field.onChange}
                          placeholder="Select issue date"
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Due Date */}
                <FormField
                  control={control}
                  name="dueDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Due Date *</FormLabel>
                      <FormControl>
                        <DatePicker
                          date={field.value}
                          onDateChange={field.onChange}
                          placeholder="Select due date"
                          fromDate={formData.issueDate}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Notes */}
              <FormField
                control={control}
                name="notes"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Notes (Optional)</FormLabel>
                    <FormControl>
                      <Textarea
                        {...field}
                        placeholder="Add any additional notes..."
                        rows={3}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Line Items Section */}
          <Card>
            <CardHeader>
              <CardTitle>Line Items</CardTitle>
            </CardHeader>
            <CardContent>
              <LineItemManager control={control} watch={watch} />
            </CardContent>
          </Card>

          {/* Invoice Totals Section */}
          <Card>
            <CardHeader>
              <CardTitle>Invoice Totals</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">Subtotal</div>
                  <div className="text-lg font-semibold">
                    {formatCurrency(totals.subtotal)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    Total Discount
                  </div>
                  <div className="text-lg font-semibold">
                    {formatCurrency(totals.totalDiscount)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">Total Tax</div>
                  <div className="text-lg font-semibold">
                    {formatCurrency(totals.totalTax)}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">
                    Total Amount
                  </div>
                  <div className="text-2xl font-bold text-primary">
                    {formatCurrency(totals.totalAmount)}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Form Actions */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              {saveStatus === 'saving' && (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Saving draft...</span>
                </>
              )}
              {saveStatus === 'saved' && lastSaved && (
                <span>
                  Last saved: {lastSaved.toLocaleTimeString()}
                </span>
              )}
              {saveStatus === 'error' && (
                <span className="text-destructive">Failed to save draft</span>
              )}
            </div>

            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={handleCancel}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={manualSave}
                disabled={isSubmitting}
              >
                <Save className="h-4 w-4 mr-2" />
                Save Draft
              </Button>
              <Button type="submit" disabled={isSubmitting || loading}>
                {isSubmitting || loading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    {mode === 'create' ? 'Creating...' : 'Updating...'}
                  </>
                ) : mode === 'create' ? (
                  'Create Invoice'
                ) : (
                  'Update Invoice'
                )}
              </Button>
            </div>
          </div>

          {/* Error Display */}
          {error && (
            <div className="text-sm text-destructive bg-destructive/10 p-3 rounded-md">
              {error}
            </div>
          )}
        </form>
      </Form>
    </div>
  );
}
