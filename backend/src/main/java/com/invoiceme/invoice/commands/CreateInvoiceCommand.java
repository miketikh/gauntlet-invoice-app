package com.invoiceme.invoice.commands;

import com.invoiceme.invoice.commands.dto.LineItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command to create a new invoice
 */
public record CreateInvoiceCommand(
    @NotNull(message = "Customer ID is required")
    UUID customerId,

    @NotNull(message = "Issue date is required")
    LocalDate issueDate,

    LocalDate dueDate,

    @NotBlank(message = "Payment terms are required")
    String paymentTerms,

    @NotEmpty(message = "At least one line item is required")
    @Valid
    List<LineItemDTO> lineItems,

    String notes
) {
}
