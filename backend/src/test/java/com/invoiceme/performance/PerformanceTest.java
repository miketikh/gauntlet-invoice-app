package com.invoiceme.performance;

import com.invoiceme.customer.commands.CreateCustomerCommand;
import com.invoiceme.customer.commands.CreateCustomerCommandHandler;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.queries.ListCustomersQuery;
import com.invoiceme.customer.queries.ListCustomersQueryHandler;
import com.invoiceme.invoice.commands.CreateInvoiceCommand;
import com.invoiceme.invoice.commands.CreateInvoiceCommandHandler;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.queries.GetDashboardStatsQuery;
import com.invoiceme.invoice.queries.GetDashboardStatsQueryHandler;
import com.invoiceme.invoice.queries.ListInvoicesQuery;
import com.invoiceme.invoice.queries.ListInvoicesQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests to ensure API response times meet requirements
 * Target: < 200ms for standard CRUD operations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PerformanceTest {

    @Autowired
    private CreateCustomerCommandHandler createCustomerHandler;

    @Autowired
    private ListCustomersQueryHandler listCustomersHandler;

    @Autowired
    private CreateInvoiceCommandHandler createInvoiceHandler;

    @Autowired
    private ListInvoicesQueryHandler listInvoicesHandler;

    @Autowired
    private GetDashboardStatsQueryHandler dashboardStatsHandler;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Create a test customer for invoice operations
        CreateCustomerCommand.AddressDto address = new CreateCustomerCommand.AddressDto(
            "123 Test St", "Test City", "TS", "12345", "USA"
        );
        CreateCustomerCommand command = new CreateCustomerCommand(
            "Test Customer",
            "test@example.com",
            "555-1234",
            address
        );
        testCustomer = createCustomerHandler.handle(command);
    }

    @Test
    void testCustomerCreationPerformance() {
        // Target: < 150ms
        long startTime = System.currentTimeMillis();

        CreateCustomerCommand.AddressDto address = new CreateCustomerCommand.AddressDto(
            "456 Perf Ave", "Perf City", "PC", "99999", "USA"
        );
        CreateCustomerCommand command = new CreateCustomerCommand(
            "Performance Test Customer",
            "perf" + System.currentTimeMillis() + "@example.com",
            "555-9999",
            address
        );
        Customer customer = createCustomerHandler.handle(command);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(customer).isNotNull();
        assertThat(duration).isLessThan(150L);
        System.out.println("Customer creation time: " + duration + "ms (target: <150ms)");
    }

    @Test
    void testCustomerListQueryPerformance() {
        // Create some test data
        for (int i = 0; i < 10; i++) {
            CreateCustomerCommand.AddressDto address = new CreateCustomerCommand.AddressDto(
                i + " Main St", "City", "ST", "12345", "USA"
            );
            CreateCustomerCommand command = new CreateCustomerCommand(
                "Customer " + i,
                "customer" + i + System.currentTimeMillis() + "@example.com",
                "555-000" + i,
                address
            );
            createCustomerHandler.handle(command);
        }

        // Target: < 200ms
        long startTime = System.currentTimeMillis();

        ListCustomersQuery query = new ListCustomersQuery(
            0, // page
            20, // size
            "name", // sortBy
            "asc", // sortDirection
            null // search
        );
        var result = listCustomersHandler.handle(query);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(200L);
        System.out.println("Customer list query time: " + duration + "ms (target: <200ms)");
    }

    @Test
    void testInvoiceCreationPerformance() {
        // Target: < 200ms
        long startTime = System.currentTimeMillis();

        List<LineItemDTO> lineItems = new ArrayList<>();
        lineItems.add(new LineItemDTO(
            "Test Service",
            2,
            BigDecimal.valueOf(100.00),
            BigDecimal.ZERO,
            BigDecimal.valueOf(0.08)
        ));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            lineItems,
            "Performance test invoice"
        );
        InvoiceResponseDTO invoice = createInvoiceHandler.handle(command);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(invoice).isNotNull();
        assertThat(duration).isLessThan(200L);
        System.out.println("Invoice creation time: " + duration + "ms (target: <200ms)");
    }

    @Test
    void testInvoiceListQueryPerformance() {
        // Create some test invoices
        for (int i = 0; i < 10; i++) {
            List<LineItemDTO> lineItems = new ArrayList<>();
            lineItems.add(new LineItemDTO(
                "Service " + i,
                1,
                BigDecimal.valueOf(50.00),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ));

            CreateInvoiceCommand command = new CreateInvoiceCommand(
                testCustomer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Net 30",
                lineItems,
                "Test invoice " + i
            );
            createInvoiceHandler.handle(command);
        }

        // Target: < 200ms
        long startTime = System.currentTimeMillis();

        ListInvoicesQuery query = new ListInvoicesQuery(
            null, // customerId filter
            null, // status filter
            null, // startDate filter
            null, // endDate filter
            0, // page
            20, // size
            "issueDate", // sortBy
            "desc" // sortDirection
        );
        var result = listInvoicesHandler.handle(query);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(200L);
        System.out.println("Invoice list query time: " + duration + "ms (target: <200ms)");
    }

    @Test
    void testDashboardStatsPerformance() {
        // Create some test data
        for (int i = 0; i < 5; i++) {
            List<LineItemDTO> lineItems = new ArrayList<>();
            lineItems.add(new LineItemDTO(
                "Service " + i,
                1,
                BigDecimal.valueOf(100.00),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ));

            CreateInvoiceCommand command = new CreateInvoiceCommand(
                testCustomer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Net 30",
                lineItems,
                "Dashboard test " + i
            );
            createInvoiceHandler.handle(command);
        }

        // Target: < 200ms
        long startTime = System.currentTimeMillis();

        GetDashboardStatsQuery query = new GetDashboardStatsQuery();
        var stats = dashboardStatsHandler.handle(query);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(stats).isNotNull();
        assertThat(duration).isLessThan(200L);
        System.out.println("Dashboard stats query time: " + duration + "ms (target: <200ms)");
    }

    @Test
    void testCachedQueryPerformance() {
        // First call - not cached
        ListCustomersQuery query = new ListCustomersQuery(
            0, 20, "name", "asc", null
        );

        long startTime1 = System.currentTimeMillis();
        var result1 = listCustomersHandler.handle(query);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call - should be cached and much faster
        long startTime2 = System.currentTimeMillis();
        var result2 = listCustomersHandler.handle(query);
        long duration2 = System.currentTimeMillis() - startTime2;

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        System.out.println("First call (uncached): " + duration1 + "ms");
        System.out.println("Second call (cached): " + duration2 + "ms");
        System.out.println("Cache improvement: " + (duration1 - duration2) + "ms");

        // Cached call should be faster (though in tests it might be similar due to H2)
        assertThat(duration2).isLessThanOrEqualTo(duration1);
    }
}
