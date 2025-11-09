package com.invoiceme.common.exceptions;

import java.util.Map;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessRuleViolationException extends DomainException {
    private static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleViolationException(String message) {
        super(message, ERROR_CODE);
    }

    public BusinessRuleViolationException(String message, String ruleName) {
        super(
                message,
                ERROR_CODE,
                Map.of("rule", ruleName)
        );
    }

    public BusinessRuleViolationException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
    }
}
