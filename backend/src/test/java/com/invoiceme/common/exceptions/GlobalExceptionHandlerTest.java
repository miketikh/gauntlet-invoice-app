package com.invoiceme.common.exceptions;

import com.invoiceme.common.dto.ApiErrorResponse;
import com.invoiceme.common.dto.ApiErrorResponse.FieldError;
import com.invoiceme.common.filters.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    private static final String TEST_CORRELATION_ID = "test-correlation-id";
    private static final String TEST_PATH = "/api/test";

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(TEST_PATH);
        MDC.put("correlationId", TEST_CORRELATION_ID);
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        org.springframework.validation.FieldError fieldError1 = new org.springframework.validation.FieldError("customer", "name", "rejected", false, null, null, "Name is required");
        org.springframework.validation.FieldError fieldError2 = new org.springframework.validation.FieldError("customer", "email", "invalid", false, null, null, "Email must be valid");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().correlationId()).isEqualTo(TEST_CORRELATION_ID);
        assertThat(response.getBody().fieldErrors()).hasSize(2);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("name");
        assertThat(response.getBody().fieldErrors().get(1).field()).isEqualTo("email");
    }

    @Test
    void shouldHandleEntityNotFoundException() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Customer", "12345");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleEntityNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("Customer not found");
        assertThat(response.getBody().correlationId()).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void shouldHandleBusinessRuleViolationException() {
        // Given
        BusinessRuleViolationException exception = new BusinessRuleViolationException("Cannot send invoice without line items");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusinessRuleViolation(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Cannot send invoice without line items");
        assertThat(response.getBody().correlationId()).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void shouldHandleInvalidStateTransitionException() {
        // Given
        InvalidStateTransitionException exception = new InvalidStateTransitionException("Draft", "Paid");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInvalidStateTransition(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Cannot transition from Draft to Paid");
    }

    @Test
    void shouldHandleInsufficientBalanceException() {
        // Given
        InsufficientBalanceException exception = new InsufficientBalanceException(
                new BigDecimal("100.00"),
                new BigDecimal("150.00")
        );

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInsufficientBalance(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("exceeds invoice balance");
    }

    @Test
    void shouldHandleDuplicateEntityException() {
        // Given
        DuplicateEntityException exception = new DuplicateEntityException("email", "test@example.com");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDuplicateEntity(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("already exists");
    }

    @Test
    void shouldHandleOptimisticLockingFailureException() {
        // Given
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException("Version conflict");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleOptimisticLock(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("modified by another user");
    }

    @Test
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException exception = mock(AuthenticationException.class);

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAuthentication(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Authentication failed");
    }

    @Test
    void shouldHandleBadCredentialsException() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Invalid password");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBadCredentials(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void shouldHandleUsernameNotFoundException() {
        // Given
        UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUsernameNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void shouldHandleAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAccessDenied(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void shouldHandleHttpMessageNotReadableException() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed request body");
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchException() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");
        when(exception.getRequiredType()).thenReturn((Class) Long.class);

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleTypeMismatch(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("must be of type");
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGenericException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void shouldIncludeCorrelationIdInAllResponses() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Customer", "123");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleEntityNotFound(exception, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().correlationId()).isEqualTo(TEST_CORRELATION_ID);
    }
}
