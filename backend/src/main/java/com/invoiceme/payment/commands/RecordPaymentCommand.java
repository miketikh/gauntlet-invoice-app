package com.invoiceme.payment.commands;

import com.invoiceme.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RecordPaymentCommand
 * Immutable command for recording a payment against an invoice
 * Follows CQRS pattern for command handling
 *
 * Validation rules:
 * - Invoice ID is required
 * - Payment date is required and cannot be in the future
 * - Amount must be positive
 * - Payment method is required
 * - Idempotency key is optional (for preventing duplicate payments)
 */
public record RecordPaymentCommand(
    @NotNull(message = "Invoice ID is required")
    UUID invoiceId,

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    LocalDate paymentDate,

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    String reference,

    String notes,

    String idempotencyKey
) {
}
