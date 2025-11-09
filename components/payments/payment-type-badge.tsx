/**
 * PaymentTypeBadge Component
 * Visual indicator for payment type (full vs partial)
 */

import { CheckCircle2, Clock } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

export type PaymentType = 'full' | 'partial';

interface PaymentTypeBadgeProps {
  type: PaymentType;
}

export function PaymentTypeBadge({ type }: PaymentTypeBadgeProps) {
  if (type === 'full') {
    return (
      <Badge variant="default" className="bg-green-600 hover:bg-green-700">
        <CheckCircle2 className="mr-1 h-3 w-3" />
        Full Payment
      </Badge>
    );
  }

  return (
    <Badge variant="default" className="bg-blue-600 hover:bg-blue-700">
      <Clock className="mr-1 h-3 w-3" />
      Partial
    </Badge>
  );
}

/**
 * Helper function to determine payment type from payment data
 * @param remainingBalance - The invoice balance after this payment
 * @returns Payment type
 */
export function getPaymentType(remainingBalance: number): PaymentType {
  return remainingBalance === 0 ? 'full' : 'partial';
}
