package com.invoiceme.customer.api;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.queries.CustomerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CustomerQueryController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CustomerQueryController Integration Tests")
class CustomerQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    private Customer customer1;
    private Customer customer2;
    private Customer customer3;

    @BeforeEach
    void setUp() {
        // Clean up before each test - soft delete all existing customers
        customerQueryRepository.findAll().forEach(c -> {
            if (!c.isDeleted()) {
                c.delete();
                customerRepository.save(c);
            }
        });

        // Create test customers
        Address address1 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        Address address2 = new Address("456 Oak Ave", "Chicago", "IL", "60601", "USA");
        Address address3 = new Address("789 Elm St", "Peoria", "IL", "61602", "USA");

        customer1 = Customer.create("Alice Johnson", "alice@example.com", "+1-555-111-1111", address1);
        customer2 = Customer.create("Bob Smith", "bob@example.com", "+1-555-222-2222", address2);
        customer3 = Customer.create("Charlie Brown", "charlie@example.com", "+1-555-333-3333", address3);

        customer1 = customerRepository.save(customer1);
        customer2 = customerRepository.save(customer2);
        customer3 = customerRepository.save(customer3);
    }

    @Test
    @DisplayName("Should require authentication for GET /api/customers")
    void shouldRequireAuthenticationForListCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/customers/{id}")
    void shouldRequireAuthenticationForGetCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with customer when found")
    void shouldReturnCustomerWhenFound() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customer1.getId().toString()))
            .andExpect(jsonPath("$.name").value("Alice Johnson"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.phone").value("+1-555-111-1111"))
            .andExpect(jsonPath("$.address.street").value("123 Main St"))
            .andExpect(jsonPath("$.address.city").value("Springfield"))
            .andExpect(jsonPath("$.address.state").value("IL"))
            .andExpect(jsonPath("$.address.postalCode").value("62701"))
            .andExpect(jsonPath("$.address.country").value("USA"))
            .andExpect(jsonPath("$.totalInvoices").value(0))
            .andExpect(jsonPath("$.outstandingBalance").value(0.00))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/customers/" + nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer Not Found"))
            .andExpect(jsonPath("$.detail").value(containsString(nonExistentId.toString())));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when customer is soft-deleted")
    void shouldReturn404WhenCustomerIsDeleted() throws Exception {
        customer1.delete();
        customerRepository.save(customer1);

        mockMvc.perform(get("/api/customers/" + customer1.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return paginated list of customers")
    void shouldReturnPaginatedListOfCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list when no customers exist")
    void shouldReturnEmptyListWhenNoCustomers() throws Exception {
        // Delete all customers
        customer1.delete();
        customer2.delete();
        customer3.delete();
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);

        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Should respect page size parameter")
    void shouldRespectPageSizeParameter() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return second page correctly")
    void shouldReturnSecondPageCorrectly() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Should sort by name ascending by default")
    void shouldSortByNameAscendingByDefault() throws Exception {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Alice Johnson"))
            .andExpect(jsonPath("$.content[1].name").value("Bob Smith"))
            .andExpect(jsonPath("$.content[2].name").value("Charlie Brown"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should sort by name descending when specified")
    void shouldSortByNameDescending() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("sort", "name")
                .param("direction", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Charlie Brown"))
            .andExpect(jsonPath("$.content[1].name").value("Bob Smith"))
            .andExpect(jsonPath("$.content[2].name").value("Alice Johnson"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should sort by email ascending")
    void shouldSortByEmailAscending() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("sort", "email")
                .param("direction", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
            .andExpect(jsonPath("$.content[1].email").value("bob@example.com"))
            .andExpect(jsonPath("$.content[2].email").value("charlie@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter by name search term")
    void shouldFilterByNameSearchTerm() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("search", "alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name").value("Alice Johnson"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter by email search term")
    void shouldFilterByEmailSearchTerm() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("search", "bob@example"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].email").value("bob@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should perform case-insensitive search")
    void shouldPerformCaseInsensitiveSearch() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("search", "ALICE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name").value("Alice Johnson"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list when search has no matches")
    void shouldReturnEmptyListWhenSearchHasNoMatches() throws Exception {
        mockMvc.perform(get("/api/customers")
                .param("search", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should exclude soft-deleted customers from list")
    void shouldExcludeSoftDeletedCustomersFromList() throws Exception {
        customer2.delete();
        customerRepository.save(customer2);

        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("Should include computed fields in list response")
    void shouldIncludeComputedFieldsInListResponse() throws Exception {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].totalInvoices").value(0))
            .andExpect(jsonPath("$.content[0].outstandingBalance").value(0.00));
    }

    @Test
    @WithMockUser
    @DisplayName("Should use default values when optional parameters are omitted")
    void shouldUseDefaultValuesWhenParametersOmitted() throws Exception {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle invalid UUID gracefully")
    void shouldHandleInvalidUuidGracefully() throws Exception {
        mockMvc.perform(get("/api/customers/invalid-uuid"))
            .andExpect(status().isBadRequest());
    }
}
