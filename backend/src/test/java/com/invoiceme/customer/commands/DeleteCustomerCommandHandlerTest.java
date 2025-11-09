package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerDeleted;
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
class DeleteCustomerCommandHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeleteCustomerCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeleteCustomerCommandHandler(customerRepository, eventPublisher);
    }

    @Test
    void shouldSoftDeleteCustomer() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        DeleteCustomerCommand command = new DeleteCustomerCommand(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        handler.handle(command);

        // Assert
        assertTrue(customer.isDeleted());
        assertNotNull(customer.getDeletedAt());

        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(customer);
        verify(eventPublisher).publishEvent(any(CustomerDeleted.class));
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        DeleteCustomerCommand command = new DeleteCustomerCommand(customerId);

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
    void shouldThrowExceptionWhenCustomerAlreadyDeleted() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);
        customer.delete(); // Mark as already deleted

        DeleteCustomerCommand command = new DeleteCustomerCommand(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );

        assertEquals("Customer already deleted with id: " + customerId, exception.getMessage());
        verify(customerRepository).findById(customerId);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishCustomerDeletedEvent() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        DeleteCustomerCommand command = new DeleteCustomerCommand(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        ArgumentCaptor<CustomerDeleted> eventCaptor = ArgumentCaptor.forClass(CustomerDeleted.class);

        // Act
        handler.handle(command);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CustomerDeleted event = eventCaptor.getValue();
        assertEquals(customer.getId(), event.customerId());
        assertNotNull(event.occurredAt());
    }

    @Test
    void shouldSaveCustomerWithDeletedAtTimestamp() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        DeleteCustomerCommand command = new DeleteCustomerCommand(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);

        // Act
        handler.handle(command);

        // Assert
        verify(customerRepository).save(customerCaptor.capture());
        Customer savedCustomer = customerCaptor.getValue();
        assertTrue(savedCustomer.isDeleted());
        assertNotNull(savedCustomer.getDeletedAt());
    }
}
