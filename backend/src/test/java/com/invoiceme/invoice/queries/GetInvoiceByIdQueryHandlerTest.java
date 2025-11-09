package com.invoiceme.invoice.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInvoiceByIdQueryHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private GetInvoiceByIdQueryHandler handler;

    private Invoice testInvoice;
    private Customer testCustomer;
    private UUID invoiceId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        // Create test customer
        testCustomer = Customer.create(
            "Acme Corp",
            "contact@acme.com",
            "555-1234",
            null
        );

        // Create test invoice
        testInvoice = Invoice.create(
            customerId,
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 15),
            "Net 30",
            "INV-2024-0001"
        );

        // Add line items
        LineItem item1 = new LineItem(
            null,
            "Consulting Services",
            10,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"), // 10% discount
            new BigDecimal("0.08")  // 8% tax
        );
        testInvoice.addLineItem(item1);
    }

    @Test
    void shouldGetInvoiceWithAllDetails() {
        // Arrange
        when(invoiceRepository.findById(any())).thenReturn(Optional.of(testInvoice));
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);
        InvoiceResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testInvoice.getId());
        assertThat(result.invoiceNumber()).isEqualTo("INV-2024-0001");
        assertThat(result.customerName()).isEqualTo("Acme Corp");
        assertThat(result.customerEmail()).isEqualTo("contact@acme.com");
        assertThat(result.status()).isEqualTo(InvoiceStatus.Draft);
        assertThat(result.subtotal()).isNotNull();
        assertThat(result.totalAmount()).isNotNull();
        assertThat(result.lineItems()).isNotNull();
        assertThat(result.lineItems()).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(UUID.randomUUID());
        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void shouldIncludeCustomerDataInResponse() {
        // Arrange
        when(invoiceRepository.findById(any())).thenReturn(Optional.of(testInvoice));
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);
        InvoiceResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result.customerName()).isEqualTo("Acme Corp");
        assertThat(result.customerEmail()).isEqualTo("contact@acme.com");
        assertThat(result.customerId()).isEqualTo(testInvoice.getCustomerId());
    }

    @Test
    void shouldCalculateDaysOverdueForOverdueInvoice() {
        // Arrange - create overdue invoice
        Invoice overdueInvoice = Invoice.create(
            customerId,
            LocalDate.now().minusDays(60),
            LocalDate.now().minusDays(30), // Due 30 days ago
            "Net 30",
            "INV-2024-0002"
        );
        LineItem item = new LineItem(
            null,
            "Test Service",
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        overdueInvoice.addLineItem(item);
        overdueInvoice.markAsSent(); // Must be sent to be overdue

        when(invoiceRepository.findById(any())).thenReturn(Optional.of(overdueInvoice));
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        InvoiceResponseDTO result = handler.handle(new GetInvoiceByIdQuery(invoiceId));

        // Assert
        assertThat(result.daysOverdue()).isNotNull();
        assertThat(result.daysOverdue()).isGreaterThan(0);
        assertThat(result.daysOverdue()).isEqualTo(30); // Approximately 30 days overdue
    }

    @Test
    void shouldReturnNullDaysOverdueForPaidInvoice() {
        // Arrange - create paid invoice
        Invoice paidInvoice = Invoice.create(
            customerId,
            LocalDate.now().minusDays(60),
            LocalDate.now().minusDays(30),
            "Net 30",
            "INV-2024-0003"
        );
        LineItem item = new LineItem(
            null,
            "Test Service",
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        paidInvoice.addLineItem(item);
        paidInvoice.markAsSent();
        paidInvoice.applyPayment(new BigDecimal("100.00")); // Pays in full, status becomes Paid

        when(invoiceRepository.findById(any())).thenReturn(Optional.of(paidInvoice));
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        InvoiceResponseDTO result = handler.handle(new GetInvoiceByIdQuery(invoiceId));

        // Assert
        assertThat(result.daysOverdue()).isNull();
    }

    @Test
    void shouldReturnNullDaysOverdueForNonOverdueInvoice() {
        // Arrange - create invoice not yet overdue
        Invoice futureInvoice = Invoice.create(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30), // Due in future
            "Net 30",
            "INV-2024-0004"
        );
        LineItem item = new LineItem(
            null,
            "Test Service",
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        futureInvoice.addLineItem(item);
        futureInvoice.markAsSent();

        when(invoiceRepository.findById(any())).thenReturn(Optional.of(futureInvoice));
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        InvoiceResponseDTO result = handler.handle(new GetInvoiceByIdQuery(invoiceId));

        // Assert
        assertThat(result.daysOverdue()).isNull();
    }
}
