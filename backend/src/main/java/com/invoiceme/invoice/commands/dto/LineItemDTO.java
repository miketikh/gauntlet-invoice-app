package com.invoiceme.invoice.commands.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO for line item in command requests
 */
public record LineItemDTO(
    @NotBlank(message = "Description is required")
    String description,

    @Positive(message = "Quantity must be positive")
    int quantity,

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price cannot be negative")
    BigDecimal unitPrice,

    @DecimalMin(value = "0.0", message = "Discount percent must be >= 0")
    @DecimalMax(value = "1.0", message = "Discount percent must be <= 1")
    BigDecimal discountPercent,

    @DecimalMin(value = "0.0", message = "Tax rate must be >= 0")
    BigDecimal taxRate
) {
    /**
     * Compact constructor to provide defaults for optional fields
     */
    public LineItemDTO {
        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        if (taxRate == null) {
            taxRate = BigDecimal.ZERO;
        }
    }
}
