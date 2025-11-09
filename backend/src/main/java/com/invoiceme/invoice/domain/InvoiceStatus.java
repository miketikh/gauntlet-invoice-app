package com.invoiceme.invoice.domain;

/**
 * InvoiceStatus Enum
 * Represents the lifecycle state of an invoice
 */
public enum InvoiceStatus {
    /**
     * Initial state - invoice is editable, line items can be modified
     */
    Draft,

    /**
     * Invoice has been sent to customer - locked from editing
     */
    Sent,

    /**
     * Invoice is fully paid - balance is zero
     */
    Paid
}
