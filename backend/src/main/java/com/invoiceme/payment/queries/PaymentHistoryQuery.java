package com.invoiceme.payment.queries;

import com.invoiceme.payment.domain.PaymentMethod;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentHistoryQuery
 * Query for payment history with dynamic filtering.
 * Supports pagination and sorting via Spring Data Pageable.
 */
public record PaymentHistoryQuery(
    Optional<UUID> customerId,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PaymentMethod> paymentMethod,
    Pageable pageable
) {
    /**
     * Factory method for querying all payments
     *
     * @param pageable Pagination and sorting parameters
     * @return PaymentHistoryQuery with no filters
     */
    public static PaymentHistoryQuery allPayments(Pageable pageable) {
        return new PaymentHistoryQuery(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            pageable
        );
    }

    /**
     * Factory method for querying payments for a specific customer
     *
     * @param customerId Customer ID to filter by
     * @param pageable Pagination and sorting parameters
     * @return PaymentHistoryQuery filtered by customer
     */
    public static PaymentHistoryQuery forCustomer(UUID customerId, Pageable pageable) {
        return new PaymentHistoryQuery(
            Optional.of(customerId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            pageable
        );
    }

    /**
     * Factory method for querying payments within a date range
     *
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @param pageable Pagination and sorting parameters
     * @return PaymentHistoryQuery filtered by date range
     */
    public static PaymentHistoryQuery forDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        return new PaymentHistoryQuery(
            Optional.empty(),
            Optional.of(start),
            Optional.of(end),
            Optional.empty(),
            pageable
        );
    }

    /**
     * Factory method for querying payments by payment method
     *
     * @param method Payment method to filter by
     * @param pageable Pagination and sorting parameters
     * @return PaymentHistoryQuery filtered by payment method
     */
    public static PaymentHistoryQuery forPaymentMethod(PaymentMethod method, Pageable pageable) {
        return new PaymentHistoryQuery(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(method),
            pageable
        );
    }
}
