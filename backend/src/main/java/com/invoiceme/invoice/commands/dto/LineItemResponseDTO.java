package com.invoiceme.invoice.commands.dto;

import java.math.BigDecimal;

/**
 * DTO for line item in response
 */
public record LineItemResponseDTO(
    String id,
    String description,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal discountPercent,
    BigDecimal taxRate,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal taxableAmount,
    BigDecimal taxAmount,
    BigDecimal total
) {
}
