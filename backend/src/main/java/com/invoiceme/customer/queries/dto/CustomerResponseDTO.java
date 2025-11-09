package com.invoiceme.customer.queries.dto;

import com.invoiceme.customer.domain.Address;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Response DTO for detailed customer view
 * Includes all customer fields plus computed data (totalInvoices, outstandingBalance)
 */
@Schema(description = "Detailed customer information with computed fields")
public record CustomerResponseDTO(
    @Schema(description = "Customer unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Customer full name", example = "John Doe")
    String name,

    @Schema(description = "Customer email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Customer phone number", example = "+1-555-123-4567", nullable = true)
    String phone,

    @Schema(description = "Customer address")
    Address address,

    @Schema(description = "Timestamp when customer was created", example = "2025-11-08T12:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when customer was last updated", example = "2025-11-08T13:00:00")
    LocalDateTime updatedAt,

    @Schema(description = "Total number of invoices for this customer", example = "5")
    Integer totalInvoices,

    @Schema(description = "Total outstanding balance from unpaid invoices", example = "1250.00")
    BigDecimal outstandingBalance
) {}
