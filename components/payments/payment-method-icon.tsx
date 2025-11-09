/**
 * PaymentMethodIcon Component
 * Icons for different payment methods
 */

import { CreditCard, Building2, FileText, Banknote } from 'lucide-react';
import type { PaymentMethod } from '@/lib/api/types';

interface PaymentMethodIconProps {
  method: PaymentMethod;
  className?: string;
}

export function PaymentMethodIcon({ method, className = "h-4 w-4" }: PaymentMethodIconProps) {
  switch (method) {
    case 'CREDIT_CARD':
      return <CreditCard className={className} aria-label="Credit Card" />;
    case 'BANK_TRANSFER':
      return <Building2 className={className} aria-label="Bank Transfer" />;
    case 'CHECK':
      return <FileText className={className} aria-label="Check" />;
    case 'CASH':
      return <Banknote className={className} aria-label="Cash" />;
    default:
      return <CreditCard className={className} aria-label="Payment" />;
  }
}

/**
 * Get payment method display label
 * @param method - Payment method enum value
 * @returns Human-readable label
 */
export function getPaymentMethodLabel(method: PaymentMethod): string {
  switch (method) {
    case 'CREDIT_CARD':
      return 'Credit Card';
    case 'BANK_TRANSFER':
      return 'Bank Transfer';
    case 'CHECK':
      return 'Check';
    case 'CASH':
      return 'Cash';
    default:
      return method;
  }
}
