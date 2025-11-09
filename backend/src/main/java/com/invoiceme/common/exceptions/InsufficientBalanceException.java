package com.invoiceme.common.exceptions;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Exception thrown when payment amount exceeds invoice balance.
 */
public class InsufficientBalanceException extends DomainException {
    private static final String ERROR_CODE = "INSUFFICIENT_BALANCE";

    public InsufficientBalanceException(BigDecimal balance, BigDecimal attemptedPayment) {
        super(
                String.format("Payment amount %s exceeds invoice balance %s", attemptedPayment, balance),
                ERROR_CODE,
                Map.of("balance", balance, "attemptedPayment", attemptedPayment)
        );
    }

    public InsufficientBalanceException(String message) {
        super(message, ERROR_CODE);
    }

    public InsufficientBalanceException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
    }
}
