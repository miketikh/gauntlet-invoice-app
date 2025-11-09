package com.invoiceme.invoice.domain;

/**
 * InvoiceNumberGenerator Domain Service
 * Generates unique sequential invoice numbers
 */
public interface InvoiceNumberGenerator {

    /**
     * Generates the next invoice number in sequence
     * Format: INV-{YEAR}-{SEQUENCE} (e.g., INV-2024-0001)
     * @return The generated invoice number
     */
    String generateNextInvoiceNumber();
}
