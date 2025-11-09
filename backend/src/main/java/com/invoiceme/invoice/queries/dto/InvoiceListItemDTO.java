package com.invoiceme.invoice.queries.dto;

import com.invoiceme.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight DTO for invoice list views
 * Does not include line items for performance optimization
 */
public record InvoiceListItemDTO(
    UUID id,
    String invoiceNumber,
    UUID customerId,
    String customerName,
    LocalDate issueDate,
    LocalDate dueDate,
    InvoiceStatus status,
    BigDecimal totalAmount,
    BigDecimal balance,
    Long version,
    Integer daysOverdue
) {
}
