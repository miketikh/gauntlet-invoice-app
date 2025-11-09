package com.invoiceme.invoice.domain.exceptions;

/**
 * Exception thrown when invoice validation fails
 */
public class InvoiceValidationException extends RuntimeException {

    public InvoiceValidationException(String message) {
        super(message);
    }

    public InvoiceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
