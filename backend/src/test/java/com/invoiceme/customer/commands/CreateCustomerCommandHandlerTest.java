package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCustomerCommandHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateCustomerCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateCustomerCommandHandler(customerRepository, eventPublisher);
    }

    @Test
    void shouldCreateCustomerSuccessfully() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "555-1234",
            null
        );

        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            // Simulate setting ID as would happen in real save
            return customer;
        });

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("555-1234", result.getPhone());

        verify(customerRepository).existsByEmail("john@example.com");
        verify(customerRepository).save(any(Customer.class));
        verify(eventPublisher).publishEvent(any(CustomerCreated.class));
    }

    @Test
    void shouldCreateCustomerWithAddress() {
        // Arrange
        CreateCustomerCommand.AddressDto addressDto = new CreateCustomerCommand.AddressDto(
            "123 Main St",
            "Springfield",
            "IL",
            "62701",
            "USA"
        );

        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "555-1234",
            addressDto
        );

        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAddress());
        assertEquals("123 Main St", result.getAddress().getStreet());
        assertEquals("Springfield", result.getAddress().getCity());

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldCreateCustomerWithMinimalData() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "Jane Doe",
            "jane@example.com",
            null,
            null
        );

        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertNotNull(result);
        assertEquals("Jane Doe", result.getName());
        assertEquals("jane@example.com", result.getEmail());
        assertNull(result.getPhone());
        assertNull(result.getAddress());

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            null,
            null
        );

        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );

        assertEquals("Email already exists: john@example.com", exception.getMessage());
        verify(customerRepository).existsByEmail("john@example.com");
        verify(customerRepository, never()).save(any(Customer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishCustomerCreatedEvent() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            null,
            null
        );

        Customer savedCustomer = Customer.create("John Doe", "john@example.com", null, null);

        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        ArgumentCaptor<CustomerCreated> eventCaptor = ArgumentCaptor.forClass(CustomerCreated.class);

        // Act
        handler.handle(command);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CustomerCreated event = eventCaptor.getValue();
        assertEquals(savedCustomer.getId(), event.customerId());
        assertEquals("John Doe", event.name());
        assertEquals("john@example.com", event.email());
        assertNotNull(event.occurredAt());
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "JOHN@EXAMPLE.COM",
            null,
            null
        );

        when(customerRepository.existsByEmail("JOHN@EXAMPLE.COM")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void shouldTrimNameAndEmail() {
        // Arrange
        CreateCustomerCommand command = new CreateCustomerCommand(
            "  John Doe  ",
            "  john@example.com  ",
            null,
            null
        );

        when(customerRepository.existsByEmail("  john@example.com  ")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = handler.handle(command);

        // Assert
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
    }
}
