package com.invoiceme.common.exceptions;

import com.invoiceme.common.dto.ApiErrorResponse;
import com.invoiceme.common.dto.ApiErrorResponse.FieldError;
import com.invoiceme.common.filters.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST API endpoints.
 * Provides consistent error responses with correlation IDs for tracking.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        BindingResult bindingResult = ex.getBindingResult();

        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = ApiErrorResponse.ofValidation(
                request.getRequestURI(),
                correlationId,
                fieldErrors
        );

        logger.warn("Validation error: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();

        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldError(
                        getFieldName(violation),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = ApiErrorResponse.ofValidation(
                request.getRequestURI(),
                correlationId,
                fieldErrors
        );

        logger.warn("Constraint violation: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles malformed JSON in request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Malformed request body: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles type mismatch errors in path variables or request parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        String message = String.format("Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Type mismatch error: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles entity not found exceptions.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Entity not found: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles business rule violation exceptions.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Business rule violation: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles invalid state transition exceptions.
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStateTransition(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Invalid state transition: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles insufficient balance exceptions.
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Insufficient balance: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles duplicate entity exceptions.
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEntity(
            DuplicateEntityException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Duplicate entity: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles domain validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainValidation(
            ValidationException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.ofValidation(
                request.getRequestURI(),
                correlationId,
                ex.getFieldErrors()
        );

        logger.warn("Domain validation error: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles optimistic locking failures.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.CONFLICT,
                "Resource was modified by another user. Please refresh and try again.",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Optimistic lock failure: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Authentication failed - CorrelationId: {}", correlationId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles bad credentials exceptions.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Bad credentials - CorrelationId: {}", correlationId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles username not found exceptions.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Username not found - CorrelationId: {}", correlationId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Access denied - CorrelationId: {}", correlationId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles domain-specific CustomerNotFoundException (from existing code).
     */
    @ExceptionHandler(com.invoiceme.customer.domain.CustomerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerNotFoundException(
            com.invoiceme.customer.domain.CustomerNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Customer not found: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles domain-specific InvoiceNotFoundException (from existing code).
     */
    @ExceptionHandler(com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceNotFoundException(
            com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Invoice not found: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles domain-specific InvalidPaymentException (from existing code).
     */
    @ExceptionHandler(com.invoiceme.payment.domain.InvalidPaymentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentException(
            com.invoiceme.payment.domain.InvalidPaymentException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Invalid payment: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles domain-specific InvoiceNotSentException (from existing code).
     */
    @ExceptionHandler(com.invoiceme.payment.exceptions.InvoiceNotSentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceNotSentException(
            com.invoiceme.payment.exceptions.InvoiceNotSentException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Invoice not sent: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles domain-specific PaymentExceedsBalanceException (from existing code).
     */
    @ExceptionHandler(com.invoiceme.payment.exceptions.PaymentExceedsBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentExceedsBalanceException(
            com.invoiceme.payment.exceptions.PaymentExceedsBalanceException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        logger.warn("Payment exceeds balance: {} - CorrelationId: {}", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles PDF generation exceptions.
     */
    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handlePdfGenerationException(
            PdfGenerationException ex,
            HttpServletRequest request) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate PDF. Please try again later.",
                request.getRequestURI(),
                correlationId
        );

        logger.error("PDF generation failed: {} - CorrelationId: {}", ex.getMessage(), correlationId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles all other exceptions as internal server errors.
     *
     * NOTE: Commented out to allow Spring Security's default exception handling to work.
     * Spring Security needs to handle AuthenticationException to return 401 for login redirects.
     * Enabling this catch-all breaks authentication by intercepting 401 responses.
     */
    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<ApiErrorResponse> handleGenericException(
    //         Exception ex,
    //         HttpServletRequest request) {
    //
    //     String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
    //     ApiErrorResponse errorResponse = ApiErrorResponse.of(
    //             HttpStatus.INTERNAL_SERVER_ERROR,
    //             "An unexpected error occurred. Please try again later.",
    //             request.getRequestURI(),
    //             correlationId
    //     );
    //
    //     logger.error("Unexpected error - CorrelationId: {}", correlationId, ex);
    //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    // }

    /**
     * Extracts field name from constraint violation.
     */
    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }
}
