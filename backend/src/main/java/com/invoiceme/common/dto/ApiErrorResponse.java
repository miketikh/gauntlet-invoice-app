package com.invoiceme.common.dto;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Standard error response format for all API errors.
 * Provides consistent structure for error handling across the application.
 */
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldError> fieldErrors
) {
    /**
     * Represents a field-level validation error.
     */
    public record FieldError(
            String field,
            String message,
            Object rejectedValue
    ) {}

    /**
     * Creates an ApiErrorResponse for a general error.
     *
     * @param status The HTTP status
     * @param message The error message
     * @param path The request path
     * @param correlationId The correlation ID for tracing
     * @return ApiErrorResponse instance
     */
    public static ApiErrorResponse of(HttpStatus status, String message, String path, String correlationId) {
        return new ApiErrorResponse(
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                correlationId,
                null
        );
    }

    /**
     * Creates an ApiErrorResponse for validation errors with field-level details.
     *
     * @param path The request path
     * @param correlationId The correlation ID for tracing
     * @param fieldErrors List of field errors
     * @return ApiErrorResponse instance
     */
    public static ApiErrorResponse ofValidation(String path, String correlationId, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                path,
                correlationId,
                fieldErrors
        );
    }

    /**
     * Creates an ApiErrorResponse with custom error details.
     *
     * @param status The HTTP status
     * @param error The error type
     * @param message The error message
     * @param path The request path
     * @param correlationId The correlation ID for tracing
     * @param fieldErrors Optional field errors
     * @return ApiErrorResponse instance
     */
    public static ApiErrorResponse of(HttpStatus status, String error, String message, String path, String correlationId, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status.value(),
                error,
                message,
                path,
                correlationId,
                fieldErrors
        );
    }
}
