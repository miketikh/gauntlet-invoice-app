package com.invoiceme.invoice.queries.dto;

import java.math.BigDecimal;

/**
 * DTO for dashboard statistics
 * Provides aggregate data for dashboard display
 */
public record DashboardStatsDTO(
    int totalCustomers,
    int totalInvoices,
    int draftInvoices,
    int sentInvoices,
    int paidInvoices,
    BigDecimal totalRevenue,      // Sum of paid invoice amounts
    BigDecimal outstandingAmount, // Sum of sent invoice balances
    BigDecimal overdueAmount      // Sum of overdue sent invoice balances
) {
}
