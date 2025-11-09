package com.invoiceme.payment.domain;

/**
 * Exception thrown when payment validation fails or business rules are violated
 * Used for domain-specific payment validation errors
 */
public class InvalidPaymentException extends RuntimeException {

    public InvalidPaymentException(String message) {
        super(message);
    }

    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
