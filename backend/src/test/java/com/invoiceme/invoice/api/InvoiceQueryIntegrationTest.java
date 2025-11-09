package com.invoiceme.invoice.api;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaInvoiceRepository invoiceRepository;

    @Autowired
    private JpaCustomerRepository customerRepository;

    private Customer testCustomer;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        // Clean up
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();

        // Create test customer
        Customer customer = Customer.create(
            "Acme Corp",
            "test@acme.com",
            "555-1234",
            null
        );
        Customer savedCustomer = ((CustomerRepository) customerRepository).save(customer);
        testCustomer = savedCustomer;

        // Create test invoice
        testInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 15),
            "Net 30",
            "INV-2024-0001"
        );

        LineItem item = new LineItem(
            null,
            "Consulting Services",
            10,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),
            new BigDecimal("0.08")
        );
        testInvoice.addLineItem(item);
        testInvoice = invoiceRepository.save(testInvoice);
    }

    @Test
    @WithMockUser
    void shouldGetInvoiceById() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testInvoice.getId().toString()))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-2024-0001"))
            .andExpect(jsonPath("$.customerName").value("Acme Corp"))
            .andExpect(jsonPath("$.customerEmail").value("test@acme.com"))
            .andExpect(jsonPath("$.status").value("Draft"))
            .andExpect(jsonPath("$.subtotal").exists())
            .andExpect(jsonPath("$.totalAmount").exists())
            .andExpect(jsonPath("$.lineItems").isArray())
            .andExpect(jsonPath("$.lineItems", hasSize(1)));
    }

    @Test
    @WithMockUser
    void shouldReturn404ForNonExistentInvoice() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/invoices/{id}", nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldListAllInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser
    void shouldFilterByCustomer() throws Exception {
        mockMvc.perform(get("/api/invoices")
                .param("customerId", testCustomer.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].customerId").value(testCustomer.getId().toString()));
    }

    @Test
    @WithMockUser
    void shouldFilterByStatus() throws Exception {
        // Create sent invoice
        Invoice sentInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2024-0002"
        );
        LineItem item = new LineItem(null, "Test", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        sentInvoice.addLineItem(item);
        sentInvoice.markAsSent();
        invoiceRepository.save(sentInvoice);

        mockMvc.perform(get("/api/invoices")
                .param("status", "Sent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].status").value("Sent"));
    }

    @Test
    @WithMockUser
    void shouldFilterByDateRange() throws Exception {
        mockMvc.perform(get("/api/invoices")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @WithMockUser
    void shouldPaginateResults() throws Exception {
        // Create additional invoices
        for (int i = 2; i <= 25; i++) {
            Invoice invoice = Invoice.create(
                testCustomer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Net 30",
                String.format("INV-2024-%04d", i)
            );
            LineItem item = new LineItem(null, "Test", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
            invoice.addLineItem(item);
            invoiceRepository.save(invoice);
        }

        mockMvc.perform(get("/api/invoices")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @WithMockUser
    void shouldSortInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices")
                .param("sortBy", "invoiceNumber")
                .param("sortDirection", "ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    void shouldGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCustomers").value(1))
            .andExpect(jsonPath("$.totalInvoices").value(1))
            .andExpect(jsonPath("$.draftInvoices").value(1))
            .andExpect(jsonPath("$.sentInvoices").value(0))
            .andExpect(jsonPath("$.paidInvoices").value(0))
            .andExpect(jsonPath("$.totalRevenue").exists())
            .andExpect(jsonPath("$.outstandingAmount").exists())
            .andExpect(jsonPath("$.overdueAmount").exists());
    }

    @Test
    @WithMockUser
    void shouldCalculateDaysOverdueForOverdueInvoice() throws Exception {
        // Create overdue invoice
        Invoice overdueInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now().minusDays(60),
            LocalDate.now().minusDays(30), // Due 30 days ago
            "Net 30",
            "INV-2024-OVERDUE"
        );
        LineItem item = new LineItem(null, "Test", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        overdueInvoice.addLineItem(item);
        overdueInvoice.markAsSent();
        overdueInvoice = invoiceRepository.save(overdueInvoice);

        mockMvc.perform(get("/api/invoices/{id}", overdueInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.daysOverdue").value(30));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/invoices"))
            .andExpect(status().isForbidden()); // 403 when no auth provided
    }
}
