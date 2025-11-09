package com.invoiceme.payment.queries;

import com.invoiceme.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentResponseDTO
 * Query DTO for payment response with enriched invoice/customer data
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

    // Enriched fields from related entities
    String invoiceNumber,
    String customerName,
    BigDecimal remainingBalance
) {
}
