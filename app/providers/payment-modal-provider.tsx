"use client";

/**
 * Payment Modal Provider
 * Renders the globally accessible PaymentForm modal
 * Allows payment recording from anywhere in the app
 */

import { PaymentForm } from "@/components/payments/payment-form";

export function PaymentModalProvider() {
  return <PaymentForm />;
}
