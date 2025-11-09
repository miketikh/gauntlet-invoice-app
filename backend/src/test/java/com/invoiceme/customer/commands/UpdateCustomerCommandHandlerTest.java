package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerCommandHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UpdateCustomerCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateCustomerCommandHandler(customerRepository, eventPublisher);
    }

    @Test
    void shouldUpdateCustomerName() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            "Jane Doe",
            null,
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("Jane Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());

        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(existingCustomer);
        verify(eventPublisher).publishEvent(any(CustomerUpdated.class));
    }

    @Test
    void shouldUpdateCustomerEmail() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            null,
            "newemail@example.com",
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailAndIdNot("newemail@example.com", customerId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("John Doe", result.getName());
        assertEquals("newemail@example.com", result.getEmail());

        verify(customerRepository).existsByEmailAndIdNot("newemail@example.com", customerId);
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void shouldUpdateCustomerPhone() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", "555-1234", null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            null,
            null,
            "555-5678",
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("555-5678", result.getPhone());

        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void shouldUpdateCustomerAddress() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand.AddressDto newAddress = new UpdateCustomerCommand.AddressDto(
            "456 Oak Ave",
            "Chicago",
            "IL",
            "60601",
            "USA"
        );

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            null,
            null,
            null,
            newAddress
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertNotNull(result.getAddress());
        assertEquals("456 Oak Ave", result.getAddress().getStreet());
        assertEquals("Chicago", result.getAddress().getCity());

        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void shouldUpdateMultipleFields() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand.AddressDto newAddress = new UpdateCustomerCommand.AddressDto(
            "456 Oak Ave",
            "Chicago",
            "IL",
            "60601",
            "USA"
        );

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            "Jane Smith",
            "jane@example.com",
            "555-9999",
            newAddress
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailAndIdNot("jane@example.com", customerId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("Jane Smith", result.getName());
        assertEquals("jane@example.com", result.getEmail());
        assertEquals("555-9999", result.getPhone());
        assertNotNull(result.getAddress());
        assertEquals("456 Oak Ave", result.getAddress().getStreet());

        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            "Jane Doe",
            null,
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );

        assertEquals("Customer not found with id: " + customerId, exception.getMessage());
        verify(customerRepository).findById(customerId);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            null,
            "existing@example.com",
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailAndIdNot("existing@example.com", customerId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );

        assertEquals("Email already exists: existing@example.com", exception.getMessage());
        verify(customerRepository).existsByEmailAndIdNot("existing@example.com", customerId);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldAllowKeepingSameEmail() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            "Jane Doe",
            "john@example.com", // Same email
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("Jane Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());

        // Should not check email uniqueness when email is not changed
        verify(customerRepository, never()).existsByEmailAndIdNot(anyString(), any(UUID.class));
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void shouldPublishCustomerUpdatedEvent() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = Customer.create("John Doe", "john@example.com", null, null);

        UpdateCustomerCommand command = new UpdateCustomerCommand(
            customerId,
            "Jane Doe",
            null,
            null,
            null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);

        ArgumentCaptor<CustomerUpdated> eventCaptor = ArgumentCaptor.forClass(CustomerUpdated.class);

        // Act
        handler.handle(command);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CustomerUpdated event = eventCaptor.getValue();
        assertEquals(existingCustomer.getId(), event.customerId());
        assertEquals("Jane Doe", event.name());
        assertEquals("john@example.com", event.email());
        assertNotNull(event.occurredAt());
    }
}
