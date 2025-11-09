package com.invoiceme.payment.queries;

import com.invoiceme.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PaymentStatisticsDTO
 * DTO for payment statistics used in dashboard.
 * Contains aggregated payment data by various dimensions.
 */
public record PaymentStatisticsDTO(
    BigDecimal totalCollected,              // All-time total
    BigDecimal collectedToday,              // Today's payments
    BigDecimal collectedThisMonth,          // Current month's payments
    BigDecimal collectedThisYear,           // Current year's payments
    int totalPaymentCount,                  // Total number of payments
    Map<PaymentMethod, BigDecimal> byMethod // Breakdown by payment method
) {
}
