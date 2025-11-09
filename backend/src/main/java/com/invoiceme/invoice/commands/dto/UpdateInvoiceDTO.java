package com.invoiceme.invoice.commands.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating an existing invoice
 */
public record UpdateInvoiceDTO(
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

    String notes,

    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
