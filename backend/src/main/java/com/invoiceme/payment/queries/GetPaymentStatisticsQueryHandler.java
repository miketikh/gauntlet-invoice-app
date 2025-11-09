package com.invoiceme.payment.queries;

import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentMethod;
import com.invoiceme.payment.domain.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * GetPaymentStatisticsQueryHandler
 * Handler for retrieving aggregated payment statistics for dashboard
 */
@Service
public class GetPaymentStatisticsQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetPaymentStatisticsQueryHandler.class);

    private final PaymentRepository paymentRepository;

    public GetPaymentStatisticsQueryHandler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Handles the GetPaymentStatisticsQuery
     * Calculates aggregated payment statistics
     *
     * @param query The query with optional date range filters
     * @return PaymentStatisticsDTO with aggregated data
     */
    @Transactional(readOnly = true)
    public PaymentStatisticsDTO handle(GetPaymentStatisticsQuery query) {
        logger.debug("Calculating payment statistics");

        // Calculate total collected (all time)
        BigDecimal totalCollected = paymentRepository.calculateTotalCollected();

        // Calculate today's total
        LocalDate today = LocalDate.now();
        BigDecimal collectedToday = paymentRepository.calculateTotalCollectedOnDate(today);

        // Calculate this month's total
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        BigDecimal collectedThisMonth = paymentRepository.calculateTotalCollectedInDateRange(
            monthStart,
            monthEnd
        );

        // Calculate this year's total
        int currentYear = today.getYear();
        LocalDate yearStart = LocalDate.of(currentYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(currentYear, 12, 31);
        BigDecimal collectedThisYear = paymentRepository.calculateTotalCollectedInDateRange(
            yearStart,
            yearEnd
        );

        // Get total payment count
        long totalPaymentCount = paymentRepository.countTotalPayments();

        // Calculate breakdown by payment method
        Map<PaymentMethod, BigDecimal> byMethod = calculateByPaymentMethod();

        logger.info("Payment statistics calculated: total={}, today={}, thisMonth={}, thisYear={}, count={}",
            totalCollected, collectedToday, collectedThisMonth, collectedThisYear, totalPaymentCount);

        return new PaymentStatisticsDTO(
            totalCollected,
            collectedToday,
            collectedThisMonth,
            collectedThisYear,
            (int) totalPaymentCount,
            byMethod
        );
    }

    /**
     * Calculates total amount by payment method
     *
     * @return Map of payment method to total amount
     */
    private Map<PaymentMethod, BigDecimal> calculateByPaymentMethod() {
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);

        // Initialize all methods with zero
        for (PaymentMethod method : PaymentMethod.values()) {
            byMethod.put(method, BigDecimal.ZERO);
        }

        // Calculate totals for each method
        for (PaymentMethod method : PaymentMethod.values()) {
            List<Payment> payments = paymentRepository.findByPaymentMethod(method);
            BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            byMethod.put(method, total);
        }

        return byMethod;
    }
}
