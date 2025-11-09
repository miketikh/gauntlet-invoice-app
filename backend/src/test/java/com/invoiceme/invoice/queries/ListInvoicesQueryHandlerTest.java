package com.invoiceme.invoice.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import com.invoiceme.invoice.queries.dto.InvoiceListItemDTO;
import com.invoiceme.invoice.queries.dto.PagedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListInvoicesQueryHandlerTest {

    @Mock
    private JpaInvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ListInvoicesQueryHandler handler;

    private Customer testCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        testCustomer = Customer.create(
            "Acme Corp",
            "contact@acme.com",
            "555-1234",
            null
        );
    }

    @Test
    void shouldFilterByCustomer() {
        // Arrange
        Invoice invoice1 = createTestInvoice("INV-001", InvoiceStatus.Draft);
        Invoice invoice2 = createTestInvoice("INV-002", InvoiceStatus.Sent);

        Page<Invoice> page = new PageImpl<>(Arrays.asList(invoice1, invoice2));
        when(invoiceRepository.findWithFilters(eq(customerId), any(), any(), any(), any()))
            .thenReturn(page);
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(customerId, null, null, null);
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).customerName()).isEqualTo("Acme Corp");
    }

    @Test
    void shouldFilterByStatus() {
        // Arrange
        Invoice invoice = createTestInvoice("INV-001", InvoiceStatus.Sent);
        Page<Invoice> page = new PageImpl<>(List.of(invoice));

        when(invoiceRepository.findWithFilters(any(), eq(InvoiceStatus.Sent), any(), any(), any()))
            .thenReturn(page);
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(null, InvoiceStatus.Sent, null, null);
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    void shouldFilterByDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        Invoice invoice = createTestInvoice("INV-001", InvoiceStatus.Draft);
        Page<Invoice> page = new PageImpl<>(List.of(invoice));

        when(invoiceRepository.findWithFilters(any(), any(), eq(startDate), eq(endDate), any()))
            .thenReturn(page);
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(null, null, startDate, endDate);
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void shouldHandlePagination() {
        // Arrange
        Invoice invoice1 = createTestInvoice("INV-001", InvoiceStatus.Draft);
        Invoice invoice2 = createTestInvoice("INV-002", InvoiceStatus.Sent);

        Page<Invoice> page = new PageImpl<>(
            Arrays.asList(invoice1, invoice2),
            Pageable.ofSize(10).withPage(0),
            25 // total elements
        );

        when(invoiceRepository.findWithFilters(any(), any(), any(), any(), any()))
            .thenReturn(page);
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(null, null, null, null, 0, 10, "issueDate", "DESC");
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldReturnEmptyWhenNoResults() {
        // Arrange
        Page<Invoice> emptyPage = new PageImpl<>(List.of());
        when(invoiceRepository.findWithFilters(any(), any(), any(), any(), any()))
            .thenReturn(emptyPage);

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(null, null, null, null);
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void shouldApplyCombinedFilters() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        Invoice invoice = createTestInvoice("INV-001", InvoiceStatus.Sent);
        Page<Invoice> page = new PageImpl<>(List.of(invoice));

        when(invoiceRepository.findWithFilters(
            eq(customerId),
            eq(InvoiceStatus.Sent),
            eq(startDate),
            eq(endDate),
            any()
        )).thenReturn(page);
        when(customerRepository.findById(any())).thenReturn(Optional.of(testCustomer));

        // Act
        ListInvoicesQuery query = new ListInvoicesQuery(
            customerId,
            InvoiceStatus.Sent,
            startDate,
            endDate,
            0,
            20,
            "issueDate",
            "DESC"
        );
        PagedResult<InvoiceListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    void shouldThrowExceptionWhenPageSizeExceedsLimit() {
        // Arrange & Act & Assert
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
            new ListInvoicesQuery(null, null, null, null, 0, 101, "issueDate", "DESC")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size cannot exceed 100");
    }

    private Invoice createTestInvoice(String invoiceNumber, InvoiceStatus status) {
        Invoice invoice = Invoice.create(
            customerId,
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 15),
            "Net 30",
            invoiceNumber
        );

        LineItem item = new LineItem(
            null,
            "Test Service",
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        invoice.addLineItem(item);

        if (status == InvoiceStatus.Sent || status == InvoiceStatus.Paid) {
            invoice.markAsSent();
        }
        if (status == InvoiceStatus.Paid) {
            invoice.applyPayment(new BigDecimal("100.00"));
        }

        return invoice;
    }
}
