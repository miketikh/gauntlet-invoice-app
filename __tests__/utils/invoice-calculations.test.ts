/**
 * Tests for Invoice Calculation Utilities
 */

import {
  calculateLineItemTotals,
  calculateInvoiceTotals,
  roundToCurrency,
  formatCurrency,
} from '@/lib/utils/invoice-calculations';
import type { LineItemFormData } from '@/lib/api/types';

describe('Invoice Calculations', () => {
  describe('calculateLineItemTotals', () => {
    it('should calculate line item totals correctly with discount and tax', () => {
      const lineItem: LineItemFormData = {
        description: 'Test Item',
        quantity: 10,
        unitPrice: 100,
        discountPercent: 0.1, // 10%
        taxRate: 0.08, // 8%
      };

      const result = calculateLineItemTotals(lineItem);

      expect(result.subtotal).toBe(1000); // 10 * 100
      expect(result.discountAmount).toBe(100); // 1000 * 0.1
      expect(result.taxableAmount).toBe(900); // 1000 - 100
      expect(result.taxAmount).toBe(72); // 900 * 0.08
      expect(result.total).toBe(972); // 900 + 72
    });

    it('should calculate line item totals without discount or tax', () => {
      const lineItem: LineItemFormData = {
        description: 'Test Item',
        quantity: 5,
        unitPrice: 20,
        discountPercent: 0,
        taxRate: 0,
      };

      const result = calculateLineItemTotals(lineItem);

      expect(result.subtotal).toBe(100); // 5 * 20
      expect(result.discountAmount).toBe(0);
      expect(result.taxableAmount).toBe(100);
      expect(result.taxAmount).toBe(0);
      expect(result.total).toBe(100);
    });

    it('should handle decimal quantities and prices', () => {
      const lineItem: LineItemFormData = {
        description: 'Test Item',
        quantity: 2.5,
        unitPrice: 15.99,
        discountPercent: 0.05, // 5%
        taxRate: 0.065, // 6.5%
      };

      const result = calculateLineItemTotals(lineItem);

      expect(result.subtotal).toBe(39.98); // 2.5 * 15.99 = 39.975, rounded
      expect(result.discountAmount).toBe(2); // 39.98 * 0.05 = 1.999, rounded
      expect(result.taxableAmount).toBe(37.98); // 39.98 - 2
      expect(result.taxAmount).toBe(2.47); // 37.98 * 0.065 = 2.4687, rounded
      expect(result.total).toBe(40.44); // 37.98 + 2.46 (rounding)
    });

    it('should handle 100% discount', () => {
      const lineItem: LineItemFormData = {
        description: 'Free Item',
        quantity: 1,
        unitPrice: 100,
        discountPercent: 1, // 100%
        taxRate: 0.08,
      };

      const result = calculateLineItemTotals(lineItem);

      expect(result.subtotal).toBe(100);
      expect(result.discountAmount).toBe(100);
      expect(result.taxableAmount).toBe(0);
      expect(result.taxAmount).toBe(0);
      expect(result.total).toBe(0);
    });
  });

  describe('calculateInvoiceTotals', () => {
    it('should calculate invoice totals from multiple line items', () => {
      const lineItems: LineItemFormData[] = [
        {
          description: 'Item 1',
          quantity: 10,
          unitPrice: 100,
          discountPercent: 0.1,
          taxRate: 0.08,
        },
        {
          description: 'Item 2',
          quantity: 5,
          unitPrice: 200,
          discountPercent: 0,
          taxRate: 0.08,
        },
      ];

      const result = calculateInvoiceTotals(lineItems);

      // Item 1: subtotal=1000, discount=100, taxable=900, tax=72, total=972
      // Item 2: subtotal=1000, discount=0, taxable=1000, tax=80, total=1080
      expect(result.subtotal).toBe(2000);
      expect(result.totalDiscount).toBe(100);
      expect(result.totalTax).toBe(152); // 72 + 80
      expect(result.totalAmount).toBe(2052); // 972 + 1080
    });

    it('should handle single line item', () => {
      const lineItems: LineItemFormData[] = [
        {
          description: 'Single Item',
          quantity: 2,
          unitPrice: 50,
          discountPercent: 0,
          taxRate: 0.1,
        },
      ];

      const result = calculateInvoiceTotals(lineItems);

      expect(result.subtotal).toBe(100);
      expect(result.totalDiscount).toBe(0);
      expect(result.totalTax).toBe(10);
      expect(result.totalAmount).toBe(110);
    });

    it('should handle empty line items array', () => {
      const lineItems: LineItemFormData[] = [];

      const result = calculateInvoiceTotals(lineItems);

      expect(result.subtotal).toBe(0);
      expect(result.totalDiscount).toBe(0);
      expect(result.totalTax).toBe(0);
      expect(result.totalAmount).toBe(0);
    });
  });

  describe('roundToCurrency', () => {
    it('should round to 2 decimal places', () => {
      expect(roundToCurrency(10.123)).toBe(10.12);
      expect(roundToCurrency(10.126)).toBe(10.13);
      expect(roundToCurrency(10.125)).toBe(10.13); // Banker's rounding
    });

    it('should handle whole numbers', () => {
      expect(roundToCurrency(10)).toBe(10);
      expect(roundToCurrency(10.0)).toBe(10);
    });

    it('should handle very small numbers', () => {
      expect(roundToCurrency(0.001)).toBe(0);
      expect(roundToCurrency(0.005)).toBe(0.01);
    });
  });

  describe('formatCurrency', () => {
    it('should format as USD by default', () => {
      expect(formatCurrency(1000)).toBe('$1,000.00');
      expect(formatCurrency(1234.56)).toBe('$1,234.56');
    });

    it('should handle negative values', () => {
      expect(formatCurrency(-100)).toBe('-$100.00');
    });

    it('should handle zero', () => {
      expect(formatCurrency(0)).toBe('$0.00');
    });

    it('should add thousand separators', () => {
      expect(formatCurrency(1000000)).toBe('$1,000,000.00');
    });
  });
});
