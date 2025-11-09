package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendInvoiceCommandHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SendInvoiceCommandHandler handler;

    private UUID invoiceId;
    private UUID customerId;
    private Customer customer;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getEmail()).thenReturn("test@example.com");

        // Create a draft invoice with line items
        invoice = Invoice.create(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0001"
        );

        LineItem lineItem = new LineItem(
            null,
            "Test Service",
            2,
            new BigDecimal("100.00"),
            new BigDecimal("0.1"),
            new BigDecimal("0.08")
        );
        invoice.addLineItem(lineItem);
    }

    @Test
    void shouldSendDraftInvoiceSuccessfully() {
        // Arrange
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponseDTO response = handler.handle(command);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(InvoiceStatus.Sent);
        assertThat(response.id()).isEqualTo(invoice.getId());
        assertThat(response.customerName()).isEqualTo("Test Customer");

        verify(invoiceRepository).save(any(Invoice.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceNotFoundException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSendingAlreadySentInvoice() {
        // Arrange
        invoice.markAsSent();  // Already sent

        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Cannot send invoice in Sent status");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSendingInvoiceWithoutLineItems() {
        // Arrange
        // Create invoice without line items
        Invoice emptyInvoice = Invoice.create(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0002"
        );

        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(emptyInvoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Cannot send invoice without line items");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Customer not found");

        verify(invoiceRepository, never()).save(any());
    }
}
