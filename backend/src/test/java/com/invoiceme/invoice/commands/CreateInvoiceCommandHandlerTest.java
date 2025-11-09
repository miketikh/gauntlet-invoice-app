package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInvoiceCommandHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateInvoiceCommandHandler handler;

    private UUID customerId;
    private Customer customer;
    private LineItemDTO lineItemDTO;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getEmail()).thenReturn("test@example.com");

        lineItemDTO = new LineItemDTO(
            "Test Service",
            2,
            new BigDecimal("100.00"),
            new BigDecimal("0.1"),
            new BigDecimal("0.08")
        );
    }

    @Test
    void shouldCreateInvoiceSuccessfully() {
        // Arrange
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(30);

        CreateInvoiceCommand command = new CreateInvoiceCommand(
            customerId,
            issueDate,
            dueDate,
            "Net 30",
            List.of(lineItemDTO),
            "Test notes"
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceNumberGenerator.generateNextInvoiceNumber()).thenReturn("INV-2024-0001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponseDTO response = handler.handle(command);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.customerName()).isEqualTo("Test Customer");
        assertThat(response.status()).isEqualTo(InvoiceStatus.Draft);
        assertThat(response.invoiceNumber()).isEqualTo("INV-2024-0001");
        assertThat(response.issueDate()).isEqualTo(issueDate);
        assertThat(response.dueDate()).isEqualTo(dueDate);
        assertThat(response.paymentTerms()).isEqualTo("Net 30");
        assertThat(response.notes()).isEqualTo("Test notes");
        assertThat(response.lineItems()).hasSize(1);

        verify(invoiceRepository).save(any(Invoice.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(lineItemDTO),
            null
        );

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
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            customerId,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(),
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("At least one line item is required");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDueDateBeforeIssueDate() {
        // Arrange
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.minusDays(1);

        CreateInvoiceCommand command = new CreateInvoiceCommand(
            customerId,
            issueDate,
            dueDate,
            "Net 30",
            List.of(lineItemDTO),
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvoiceValidationException.class)
            .hasMessageContaining("Due date must be on or after issue date");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldUsIssueDateAsDueDateWhenDueDateNotProvided() {
        // Arrange
        LocalDate issueDate = LocalDate.now();

        CreateInvoiceCommand command = new CreateInvoiceCommand(
            customerId,
            issueDate,
            null,  // No due date provided
            "Due on receipt",
            List.of(lineItemDTO),
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceNumberGenerator.generateNextInvoiceNumber()).thenReturn("INV-2024-0002");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponseDTO response = handler.handle(command);

        // Assert
        assertThat(response.dueDate()).isEqualTo(issueDate);
    }
}
