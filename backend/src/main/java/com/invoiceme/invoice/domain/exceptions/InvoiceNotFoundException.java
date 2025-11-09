package com.invoiceme.invoice.domain.exceptions;

import java.util.UUID;

/**
 * Exception thrown when an invoice is not found
 */
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Invoice not found: " + invoiceId);
    }

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
