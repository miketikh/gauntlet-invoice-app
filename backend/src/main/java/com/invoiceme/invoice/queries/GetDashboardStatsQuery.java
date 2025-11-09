package com.invoiceme.invoice.queries;

/**
 * Query to retrieve dashboard statistics
 * Returns aggregate data for dashboard display
 */
public record GetDashboardStatsQuery() {
    // No parameters - returns all-time stats
    // Could add optional date range filters in future
}
