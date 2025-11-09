package com.invoiceme.payment.queries;

import java.time.LocalDate;
import java.util.Optional;

/**
 * GetPaymentStatisticsQuery
 * Query to retrieve aggregated payment statistics for dashboard
 */
public record GetPaymentStatisticsQuery(
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate
) {
    /**
     * Factory method for querying all-time statistics
     *
     * @return Query with no date filters
     */
    public static GetPaymentStatisticsQuery allTime() {
        return new GetPaymentStatisticsQuery(Optional.empty(), Optional.empty());
    }

    /**
     * Factory method for querying statistics within a date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Query filtered by date range
     */
    public static GetPaymentStatisticsQuery forDateRange(LocalDate startDate, LocalDate endDate) {
        return new GetPaymentStatisticsQuery(Optional.ofNullable(startDate), Optional.ofNullable(endDate));
    }
}
