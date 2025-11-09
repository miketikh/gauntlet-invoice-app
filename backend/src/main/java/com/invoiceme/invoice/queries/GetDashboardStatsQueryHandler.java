package com.invoiceme.invoice.queries;

import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import com.invoiceme.invoice.queries.dto.DashboardStatsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handler for GetDashboardStatsQuery
 * Retrieves aggregate statistics for dashboard display
 * Cached for 1 minute to reduce database load
 */
@Service
public class GetDashboardStatsQueryHandler {

    private final JpaInvoiceRepository invoiceRepository;
    private final JpaCustomerRepository customerRepository;

    public GetDashboardStatsQueryHandler(JpaInvoiceRepository invoiceRepository,
                                         JpaCustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the query to get dashboard stats
     * @param query The query (no parameters)
     * @return DashboardStatsDTO with all statistics
     */
    @Cacheable(value = "dashboardStats")
    @Transactional(readOnly = true)
    public DashboardStatsDTO handle(GetDashboardStatsQuery query) {
        // Count customers (excluding soft-deleted)
        long totalCustomers = customerRepository.count();

        // Count invoices by status
        long draftInvoices = invoiceRepository.countByStatus(InvoiceStatus.Draft);
        long sentInvoices = invoiceRepository.countByStatus(InvoiceStatus.Sent);
        long paidInvoices = invoiceRepository.countByStatus(InvoiceStatus.Paid);
        long totalInvoices = draftInvoices + sentInvoices + paidInvoices;

        // Calculate amounts
        BigDecimal totalRevenue = invoiceRepository.calculateTotalRevenue();
        BigDecimal outstandingAmount = invoiceRepository.calculateOutstandingAmount();
        BigDecimal overdueAmount = invoiceRepository.calculateOverdueAmount();

        return new DashboardStatsDTO(
            (int) totalCustomers,
            (int) totalInvoices,
            (int) draftInvoices,
            (int) sentInvoices,
            (int) paidInvoices,
            totalRevenue,
            outstandingAmount,
            overdueAmount
        );
    }
}
