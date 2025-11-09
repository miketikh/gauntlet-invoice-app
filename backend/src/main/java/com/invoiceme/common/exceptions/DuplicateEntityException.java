package com.invoiceme.common.exceptions;

import java.util.Map;

/**
 * Exception thrown when a uniqueness constraint is violated.
 */
public class DuplicateEntityException extends DomainException {
    private static final String ERROR_CODE = "DUPLICATE_ENTITY";

    public DuplicateEntityException(String field, Object value) {
        super(
                String.format("Entity with %s '%s' already exists", field, value),
                ERROR_CODE,
                Map.of("field", field, "value", value)
        );
    }

    public DuplicateEntityException(String message) {
        super(message, ERROR_CODE);
    }

    public DuplicateEntityException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
    }
}
