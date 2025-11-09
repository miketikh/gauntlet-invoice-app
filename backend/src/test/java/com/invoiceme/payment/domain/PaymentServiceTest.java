package com.invoiceme.payment.domain;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaymentService
 * Tests payment-invoice reconciliation business logic
 */
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    private PaymentService paymentService;
    private UUID invoiceId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        invoiceId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    // Helper method to create a test invoice
    private Invoice createTestInvoice(InvoiceStatus status, BigDecimal balance) {
        Invoice invoice = Invoice.create(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-001"
        );

        // If we need a balance, we need to add line items and potentially mark as sent
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            // Add line items to create balance
            invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
                null,
                "Test Item",
                1,
                balance,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ));
        }

        // Mark as sent if needed
        if (status == InvoiceStatus.Sent && !invoice.getLineItems().isEmpty()) {
            invoice.markAsSent();
        }

        return invoice;
    }

    // Helper method to create a test payment
    private Payment createTestPayment(BigDecimal amount) {
        return Payment.createPayment(
            invoiceId,
            LocalDate.now(),
            amount,
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            "Test payment",
            "user@example.com"
        );
    }

    @Test
    @DisplayName("Should validate payment against sent invoice successfully")
    void shouldValidatePaymentAgainstSentInvoice() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("500.00"));
        Payment payment = createTestPayment(new BigDecimal("200.00"));

        // Act & Assert - should not throw exception
        assertThatCode(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when payment is null")
    void shouldThrowExceptionWhenPaymentIsNull() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("500.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentAgainstInvoice(null, invoice))
            .isInstanceOf(InvalidPaymentException.class)
            .hasMessageContaining("Payment cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when invoice is null")
    void shouldThrowExceptionWhenInvoiceIsNull() {
        // Arrange
        Payment payment = createTestPayment(new BigDecimal("100.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentAgainstInvoice(payment, null))
            .isInstanceOf(InvalidPaymentException.class)
            .hasMessageContaining("Invoice cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when applying payment to draft invoice")
    void shouldThrowExceptionWhenPaymentAppliedToDraftInvoice() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Draft, new BigDecimal("500.00"));
        Payment payment = createTestPayment(new BigDecimal("100.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .isInstanceOf(InvalidPaymentException.class)
            .hasMessageContaining("Cannot apply payment to Draft invoice");
    }

    @Test
    @DisplayName("Should throw exception when applying payment to paid invoice")
    void shouldThrowExceptionWhenPaymentAppliedToPaidInvoice() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("100.00"));
        invoice.applyPayment(new BigDecimal("100.00")); // This marks it as Paid
        Payment payment = createTestPayment(new BigDecimal("50.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .isInstanceOf(InvalidPaymentException.class)
            .hasMessageContaining("Cannot apply payment to Paid invoice");
    }

    @Test
    @DisplayName("Should throw exception when payment exceeds invoice balance")
    void shouldThrowExceptionWhenPaymentExceedsBalance() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("100.00"));
        Payment payment = createTestPayment(new BigDecimal("150.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .isInstanceOf(InvalidPaymentException.class)
            .hasMessageContaining("Payment amount ($150.00) exceeds invoice balance ($100.00)");
    }

    @Test
    @DisplayName("Should allow payment equal to invoice balance")
    void shouldAllowPaymentEqualToBalance() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("100.00"));
        Payment payment = createTestPayment(new BigDecimal("100.00"));

        // Act & Assert - should not throw exception
        assertThatCode(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow partial payment (amount less than balance)")
    void shouldAllowPartialPayment() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("500.00"));
        Payment payment = createTestPayment(new BigDecimal("200.00"));

        // Act & Assert - should not throw exception
        assertThatCode(() -> paymentService.validatePaymentAgainstInvoice(payment, invoice))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should calculate new balance correctly")
    void shouldCalculateNewBalanceCorrectly() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("500.00"));
        BigDecimal paymentAmount = new BigDecimal("200.00");

        // Act
        BigDecimal newBalance = paymentService.calculateNewBalance(invoice, paymentAmount);

        // Assert
        assertThat(newBalance).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Should calculate zero balance when payment equals total")
    void shouldCalculateZeroBalanceWhenPaymentEqualsTotal() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("100.00"));
        BigDecimal paymentAmount = new BigDecimal("100.00");

        // Act
        BigDecimal newBalance = paymentService.calculateNewBalance(invoice, paymentAmount);

        // Assert
        assertThat(newBalance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when calculating balance with null invoice")
    void shouldThrowExceptionWhenCalculatingBalanceWithNullInvoice() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("100.00");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.calculateNewBalance(null, paymentAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invoice cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when calculating balance with null payment amount")
    void shouldThrowExceptionWhenCalculatingBalanceWithNullAmount() {
        // Arrange
        Invoice invoice = createTestInvoice(InvoiceStatus.Sent, new BigDecimal("100.00"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.calculateNewBalance(invoice, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount cannot be null");
    }

    @Test
    @DisplayName("Should mark invoice as paid when balance is zero")
    void shouldMarkInvoiceAsPaidWhenBalanceIsZero() {
        // Arrange
        BigDecimal zeroBalance = BigDecimal.ZERO;

        // Act
        boolean shouldMarkPaid = paymentService.shouldMarkInvoiceAsPaid(zeroBalance);

        // Assert
        assertThat(shouldMarkPaid).isTrue();
    }

    @Test
    @DisplayName("Should mark invoice as paid when balance is within rounding tolerance")
    void shouldMarkInvoiceAsPaidWhenBalanceWithinTolerance() {
        // Arrange - 0.005 is within 0.01 tolerance
        BigDecimal tinyBalance = new BigDecimal("0.005");

        // Act
        boolean shouldMarkPaid = paymentService.shouldMarkInvoiceAsPaid(tinyBalance);

        // Assert
        assertThat(shouldMarkPaid).isTrue();
    }

    @Test
    @DisplayName("Should mark invoice as paid when balance is negative within tolerance")
    void shouldMarkInvoiceAsPaidWhenBalanceIsNegativeWithinTolerance() {
        // Arrange - -0.005 is within 0.01 tolerance (absolute value)
        BigDecimal tinyNegativeBalance = new BigDecimal("-0.005");

        // Act
        boolean shouldMarkPaid = paymentService.shouldMarkInvoiceAsPaid(tinyNegativeBalance);

        // Assert
        assertThat(shouldMarkPaid).isTrue();
    }

    @Test
    @DisplayName("Should not mark invoice as paid when balance exceeds tolerance")
    void shouldNotMarkInvoiceAsPaidWhenBalanceExceedsTolerance() {
        // Arrange - 0.02 exceeds 0.01 tolerance
        BigDecimal balance = new BigDecimal("0.02");

        // Act
        boolean shouldMarkPaid = paymentService.shouldMarkInvoiceAsPaid(balance);

        // Assert
        assertThat(shouldMarkPaid).isFalse();
    }

    @Test
    @DisplayName("Should not mark invoice as paid when balance is significant")
    void shouldNotMarkInvoiceAsPaidWhenBalanceIsSignificant() {
        // Arrange
        BigDecimal significantBalance = new BigDecimal("50.00");

        // Act
        boolean shouldMarkPaid = paymentService.shouldMarkInvoiceAsPaid(significantBalance);

        // Assert
        assertThat(shouldMarkPaid).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when checking paid status with null balance")
    void shouldThrowExceptionWhenCheckingPaidStatusWithNullBalance() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.shouldMarkInvoiceAsPaid(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Balance cannot be null");
    }
}
