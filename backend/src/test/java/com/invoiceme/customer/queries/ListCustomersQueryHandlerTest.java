package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.queries.dto.CustomerListItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ListCustomersQueryHandler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListCustomersQueryHandler Unit Tests")
class ListCustomersQueryHandlerTest {

    @Mock
    private CustomerQueryRepository customerQueryRepository;

    @InjectMocks
    private ListCustomersQueryHandler handler;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        Address address = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        customer1 = Customer.create("Alice Johnson", "alice@example.com", "+1-555-111-1111", address);
        customer2 = Customer.create("Bob Smith", "bob@example.com", "+1-555-222-2222", address);

        // Set IDs using reflection
        try {
            setCustomerId(customer1, UUID.randomUUID());
            setCustomerId(customer2, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test customers", e);
        }
    }

    private void setCustomerId(Customer customer, UUID id) throws Exception {
        var idField = Customer.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);

        var createdAtField = Customer.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(customer, LocalDateTime.now().minusDays(30));
    }

    @Test
    @DisplayName("Should return empty page when no customers exist")
    void shouldReturnEmptyPageWhenNoCustomers() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "name", "asc", "");
        Page<Customer> emptyPage = new PageImpl<>(Collections.emptyList());
        when(customerQueryRepository.findAllNotDeleted(any(Pageable.class))).thenReturn(emptyPage);

        // Act
        Page<CustomerListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(customerQueryRepository).findAllNotDeleted(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return paginated results correctly")
    void shouldReturnPaginatedResults() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20);
        List<Customer> customers = Arrays.asList(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customers, Pageable.ofSize(20), 2);

        when(customerQueryRepository.findAllNotDeleted(any(Pageable.class))).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(3);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(new BigDecimal("500.00"));

        // Act
        Page<CustomerListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);

        CustomerListItemDTO dto1 = result.getContent().get(0);
        assertThat(dto1.name()).isEqualTo("Alice Johnson");
        assertThat(dto1.email()).isEqualTo("alice@example.com");
        assertThat(dto1.totalInvoices()).isEqualTo(3);
        assertThat(dto1.outstandingBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should sort by name ascending by default")
    void shouldSortByNameAscendingByDefault() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20);
        List<Customer> customers = Arrays.asList(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Should sort by name descending when specified")
    void shouldSortByNameDescending() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "name", "desc", "");
        List<Customer> customers = Arrays.asList(customer2, customer1);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Should sort by email when specified")
    void shouldSortByEmail() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "email", "asc", "");
        List<Customer> customers = Arrays.asList(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("email")).isNotNull();
    }

    @Test
    @DisplayName("Should sort by createdAt when specified")
    void shouldSortByCreatedAt() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "createdAt", "asc", "");
        List<Customer> customers = Arrays.asList(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("Should filter by search term")
    void shouldFilterBySearchTerm() {
        // Arrange
        String searchTerm = "alice";
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "name", "asc", searchTerm);
        List<Customer> customers = Arrays.asList(customer1);
        Page<Customer> customerPage = new PageImpl<>(customers);

        when(customerQueryRepository.searchByNameOrEmail(eq(searchTerm), any(Pageable.class))).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        Page<CustomerListItemDTO> result = handler.handle(query);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(customerQueryRepository).searchByNameOrEmail(eq(searchTerm), any(Pageable.class));
        verify(customerQueryRepository, never()).findAllNotDeleted(any());
    }

    @Test
    @DisplayName("Should use findAllNotDeleted when search is empty")
    void shouldUseFindAllNotDeletedWhenSearchIsEmpty() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "name", "asc", "");
        List<Customer> customers = Arrays.asList(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customers);

        when(customerQueryRepository.findAllNotDeleted(any(Pageable.class))).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        verify(customerQueryRepository).findAllNotDeleted(any(Pageable.class));
        verify(customerQueryRepository, never()).searchByNameOrEmail(any(), any());
    }

    @Test
    @DisplayName("Should handle pagination parameters correctly")
    void shouldHandlePaginationParametersCorrectly() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(2, 10);
        List<Customer> customers = Arrays.asList(customer1);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(2);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should default to name sort for invalid sort field")
    void shouldDefaultToNameSortForInvalidField() {
        // Arrange
        ListCustomersQuery query = new ListCustomersQuery(0, 20, "invalidField", "asc", "");
        List<Customer> customers = Arrays.asList(customer1);
        Page<Customer> customerPage = new PageImpl<>(customers);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(customerQueryRepository.findAllNotDeleted(pageableCaptor.capture())).thenReturn(customerPage);
        when(customerQueryRepository.countInvoicesByCustomerId(any())).thenReturn(0);
        when(customerQueryRepository.calculateOutstandingBalance(any())).thenReturn(BigDecimal.ZERO);

        // Act
        handler.handle(query);

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative page number")
    void shouldThrowExceptionForNegativePage() {
        // Act & Assert
        assertThatThrownBy(() -> new ListCustomersQuery(-1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page number cannot be negative");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero page size")
    void shouldThrowExceptionForZeroPageSize() {
        // Act & Assert
        assertThatThrownBy(() -> new ListCustomersQuery(0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size must be greater than zero");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for page size exceeding 100")
    void shouldThrowExceptionForPageSizeExceeding100() {
        // Act & Assert
        assertThatThrownBy(() -> new ListCustomersQuery(0, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size cannot exceed 100");
    }
}
