package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.*;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInvoiceCommandHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private UpdateInvoiceCommandHandler handler;

    private UUID invoiceId;
    private UUID customerId;
    private Customer customer;
    private Invoice invoice;
    private LineItemDTO lineItemDTO;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getEmail()).thenReturn("test@example.com");

        // Create a draft invoice
        invoice = Invoice.create(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0001"
        );

        lineItemDTO = new LineItemDTO(
            "Updated Service",
            3,
            new BigDecimal("150.00"),
            new BigDecimal("0.05"),
            new BigDecimal("0.08")
        );
    }

    @Test
    void shouldUpdateDraftInvoiceSuccessfully() {
        // Arrange
        LocalDate newIssueDate = LocalDate.now();
        LocalDate newDueDate = newIssueDate.plusDays(45);

        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            newIssueDate,
            newDueDate,
            "Net 45",
            List.of(lineItemDTO),
            "Updated notes",
            0L  // version
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponseDTO response = handler.handle(command);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(invoice.getId());
        assertThat(response.paymentTerms()).isEqualTo("Net 45");
        assertThat(response.notes()).isEqualTo("Updated notes");
        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.lineItems().get(0).description()).isEqualTo("Updated Service");

        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(lineItemDTO),
            null,
            0L
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceNotFoundException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingSentInvoice() {
        // Arrange
        // Add line item before sending
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Service", 1, new BigDecimal("100.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice.markAsSent();

        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(lineItemDTO),
            null,
            0L
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Cannot update invoice in Sent status");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenVersionMismatch() {
        // Arrange
        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(lineItemDTO),
            null,
            99L  // Wrong version
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(OptimisticLockException.class)
            .hasMessageContaining("modified by another transaction");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(lineItemDTO),
            null,
            0L
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Customer not found");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenLineItemsEmpty() {
        // Arrange
        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            invoiceId,
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(),  // Empty line items
            null,
            0L
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("At least one line item is required");

        verify(invoiceRepository, never()).save(any());
    }
}
