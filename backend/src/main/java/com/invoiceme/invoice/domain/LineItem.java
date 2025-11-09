package com.invoiceme.invoice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * LineItem Value Object
 * Immutable value object representing a line item on an invoice
 * Contains both stored fields and calculated amounts
 */
public record LineItem(
    String id,
    String description,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal discountPercent,
    BigDecimal taxRate
) {
    /**
     * Compact constructor for validation
     */
    public LineItem {
        // Generate ID if not provided
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        // Validate required fields
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }

        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 1");
        }

        if (taxRate == null) {
            taxRate = BigDecimal.ZERO;
        }
        if (taxRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative");
        }

        // Ensure proper scale for monetary values
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        discountPercent = discountPercent.setScale(4, RoundingMode.HALF_UP);
        taxRate = taxRate.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculated field: subtotal before discount and tax
     * @return quantity * unitPrice
     */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculated field: discount amount
     * @return subtotal * discountPercent
     */
    public BigDecimal discountAmount() {
        return subtotal().multiply(discountPercent)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculated field: amount subject to tax (after discount)
     * @return subtotal - discountAmount
     */
    public BigDecimal taxableAmount() {
        return subtotal().subtract(discountAmount())
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculated field: tax amount
     * @return taxableAmount * taxRate
     */
    public BigDecimal taxAmount() {
        return taxableAmount().multiply(taxRate)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculated field: total line item amount
     * @return taxableAmount + taxAmount
     */
    public BigDecimal total() {
        return taxableAmount().add(taxAmount())
            .setScale(2, RoundingMode.HALF_UP);
    }
}
