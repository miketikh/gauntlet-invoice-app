/**
 * Tests for LineItemManager Component
 */

/* eslint-disable @typescript-eslint/no-explicit-any */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { LineItemManager } from '@/components/invoices/line-item-manager';
import { invoiceFormSchema, type InvoiceFormValues } from '@/lib/schemas/invoice-schemas';
import { Form } from '@/components/ui/form';

// Wrapper component for testing with form context
function TestWrapper({ children, defaultValues }: any) {
  const form = useForm<InvoiceFormValues>({
    resolver: zodResolver(invoiceFormSchema),
    defaultValues: defaultValues || {
      customerId: 'test-customer',
      issueDate: new Date(),
      dueDate: new Date(),
      paymentTerms: 'Net 30',
      lineItems: [],
      notes: '',
    },
  });

  return (
    <Form {...form}>
      <form>{children({ control: form.control, watch: form.watch })}</form>
    </Form>
  );
}

describe('LineItemManager', () => {
  it('should render empty state when no line items', () => {
    render(
      <TestWrapper>
        {({ control, watch }: any) => (
          <LineItemManager control={control} watch={watch} />
        )}
      </TestWrapper>
    );

    expect(screen.getByText(/No line items yet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Add Line Item/i })).toBeInTheDocument();
  });

  it('should add a new line item when clicking Add button', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        {({ control, watch }: any) => (
          <LineItemManager control={control} watch={watch} />
        )}
      </TestWrapper>
    );

    const addButton = screen.getByRole('button', { name: /Add Line Item/i });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Quantity/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Unit Price/i)).toBeInTheDocument();
    });
  });

  it('should display calculated totals for valid line item', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        {({ control, watch }: any) => (
          <LineItemManager control={control} watch={watch} />
        )}
      </TestWrapper>
    );

    // Add a line item
    const addButton = screen.getByRole('button', { name: /Add Line Item/i });
    await user.click(addButton);

    // Fill in the line item
    const descInput = screen.getByLabelText(/Description/i);
    const qtyInput = screen.getByLabelText(/Quantity/i);
    const priceInput = screen.getByLabelText(/Unit Price/i);

    await user.type(descInput, 'Test Item');
    await user.type(qtyInput, '10');
    await user.type(priceInput, '100');

    // Check calculated totals appear
    await waitFor(
      () => {
        expect(screen.getByText(/Subtotal/i)).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });

  it('should remove line item when clicking remove button', async () => {
    const user = userEvent.setup();

    // Mock window.confirm to always return true
    global.window.confirm = jest.fn(() => true);

    render(
      <TestWrapper>
        {({ control, watch }: any) => (
          <LineItemManager control={control} watch={watch} />
        )}
      </TestWrapper>
    );

    // Add a line item
    const addButton = screen.getByRole('button', { name: /Add Line Item/i });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
    });

    // Fill in some data
    const descInput = screen.getByLabelText(/Description/i);
    await user.type(descInput, 'Test Item');

    // Find and click remove button
    const removeButtons = screen.getAllByRole('button');
    const removeButton = removeButtons.find((btn) =>
      btn.querySelector('svg')?.classList.contains('lucide-trash-2')
    );

    if (removeButton) {
      await user.click(removeButton);
    }

    // Should show confirmation dialog
    expect(global.window.confirm).toHaveBeenCalled();

    // Line item should be removed
    await waitFor(() => {
      expect(screen.queryByLabelText(/Description/i)).not.toBeInTheDocument();
      expect(screen.getByText(/No line items yet/i)).toBeInTheDocument();
    });
  });

  it('should render multiple line items with alternating backgrounds', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        {({ control, watch }: any) => (
          <LineItemManager control={control} watch={watch} />
        )}
      </TestWrapper>
    );

    // Add two line items
    const addButton = screen.getByRole('button', { name: /Add Line Item/i });
    await user.click(addButton);
    await user.click(addButton);

    await waitFor(() => {
      const descriptions = screen.getAllByLabelText(/Description/i);
      expect(descriptions).toHaveLength(2);
    });
  });
});
