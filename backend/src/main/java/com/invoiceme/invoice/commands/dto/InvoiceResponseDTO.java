package com.invoiceme.invoice.commands.dto;

import com.invoiceme.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for invoice response
 */
public record InvoiceResponseDTO(
    UUID id,
    String invoiceNumber,
    UUID customerId,
    String customerName,
    String customerEmail,
    LocalDate issueDate,
    LocalDate dueDate,
    InvoiceStatus status,
    String paymentTerms,
    BigDecimal subtotal,
    BigDecimal totalDiscount,
    BigDecimal totalTax,
    BigDecimal totalAmount,
    BigDecimal balance,
    List<LineItemResponseDTO> lineItems,
    String notes,
    Long version,
    Instant createdAt,
    Instant updatedAt,
    Integer daysOverdue
) {
}
