package com.invoiceme.payment.domain.events;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentRecorded
 * Domain event published when a payment is successfully recorded against an invoice
 * Can be used for audit logging, notification triggers, analytics, or event sourcing
 *
 * This event captures the state after payment application, including:
 * - Payment details (ID, amount, date)
 * - Invoice state changes (new balance, potentially new status)
 * - Timestamp of when the event occurred
 */
public record PaymentRecorded(
    UUID paymentId,
    UUID invoiceId,
    BigDecimal amount,
    LocalDate paymentDate,
    BigDecimal newBalance,
    InvoiceStatus newStatus,
    LocalDateTime occurredAt
) {
    /**
     * Factory method to create PaymentRecorded event from Payment and Invoice entities
     *
     * @param payment The payment that was recorded
     * @param invoice The invoice after payment was applied
     * @return A new PaymentRecorded event
     */
    public static PaymentRecorded of(Payment payment, Invoice invoice) {
        return new PaymentRecorded(
            payment.getId(),
            payment.getInvoiceId(),
            payment.getAmount(),
            payment.getPaymentDate(),
            invoice.getBalance(),
            invoice.getStatus(),
            LocalDateTime.now()
        );
    }
}
