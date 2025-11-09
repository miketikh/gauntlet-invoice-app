package com.invoiceme.payment.commands;

import com.invoiceme.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RecordPaymentDTO
 * Command DTO for recording a new payment
 * Uses Java record for immutability
 */
public record RecordPaymentDTO(
    @NotNull(message = "Invoice ID is required")
    UUID invoiceId,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    String reference,

    String notes
) {
}
