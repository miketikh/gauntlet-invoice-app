package com.invoiceme.payment.domain;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PaymentService
 * Domain service for payment-invoice reconciliation business logic
 * Handles cross-aggregate validation and calculations between Payment and Invoice
 */
@Service
public class PaymentService {

    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("0.01");

    /**
     * Validates that a payment can be applied to an invoice
     * Checks business rules:
     * - Invoice must be in Sent status (not Draft or already Paid)
     * - Payment amount must not exceed invoice balance
     *
     * @param payment The payment to validate
     * @param invoice The invoice to apply payment to
     * @throws InvalidPaymentException if validation fails
     */
    public void validatePaymentAgainstInvoice(Payment payment, Invoice invoice) {
        if (payment == null) {
            throw new InvalidPaymentException("Payment cannot be null");
        }
        if (invoice == null) {
            throw new InvalidPaymentException("Invoice cannot be null");
        }

        // Check invoice status - can only apply payments to Sent invoices
        if (invoice.getStatus() != InvoiceStatus.Sent) {
            throw new InvalidPaymentException(
                String.format("Cannot apply payment to %s invoice. Only Sent invoices can receive payments.",
                    invoice.getStatus())
            );
        }

        // Check payment amount doesn't exceed balance
        if (payment.getAmount().compareTo(invoice.getBalance()) > 0) {
            throw new InvalidPaymentException(
                String.format("Payment amount ($%s) exceeds invoice balance ($%s)",
                    payment.getAmount(), invoice.getBalance())
            );
        }
    }

    /**
     * Calculates the new invoice balance after applying a payment
     *
     * @param invoice The invoice
     * @param paymentAmount The payment amount to apply
     * @return The new balance (invoice balance - payment amount)
     */
    public BigDecimal calculateNewBalance(Invoice invoice, BigDecimal paymentAmount) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }
        if (paymentAmount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null");
        }

        return invoice.getBalance().subtract(paymentAmount);
    }

    /**
     * Determines if an invoice should be marked as Paid based on the balance
     * Uses a small rounding tolerance (0.01) to handle floating point precision issues
     *
     * @param newBalance The balance to check
     * @return true if balance is zero or within rounding tolerance
     */
    public boolean shouldMarkInvoiceAsPaid(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new IllegalArgumentException("Balance cannot be null");
        }

        // Invoice is considered paid if balance is zero or very close to zero
        return newBalance.compareTo(BigDecimal.ZERO) == 0 ||
               newBalance.abs().compareTo(ROUNDING_TOLERANCE) < 0;
    }
}
