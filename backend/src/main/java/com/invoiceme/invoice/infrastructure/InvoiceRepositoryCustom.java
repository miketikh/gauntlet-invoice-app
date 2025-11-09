package com.invoiceme.invoice.infrastructure;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Custom repository interface for complex invoice queries
 * Implemented by InvoiceRepositoryCustomImpl
 */
public interface InvoiceRepositoryCustom {

    /**
     * Finds invoices with dynamic filtering
     * @param customerId Optional customer ID filter
     * @param status Optional status filter
     * @param startDate Optional start date filter (issueDate >= startDate)
     * @param endDate Optional end date filter (issueDate <= endDate)
     * @param pageable Pagination and sorting parameters
     * @return Page of invoices matching filters
     */
    Page<Invoice> findWithFilters(
        UUID customerId,
        InvoiceStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );

    /**
     * Counts invoices by status
     * @param status The invoice status
     * @return Count of invoices with given status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Calculates total revenue (sum of paid invoice amounts)
     * @return Total revenue from paid invoices
     */
    BigDecimal calculateTotalRevenue();

    /**
     * Calculates outstanding amount (sum of sent invoice balances)
     * @return Outstanding amount from sent invoices
     */
    BigDecimal calculateOutstandingAmount();

    /**
     * Calculates overdue amount (sum of sent invoice balances where due date < today)
     * @return Overdue amount
     */
    BigDecimal calculateOverdueAmount();
}
