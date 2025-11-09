package com.invoiceme.invoice.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * InvoiceRepository Domain Interface
 * Defines persistence operations for Invoice aggregate
 * Implementation in infrastructure layer
 */
public interface InvoiceRepository {

    /**
     * Saves an invoice (create or update)
     * @param invoice The invoice to save
     * @return The saved invoice
     */
    Invoice save(Invoice invoice);

    /**
     * Finds an invoice by ID
     * @param id The invoice ID
     * @return Optional containing the invoice if found
     */
    Optional<Invoice> findById(UUID id);

    /**
     * Checks if an invoice number already exists
     * @param invoiceNumber The invoice number to check
     * @return true if exists, false otherwise
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Finds an invoice by its invoice number
     * @param invoiceNumber The invoice number
     * @return Optional containing the invoice if found
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
