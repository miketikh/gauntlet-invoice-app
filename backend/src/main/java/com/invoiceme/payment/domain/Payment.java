package com.invoiceme.payment.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment Entity
 * Represents a payment applied to an invoice
 * Immutable after creation - no setters provided
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_invoice_id", columnList = "invoiceId"),
    @Index(name = "idx_payments_payment_date", columnList = "paymentDate")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    @NotNull
    private UUID invoiceId;

    @Column(name = "payment_date", nullable = false)
    @NotNull
    private LocalDate paymentDate;

    @Column(precision = 10, scale = 2, nullable = false)
    @NotNull
    @Positive
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    @NotNull
    private PaymentMethod paymentMethod;

    @Column(length = 255)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 255)
    @NotNull
    private String createdBy;

    /**
     * Private constructor for creating Payment instances
     * Use static factory method createPayment() instead
     */
    private Payment(UUID invoiceId, LocalDate paymentDate, BigDecimal amount,
                   PaymentMethod paymentMethod, String reference, String notes, String createdBy) {
        this.invoiceId = invoiceId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    /**
     * Factory method for creating payments with validation
     * Ensures all business rules are validated before creating a Payment instance
     *
     * @param invoiceId The invoice ID this payment applies to
     * @param paymentDate The date the payment was made
     * @param amount The payment amount
     * @param paymentMethod The method of payment
     * @param reference Optional reference number (transaction ID, check number, etc.)
     * @param notes Optional notes about the payment
     * @param createdBy The user who recorded this payment
     * @return A new validated Payment instance
     * @throws InvalidPaymentException if validation fails
     */
    public static Payment createPayment(
        UUID invoiceId,
        LocalDate paymentDate,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String reference,
        String notes,
        String createdBy
    ) {
        // Validate invoice ID
        if (invoiceId == null) {
            throw new InvalidPaymentException("Invoice ID is required");
        }

        // Validate payment date
        validatePaymentDate(paymentDate);

        // Validate amount
        validateAmount(amount);

        // Validate payment method
        if (paymentMethod == null) {
            throw new InvalidPaymentException("Payment method is required");
        }

        // Validate created by
        if (createdBy == null || createdBy.isBlank()) {
            throw new InvalidPaymentException("Created by is required");
        }

        return new Payment(invoiceId, paymentDate, amount, paymentMethod, reference, notes, createdBy);
    }

    /**
     * Validates payment amount
     * @param amount The amount to validate
     * @throws InvalidPaymentException if amount is null, zero, or negative
     */
    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidPaymentException("Payment amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be positive");
        }
    }

    /**
     * Validates payment date
     * @param paymentDate The date to validate
     * @throws InvalidPaymentException if date is null or in the future
     */
    private static void validatePaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new InvalidPaymentException("Payment date is required");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new InvalidPaymentException("Payment date cannot be in the future");
        }
    }

    /**
     * JPA lifecycle callback to set createdAt timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Equals based on entity identity (id)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return id != null && Objects.equals(id, payment.id);
    }

    /**
     * HashCode based on entity identity (id)
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", invoiceId=" + invoiceId +
                ", amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", paymentDate=" + paymentDate +
                ", reference='" + reference + '\'' +
                '}';
    }
}
