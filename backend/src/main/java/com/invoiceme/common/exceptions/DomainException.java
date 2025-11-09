package com.invoiceme.common.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all domain-level exceptions.
 * Provides error code and additional details for business rule violations.
 */
public class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public DomainException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public DomainException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}
