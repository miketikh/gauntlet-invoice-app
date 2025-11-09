package com.invoiceme.payment.queries;

import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentResponseDTO
 * Enhanced DTO for payment queries with invoice and customer context.
 * Export-ready format for accounting reconciliation.
 * Uses Java record for immutability
 */
public record PaymentResponseDTO(
    UUID id,
    UUID invoiceId,
    LocalDate paymentDate,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    String reference,
    String notes,
    LocalDateTime createdAt,
    String createdBy,

    // Enriched fields from Invoice
    String invoiceNumber,
    BigDecimal invoiceTotal,
    BigDecimal remainingBalance,
    InvoiceStatus invoiceStatus,

    // Enriched fields from Customer (via Invoice)
    String customerName,
    String customerEmail,

    // Optional fields for payment history queries
    BigDecimal runningBalance  // Cumulative balance (for payment history - null for single payment queries)
) {
}
