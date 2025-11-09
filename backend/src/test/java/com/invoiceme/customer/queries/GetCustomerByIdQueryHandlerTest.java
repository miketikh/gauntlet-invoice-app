package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerNotFoundException;
import com.invoiceme.customer.queries.dto.CustomerResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetCustomerByIdQueryHandler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetCustomerByIdQueryHandler Unit Tests")
class GetCustomerByIdQueryHandlerTest {

    @Mock
    private CustomerQueryRepository customerQueryRepository;

    @InjectMocks
    private GetCustomerByIdQueryHandler handler;

    private UUID customerId;
    private Customer customer;
    private Address address;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        address = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        customer = Customer.create("John Doe", "john.doe@example.com", "+1-555-123-4567", address);

        // Use reflection to set the ID since it's private
        try {
            var idField = Customer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(customer, customerId);

            var createdAtField = Customer.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(customer, LocalDateTime.now().minusDays(30));

            var updatedAtField = Customer.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(customer, LocalDateTime.now().minusDays(1));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test customer", e);
        }
    }

    @Test
    @DisplayName("Should return customer when found")
    void shouldReturnCustomerWhenFound() {
        // Arrange
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerQueryRepository.countInvoicesByCustomerId(customerId)).thenReturn(5);
        when(customerQueryRepository.calculateOutstandingBalance(customerId)).thenReturn(new BigDecimal("1250.00"));

        // Act
        CustomerResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        assertThat(result.phone()).isEqualTo("+1-555-123-4567");
        assertThat(result.address()).isEqualTo(address);
        assertThat(result.totalInvoices()).isEqualTo(5);
        assertThat(result.outstandingBalance()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();

        verify(customerQueryRepository).findById(customerId);
        verify(customerQueryRepository).countInvoicesByCustomerId(customerId);
        verify(customerQueryRepository).calculateOutstandingBalance(customerId);
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining(customerId.toString());

        verify(customerQueryRepository).findById(customerId);
        verify(customerQueryRepository, never()).countInvoicesByCustomerId(any());
        verify(customerQueryRepository, never()).calculateOutstandingBalance(any());
    }

    @Test
    @DisplayName("Should handle customer with no invoices")
    void shouldHandleCustomerWithNoInvoices() {
        // Arrange
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerQueryRepository.countInvoicesByCustomerId(customerId)).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(customerId)).thenReturn(BigDecimal.ZERO);

        // Act
        CustomerResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.totalInvoices()).isEqualTo(0);
        assertThat(result.outstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should correctly compute totalInvoices")
    void shouldCorrectlyComputeTotalInvoices() {
        // Arrange
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerQueryRepository.countInvoicesByCustomerId(customerId)).thenReturn(42);
        when(customerQueryRepository.calculateOutstandingBalance(customerId)).thenReturn(BigDecimal.ZERO);

        // Act
        CustomerResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result.totalInvoices()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should correctly compute outstandingBalance")
    void shouldCorrectlyComputeOutstandingBalance() {
        // Arrange
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        BigDecimal expectedBalance = new BigDecimal("9999.99");
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerQueryRepository.countInvoicesByCustomerId(customerId)).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(customerId)).thenReturn(expectedBalance);

        // Act
        CustomerResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result.outstandingBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    @DisplayName("Should handle customer with null address")
    void shouldHandleCustomerWithNullAddress() {
        // Arrange
        Customer customerWithoutAddress = Customer.create("Jane Doe", "jane@example.com", null, null);
        try {
            var idField = Customer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(customerWithoutAddress, customerId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }

        GetCustomerByIdQuery query = new GetCustomerByIdQuery(customerId);
        when(customerQueryRepository.findById(customerId)).thenReturn(Optional.of(customerWithoutAddress));
        when(customerQueryRepository.countInvoicesByCustomerId(customerId)).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(customerId)).thenReturn(BigDecimal.ZERO);

        // Act
        CustomerResponseDTO result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.address()).isNull();
        assertThat(result.phone()).isNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when query customerId is null")
    void shouldThrowExceptionWhenQueryCustomerIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GetCustomerByIdQuery(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer ID cannot be null");
    }
}
