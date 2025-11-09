package com.invoiceme.payment.queries;

import java.util.UUID;

/**
 * GetPaymentByIdQuery
 * Query to retrieve a single payment by ID with enriched invoice and customer data
 */
public record GetPaymentByIdQuery(
    UUID paymentId
) {
}
