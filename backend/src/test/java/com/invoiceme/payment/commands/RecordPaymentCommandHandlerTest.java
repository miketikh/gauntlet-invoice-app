package com.invoiceme.payment.commands;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentMethod;
import com.invoiceme.payment.domain.PaymentRepository;
import com.invoiceme.payment.exceptions.InvoiceNotSentException;
import com.invoiceme.payment.exceptions.PaymentExceedsBalanceException;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RecordPaymentCommandHandler
 * Tests full payment recording flow with database transactions
 */
@SpringBootTest
@Transactional
@DisplayName("RecordPaymentCommandHandler Integration Tests")
class RecordPaymentCommandHandlerTest {

    @Autowired
    private RecordPaymentCommandHandler handler;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Customer customer;
    private Invoice invoice;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user@example.com";

        // Create test customer
        customer = Customer.create(
            "Acme Corporation",
            "contact@acme.com",
            "555-1234",
            new Address(
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA"
            )
        );
        customer = customerRepository.save(customer);

        // Create test invoice with line items
        invoice = Invoice.create(
            customer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0001"
        );

        // Add line items
        LineItem lineItem = new LineItem(
            UUID.randomUUID().toString(),
            "Test Service",
            2,
            new BigDecimal("250.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        invoice.addLineItem(lineItem);

        // Mark as sent to allow payments
        invoice.markAsSent();
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("Should record payment and update invoice balance")
    void shouldRecordPaymentAndUpdateInvoiceBalance() {
        // Given: Invoice with balance of $500
        assertThat(invoice.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Sent);

        RecordPaymentCommand command = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("200.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            "Partial payment",
            null
        );

        // When: Record payment
        PaymentResponseDTO response = handler.handle(command, userId);

        // Then: Payment created and invoice balance updated
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.remainingBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.invoiceNumber()).isEqualTo("INV-2024-0001");
        assertThat(response.customerName()).isEqualTo("Acme Corporation");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(response.reference()).isEqualTo("REF-123");
        assertThat(response.createdBy()).isEqualTo(userId);

        // Verify invoice was updated
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.Sent);

        // Verify payment was saved
        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Should mark invoice as Paid when balance reaches zero")
    void shouldMarkInvoiceAsPaidWhenBalanceReachesZero() {
        // Given: Invoice with balance of $500
        RecordPaymentCommand command = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("500.00"),
            PaymentMethod.BANK_TRANSFER,
            "REF-456",
            "Full payment",
            null
        );

        // When: Record full payment
        PaymentResponseDTO response = handler.handle(command, userId);

        // Then: Invoice marked as Paid
        assertThat(response.remainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.Paid);
    }

    @Test
    @DisplayName("Should support partial payments with remaining balance")
    void shouldSupportPartialPaymentsWithRemainingBalance() {
        // Given: Invoice with balance of $500
        // When: Apply two partial payments
        RecordPaymentCommand payment1 = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("150.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-1",
            "First payment",
            null
        );
        PaymentResponseDTO response1 = handler.handle(payment1, userId);

        RecordPaymentCommand payment2 = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CASH,
            "REF-2",
            "Second payment",
            null
        );
        PaymentResponseDTO response2 = handler.handle(payment2, userId);

        // Then: Both payments recorded, balance reduced correctly
        assertThat(response1.remainingBalance()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(response2.remainingBalance()).isEqualByComparingTo(new BigDecimal("250.00"));

        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.Sent);

        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).hasSize(2);
    }

    @Test
    @DisplayName("Should throw exception when payment applied to Draft invoice")
    void shouldThrowExceptionWhenPaymentAppliedToDraftInvoice() {
        // Given: Create draft invoice (not sent)
        Invoice draftInvoice = Invoice.create(
            customer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0002"
        );
        LineItem lineItem = new LineItem(
            UUID.randomUUID().toString(),
            "Service",
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        draftInvoice.addLineItem(lineItem);
        draftInvoice = invoiceRepository.save(draftInvoice);

        RecordPaymentCommand command = new RecordPaymentCommand(
            draftInvoice.getId(),
            LocalDate.now(),
            new BigDecimal("50.00"),
            PaymentMethod.CASH,
            "REF-789",
            null,
            null
        );

        // When/Then: Recording payment should fail
        assertThatThrownBy(() -> handler.handle(command, userId))
            .isInstanceOf(InvoiceNotSentException.class)
            .hasMessageContaining("Cannot apply payment to Draft invoice");

        // Verify no payment was created
        List<Payment> payments = paymentRepository.findByInvoiceId(draftInvoice.getId());
        assertThat(payments).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when payment exceeds balance")
    void shouldThrowExceptionWhenPaymentExceedsBalance() {
        // Given: Invoice with balance of $500
        RecordPaymentCommand command = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("600.00"),  // Exceeds balance
            PaymentMethod.CHECK,
            "REF-999",
            null,
            null
        );

        // When/Then: Recording payment should fail
        assertThatThrownBy(() -> handler.handle(command, userId))
            .isInstanceOf(PaymentExceedsBalanceException.class)
            .hasMessageContaining("exceeds invoice balance");

        // Verify transaction rolled back (no payment, invoice unchanged)
        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).isEmpty();

        Invoice unchangedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(unchangedInvoice.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(unchangedInvoice.getStatus()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    @DisplayName("Should throw exception for non-existent invoice")
    void shouldThrowExceptionForNonExistentInvoice() {
        // Given: Non-existent invoice ID
        UUID nonExistentId = UUID.randomUUID();
        RecordPaymentCommand command = new RecordPaymentCommand(
            nonExistentId,
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-000",
            null,
            null
        );

        // When/Then: Should throw InvoiceNotFoundException
        assertThatThrownBy(() -> handler.handle(command, userId))
            .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    @DisplayName("Should prevent duplicate payment with idempotency key")
    void shouldPreventDuplicatePaymentWithIdempotencyKey() {
        // Given: Payment command with idempotency key
        String idempotencyKey = "payment-test-001-" + UUID.randomUUID();
        RecordPaymentCommand command = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-IDM",
            null,
            idempotencyKey
        );

        // When: Record payment twice with same idempotency key
        PaymentResponseDTO response1 = handler.handle(command, userId);
        PaymentResponseDTO response2 = handler.handle(command, userId);

        // Then: Only one payment created, same response returned
        assertThat(response1.id()).isEqualTo(response2.id());
        assertThat(response1.amount()).isEqualByComparingTo(response2.amount());

        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).hasSize(1);  // Only one payment despite two requests

        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    @DisplayName("Should handle payment to already Paid invoice gracefully")
    void shouldHandlePaymentToAlreadyPaidInvoice() {
        // Given: Invoice that has been fully paid
        RecordPaymentCommand fullPayment = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("500.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-FULL",
            "Full payment",
            null
        );
        handler.handle(fullPayment, userId);

        // Verify invoice is paid
        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.Paid);

        // When: Try to apply another payment
        RecordPaymentCommand extraPayment = new RecordPaymentCommand(
            invoice.getId(),
            LocalDate.now(),
            new BigDecimal("50.00"),
            PaymentMethod.CASH,
            "REF-EXTRA",
            null,
            null
        );

        // Then: Should throw exception
        assertThatThrownBy(() -> handler.handle(extraPayment, userId))
            .isInstanceOf(InvoiceNotSentException.class)
            .hasMessageContaining("Cannot apply payment to Paid invoice");
    }
}
