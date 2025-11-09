package com.invoiceme.config;

import com.invoiceme.customer.domain.CustomerNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for REST controllers
 * Converts exceptions to appropriate HTTP responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles CustomerNotFoundException
     * Returns 404 Not Found
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFoundException(CustomerNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problemDetail.setTitle("Customer Not Found");
        problemDetail.setProperty("customerId", ex.getCustomerId());
        return problemDetail;
    }

    /**
     * Handles InvoiceNotFoundException
     * Returns 404 Not Found
     */
    @ExceptionHandler(InvoiceNotFoundException.class)
    public ProblemDetail handleInvoiceNotFoundException(InvoiceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problemDetail.setTitle("Invoice Not Found");
        return problemDetail;
    }

    /**
     * Handles IllegalArgumentException (validation errors)
     * Returns 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Invalid Request");
        return problemDetail;
    }

    /**
     * Handles MethodArgumentTypeMismatchException (e.g., invalid UUID format)
     * Returns 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            message
        );
        problemDetail.setTitle("Invalid Parameter");
        return problemDetail;
    }

    /**
     * Handles generic exceptions
     * Returns 500 Internal Server Error
     * Note: Don't catch all exceptions - let Spring's default handlers work for auth/validation
     */
    // Commented out to let Spring's default exception handling work for other cases
    // @ExceptionHandler(Exception.class)
    // public ProblemDetail handleGenericException(Exception ex) {
    //     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
    //         HttpStatus.INTERNAL_SERVER_ERROR,
    //         "An unexpected error occurred"
    //     );
    //     problemDetail.setTitle("Internal Server Error");
    //     // Don't expose internal exception details in production
    //     return problemDetail;
    // }
}
