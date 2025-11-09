package com.invoiceme.customer.queries.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer List Item DTO for customer list view
 * Lighter version without full address details
 */
@Schema(description = "Customer information for list view")
public record CustomerListItemDTO(
    @Schema(description = "Customer unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Customer full name", example = "John Doe")
    String name,

    @Schema(description = "Customer email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Customer phone number", example = "+1-555-123-4567", nullable = true)
    String phone,

    @Schema(description = "Timestamp when customer was created", example = "2025-11-08T12:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Total number of invoices for this customer", example = "5")
    Integer totalInvoices,

    @Schema(description = "Total outstanding balance from unpaid invoices", example = "1250.00")
    BigDecimal outstandingBalance
) {}
