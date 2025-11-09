package com.invoiceme.payment.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Payment entity
 * Tests validation logic and business rules
 */
@DisplayName("Payment Entity Tests")
class PaymentTest {

    private UUID invoiceId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String createdBy;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        paymentDate = LocalDate.now();
        amount = new BigDecimal("100.00");
        paymentMethod = PaymentMethod.CREDIT_CARD;
        createdBy = "user@example.com";
    }

    @Test
    @DisplayName("Should create payment with valid inputs")
    void shouldCreatePaymentWithValidInputs() {
        // Act
        Payment payment = Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Test payment",
            createdBy
        );

        // Assert
        assertThat(payment).isNotNull();
        assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(payment.getPaymentDate()).isEqualTo(paymentDate);
        assertThat(payment.getAmount()).isEqualByComparingTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(payment.getReference()).isEqualTo("REF-123");
        assertThat(payment.getNotes()).isEqualTo("Test payment");
        assertThat(payment.getCreatedBy()).isEqualTo(createdBy);
    }

    @Test
    @DisplayName("Should create payment with null reference and notes")
    void shouldCreatePaymentWithNullOptionalFields() {
        // Act
        Payment payment = Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            null,
            null,
            createdBy
        );

        // Assert
        assertThat(payment).isNotNull();
        assertThat(payment.getReference()).isNull();
        assertThat(payment.getNotes()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when invoice ID is null")
    void shouldThrowExceptionWhenInvoiceIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            null,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Invoice ID is required");
    }

    @Test
    @DisplayName("Should throw exception when payment date is null")
    void shouldThrowExceptionWhenPaymentDateIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            null,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment date is required");
    }

    @Test
    @DisplayName("Should throw exception when payment date is in future")
    void shouldThrowExceptionWhenPaymentDateIsInFuture() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            futureDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment date cannot be in the future");
    }

    @Test
    @DisplayName("Should allow payment date from the past")
    void shouldAllowPaymentDateFromPast() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(10);

        // Act
        Payment payment = Payment.createPayment(
            invoiceId,
            pastDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        );

        // Assert
        assertThat(payment).isNotNull();
        assertThat(payment.getPaymentDate()).isEqualTo(pastDate);
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            null,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment amount is required");
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            BigDecimal.ZERO,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment amount must be positive");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Arrange
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            negativeAmount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment amount must be positive");
    }

    @Test
    @DisplayName("Should throw exception when payment method is null")
    void shouldThrowExceptionWhenPaymentMethodIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            null,
            "REF-123",
            "Note",
            createdBy
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Payment method is required");
    }

    @Test
    @DisplayName("Should throw exception when createdBy is null")
    void shouldThrowExceptionWhenCreatedByIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            null
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Created by is required");
    }

    @Test
    @DisplayName("Should throw exception when createdBy is blank")
    void shouldThrowExceptionWhenCreatedByIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            "   "
        ))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Created by is required");
    }

    @Test
    @DisplayName("Should support all payment methods")
    void shouldSupportAllPaymentMethods() {
        // Test each payment method
        for (PaymentMethod method : PaymentMethod.values()) {
            Payment payment = Payment.createPayment(
                invoiceId,
                paymentDate,
                amount,
                method,
                "REF-" + method.name(),
                "Test " + method.getDisplayName(),
                createdBy
            );

            assertThat(payment).isNotNull();
            assertThat(payment.getPaymentMethod()).isEqualTo(method);
        }
    }

    @Test
    @DisplayName("Should have proper equals implementation based on id")
    void shouldHaveProperEqualsImplementation() {
        // Arrange
        Payment payment1 = Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        );

        Payment payment2 = Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Note",
            createdBy
        );

        // Assert
        assertThat(payment1).isNotEqualTo(payment2); // Different IDs (not persisted yet)
        assertThat(payment1).isEqualTo(payment1); // Same instance
    }

    @Test
    @DisplayName("Should have proper toString implementation")
    void shouldHaveProperToString() {
        // Arrange
        Payment payment = Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            "REF-123",
            "Test payment",
            createdBy
        );

        // Act
        String toString = payment.toString();

        // Assert
        assertThat(toString).contains("Payment{");
        assertThat(toString).contains("invoiceId=" + invoiceId);
        assertThat(toString).contains("amount=" + amount);
        assertThat(toString).contains("paymentMethod=" + paymentMethod);
    }
}
