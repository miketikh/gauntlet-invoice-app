package com.invoiceme.config;

import com.invoiceme.customer.domain.CustomerNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.payment.domain.InvalidPaymentException;
import com.invoiceme.payment.exceptions.InvoiceNotSentException;
import com.invoiceme.payment.exceptions.PaymentExceedsBalanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 * Converts exceptions to appropriate HTTP responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
     * Handles MethodArgumentNotValidException (Bean Validation errors)
     * Returns 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    /**
     * Handles InvoiceNotSentException
     * Returns 400 Bad Request
     */
    @ExceptionHandler(InvoiceNotSentException.class)
    public ProblemDetail handleInvoiceNotSentException(InvoiceNotSentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Invalid Invoice Status");
        problemDetail.setProperty("invoiceId", ex.getInvoiceId());
        problemDetail.setProperty("currentStatus", ex.getCurrentStatus());
        return problemDetail;
    }

    /**
     * Handles PaymentExceedsBalanceException
     * Returns 400 Bad Request
     */
    @ExceptionHandler(PaymentExceedsBalanceException.class)
    public ProblemDetail handlePaymentExceedsBalanceException(PaymentExceedsBalanceException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Payment Amount Invalid");
        problemDetail.setProperty("invoiceId", ex.getInvoiceId());
        problemDetail.setProperty("paymentAmount", ex.getPaymentAmount());
        problemDetail.setProperty("invoiceBalance", ex.getBalance());
        return problemDetail;
    }

    /**
     * Handles InvalidPaymentException (domain validation errors)
     * Returns 400 Bad Request
     */
    @ExceptionHandler(InvalidPaymentException.class)
    public ProblemDetail handleInvalidPaymentException(InvalidPaymentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Invalid Payment");
        return problemDetail;
    }

    /**
     * Handles all unhandled exceptions
     * Returns 500 Internal Server Error
     * This prevents Spring's default error page redirect that loses auth context
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        // Log the full exception with stack trace
        log.error("Unhandled exception in REST controller: {} - {}",
            ex.getClass().getName(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred while processing your request. Please try again."
        );
        problemDetail.setTitle("Internal Server Error");

        // Include exception details in development (never in production)
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // TODO: Add environment check
        // if (isDevelopmentEnvironment()) {
        //     problemDetail.setProperty("exceptionClass", ex.getClass().getName());
        //     problemDetail.setProperty("exceptionMessage", ex.getMessage());
        // }

        return problemDetail;
    }
}
