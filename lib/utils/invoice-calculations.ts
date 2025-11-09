/**
 * Invoice Calculation Utilities
 * Matches backend calculation logic from LineItem domain model
 */

import type {
  LineItemFormData,
  CalculatedLineItem,
  InvoiceTotals,
} from '@/lib/api/types';

/**
 * Calculate all totals for a single line item
 * Logic matches backend LineItem.java calculations:
 * - subtotal = quantity * unitPrice
 * - discountAmount = subtotal * discountPercent
 * - taxableAmount = subtotal - discountAmount
 * - taxAmount = taxableAmount * taxRate
 * - total = taxableAmount + taxAmount
 */
export function calculateLineItemTotals(
  lineItem: LineItemFormData
): CalculatedLineItem {
  const { quantity, unitPrice, discountPercent, taxRate } = lineItem;

  // Calculate subtotal
  const subtotal = quantity * unitPrice;

  // Calculate discount amount
  const discountAmount = subtotal * (discountPercent || 0);

  // Calculate taxable amount (after discount)
  const taxableAmount = subtotal - discountAmount;

  // Calculate tax amount
  const taxAmount = taxableAmount * (taxRate || 0);

  // Calculate line item total
  const total = taxableAmount + taxAmount;

  return {
    ...lineItem,
    subtotal: roundToCurrency(subtotal),
    discountAmount: roundToCurrency(discountAmount),
    taxableAmount: roundToCurrency(taxableAmount),
    taxAmount: roundToCurrency(taxAmount),
    total: roundToCurrency(total),
  };
}

/**
 * Calculate invoice-level totals from all line items
 */
export function calculateInvoiceTotals(
  lineItems: LineItemFormData[]
): InvoiceTotals {
  // Calculate all line items first to get their totals
  const calculatedItems = lineItems.map(calculateLineItemTotals);

  // Sum up the totals
  const subtotal = calculatedItems.reduce(
    (sum, item) => sum + (item.subtotal || 0),
    0
  );
  const totalDiscount = calculatedItems.reduce(
    (sum, item) => sum + (item.discountAmount || 0),
    0
  );
  const totalTax = calculatedItems.reduce(
    (sum, item) => sum + (item.taxAmount || 0),
    0
  );
  const totalAmount = calculatedItems.reduce(
    (sum, item) => sum + (item.total || 0),
    0
  );

  return {
    subtotal: roundToCurrency(subtotal),
    totalDiscount: roundToCurrency(totalDiscount),
    totalTax: roundToCurrency(totalTax),
    totalAmount: roundToCurrency(totalAmount),
  };
}

/**
 * Round number to 2 decimal places for currency
 */
export function roundToCurrency(value: number): number {
  return Math.round(value * 100) / 100;
}

/**
 * Format number as currency string
 */
export function formatCurrency(
  value: number,
  currency: string = 'USD'
): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}
