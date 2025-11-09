package com.invoiceme.common.exceptions;

import com.invoiceme.common.dto.ApiErrorResponse.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown for domain validation errors with field-level details.
 */
public class ValidationException extends DomainException {
    private static final String ERROR_CODE = "VALIDATION_ERROR";
    private final List<FieldError> fieldErrors;

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(message, ERROR_CODE);
        this.fieldErrors = new ArrayList<>(fieldErrors);
    }

    public ValidationException(String field, String message) {
        super(message, ERROR_CODE);
        this.fieldErrors = List.of(new FieldError(field, message, null));
    }

    public ValidationException(String field, String message, Object rejectedValue) {
        super(message, ERROR_CODE);
        this.fieldErrors = List.of(new FieldError(field, message, rejectedValue));
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
        this.fieldErrors = new ArrayList<>();
    }

    public List<FieldError> getFieldErrors() {
        return new ArrayList<>(fieldErrors);
    }
}
