package com.invoiceme.invoice.domain.exceptions;

/**
 * InvalidInvoiceStateException
 * Thrown when attempting an invalid state transition or operation for the current invoice state
 */
public class InvalidInvoiceStateException extends RuntimeException {

    public InvalidInvoiceStateException(String message) {
        super(message);
    }
}
