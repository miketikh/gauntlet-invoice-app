package com.invoiceme.common.exceptions;

import java.util.Map;

/**
 * Exception thrown when an invalid invoice state transition is attempted.
 */
public class InvalidStateTransitionException extends DomainException {
    private static final String ERROR_CODE = "INVALID_STATE_TRANSITION";

    public InvalidStateTransitionException(String currentState, String attemptedState) {
        super(
                String.format("Cannot transition from %s to %s", currentState, attemptedState),
                ERROR_CODE,
                Map.of("currentState", currentState, "attemptedState", attemptedState)
        );
    }

    public InvalidStateTransitionException(String message) {
        super(message, ERROR_CODE);
    }

    public InvalidStateTransitionException(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, details);
    }
}
