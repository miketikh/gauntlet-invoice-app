package com.invoiceme.invoice.queries;

import java.util.UUID;

/**
 * Query to retrieve a single invoice by ID
 * Returns full invoice details with line items and customer information
 */
public record GetInvoiceByIdQuery(
    UUID invoiceId
) {
}
