package com.invoiceme.payment.queries;

import java.util.UUID;

/**
 * ListPaymentsByInvoiceQuery
 * Query to retrieve all payments for a specific invoice with running balance calculation
 */
public record ListPaymentsByInvoiceQuery(
    UUID invoiceId
) {
}
