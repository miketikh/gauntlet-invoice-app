package com.invoiceme.common.exceptions;

import java.util.Map;

/**
 * Exception thrown when an entity is not found by ID.
 */
public class EntityNotFoundException extends DomainException {
    private static final String ERROR_CODE = "ENTITY_NOT_FOUND";

    public EntityNotFoundException(String entityType, String entityId) {
        super(
                String.format("%s not found with ID: %s", entityType, entityId),
                ERROR_CODE,
                Map.of("entityType", entityType, "entityId", entityId)
        );
    }

    public EntityNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public EntityNotFoundException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
    }
}
