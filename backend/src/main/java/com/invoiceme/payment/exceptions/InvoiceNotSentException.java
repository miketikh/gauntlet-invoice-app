package com.invoiceme.payment.exceptions;

import com.invoiceme.invoice.domain.InvoiceStatus;

import java.util.UUID;

/**
 * InvoiceNotSentException
 * Thrown when attempting to apply a payment to an invoice that is not in Sent status
 * Business rule: Only Sent invoices can accept payments
 */
public class InvoiceNotSentException extends RuntimeException {

    private final UUID invoiceId;
    private final InvoiceStatus currentStatus;

    public InvoiceNotSentException(UUID invoiceId, InvoiceStatus currentStatus) {
        super(String.format("Cannot apply payment to %s invoice. Invoice must be in Sent status to accept payments. Invoice ID: %s",
            currentStatus, invoiceId));
        this.invoiceId = invoiceId;
        this.currentStatus = currentStatus;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public InvoiceStatus getCurrentStatus() {
        return currentStatus;
    }
}
