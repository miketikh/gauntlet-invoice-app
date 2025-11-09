package com.invoiceme.invoice.domain.exceptions;

/**
 * InvoiceImmutableException
 * Thrown when attempting to modify an invoice that is locked from editing
 */
public class InvoiceImmutableException extends RuntimeException {

    public InvoiceImmutableException(String message) {
        super(message);
    }
}
