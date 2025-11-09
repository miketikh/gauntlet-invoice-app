package com.invoiceme.invoice.commands;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to send an invoice (transition from Draft to Sent)
 */
public record SendInvoiceCommand(
    @NotNull(message = "Invoice ID is required")
    UUID invoiceId
) {
}
