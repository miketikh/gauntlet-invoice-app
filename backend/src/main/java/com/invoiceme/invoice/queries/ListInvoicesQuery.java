package com.invoiceme.invoice.queries;

import com.invoiceme.invoice.domain.InvoiceStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Query to list invoices with optional filters
 * Supports filtering by customer, status, date range, and pagination
 */
public record ListInvoicesQuery(
    UUID customerId,
    InvoiceStatus status,
    LocalDate startDate,
    LocalDate endDate,
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    /**
     * Compact constructor with defaults
     */
    public ListInvoicesQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "issueDate";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESC";
        }
    }

    /**
     * Convenience constructor with defaults
     */
    public ListInvoicesQuery(
        UUID customerId,
        InvoiceStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        this(customerId, status, startDate, endDate, 0, 20, "issueDate", "DESC");
    }
}
